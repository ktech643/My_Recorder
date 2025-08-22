#ifndef FRAME_BUFFER
#define FRAME_BUFFER
#include <vector>
#include <mutex>
#include <thread>
#include <condition_variable>
#include "ffheader.h"
#include <list>

template <class T>
struct CircularProp {
	std::function<T()> alloc;
	std::function<void(T*)> del;
	std::function<int(T, T)> ref;
	std::function<void(T)> unref;
	int waitTime = 0;
	int size = 10;
};

template <class T>
class CircularBuffer {
public:
	CircularBuffer(CircularProp<T> _prop) : prop(_prop)
	{
		buff.resize(_prop.size);
		for (auto &item : buff) {
			item = prop.alloc();
		}
	}

	void pushBack(T _t)
	{
		std::mutex cMutex;
		std::unique_lock<std::mutex> cLock(cMutex);
		while (getSize() == prop.size-1) {
			if ((prop.waitTime <= 0) || cVar.wait_for(cLock, std::chrono::milliseconds(prop.waitTime)) == std::cv_status::timeout)
				break;
			else
				continue;
		}
		if (getSize() == prop.size - 1)
			popFront();
		std::unique_lock<std::mutex> mLock(mutex);
		prop.ref(buff[end], _t);
		end = (end + 1) % prop.size;
	}

	int getSize()
	{
		if (beg <= end)
			return end - beg;
		else
			return prop.size - beg + end;
	}

	bool popFront(int _count = 1)
	{
		if (getSize() < _count)
			_count = getSize();
		if(_count == 0)
			return false;
		std::unique_lock<std::mutex> mLock(mutex);
		for(int i = 0; i < _count; ++i) {
			prop.unref(buff[beg]);
			beg = (beg + 1) % prop.size;
		}
		cVar.notify_all();
		return true;
	}

	bool getFront(T _t)
	{
		if (getSize() == 0)
			return false;
		std::unique_lock<std::mutex> mLock(mutex);
		prop.ref(_t, buff[beg]);
		prop.unref(buff[beg]);
		beg = (beg + 1) % prop.size;
		cVar.notify_all();
		return true;
	}

	void clear()
	{
		while (getSize())
			popFront();
	}

	T front() {
		return buff[beg];
	}

	T at(int i) {
		return buff[(beg+i)%buff.size()];
	}

	~CircularBuffer()
	{
		for (auto &item : buff) {
			prop.del(&item);
		}
	}
private:
	CircularProp<T> prop;
	std::condition_variable cVar;
	std::mutex mutex;
	std::vector<T> buff;
	int beg = 0;
	int end = 0;
};

class InputBuffer {
public:
	InputBuffer();
	void addVideoFrame(AVFrame* _vFrame);
	bool getVideoFrame(AVFrame* _vFrame);
	void addAudioFrame(AVFrame* _aFrame);
	bool getAudioFrame(AVFrame* _aFrame);
	AVFrame* frontAudioFrame();
	AVPacket* frontAudioPacket();
	void addAudioPacket(AVPacket* _aPacket);
	bool getAudioPacket(AVPacket* _aPacket);
	void addVideoPacket(AVPacket* _vPacket);
	bool getVideoPacket(AVPacket* _vPacket);
	bool isEmpty(int _type = -1);
	int getPackCount(int _type = -1);
	int getFrameCount(int _type = -1);
	void clear(int _type = -1);
	AVPacket* atPacket(int _type, int _index);
	AVFrame* atFrame(int _type, int _index);
	void popPackets(int _type, int _count);
	void popFrames(int _type, int _count);
private:
	bool needConvert = false;
	CircularBuffer<AVFrame*> vFrames;
	CircularBuffer<AVFrame*> aFrames;
	CircularBuffer<AVPacket*> aPackets;
	CircularBuffer<AVPacket*> vPackets;
};

#endif