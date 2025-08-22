#ifndef FF_FILTER
#define FF_FILTER
#include "VS_Reader.h"
#include "VS_Abstract.h"
#include "FrameBuffer.h"
#include <string>

struct TextOption {
	bool overlayEnabled = false;
	bool box;
	//Color boxcolor;
	std::string  boxcolor_expr;
	//Color fontcolor;
	std::string fontcolor_expr;
	std::string fontfile;
	std::string text; //"\'%{localtime\\:%X}\'";
	int x = -1;
	int y = -1;
	int fontsize = 26;
	/*TextOption() {
		memset(fontfile, '/0', 300);
		memset(fontcolor_expr, '/0', 20);
		memset(boxcolor_expr, '/0', 20);
		memset(text, '/0', 20);
	}*/
};



class VSFilter : public AbstructFrameTask {	//todo добавить реализацию нескольких входов
public:
	VSFilter     (int _taskID);
    int          addAudioFilter(CodecProperty& _inProperty, CodecProperty& _outProperty);
	int          addVideoFilter(CodecProperty& _inProperty, CodecProperty& _outProperty, std::string _other = "");
	void         prepare(CodecProperty& _inProperty, CodecProperty& _outProperty, std::string _other = "");
	int 		 open();
	static bool  addStringToFilterStr(std::string &_filterStr, std::string&& _key, std::string _value, bool _needDesp);

	CodecProperty& getInputProperty();
	CodecProperty& getOutputProperty();

	static std::string  generateFilterStr(CodecProperty _inOption, CodecProperty _outOption);
	static std::string  generateTextOverlay(TextOption _textOption);

    int          sendFrame(AVFrame* _inFrame, int _inputID) override ;
	int          resaveFrame(AVFrame* _outFrame) override ;

	static void  saveFrameAsPNG(CodecProperty &_property, AVFrame *pFrame, int FrameNo, const char* _path);
	AVFilterLink *getOutLink();

	void         clear();
    bool         isInit();
	VSFilter(VSFilter&& _sourceFilter);
    ~VSFilter();
private:
	CodecProperty m_inProperty;
	CodecProperty m_outProperty;
	AVFilterContext *bufferSinkCtx = NULL;
	AVFilterContext *bufferSrcCtx = NULL;
	AVFilterGraph *filterGraph = NULL;
	bool bInit = false;
	std::string m_other;
};


class FiltersLine {
public:
	FiltersLine(int _inputID) : m_inputID(_inputID), m_thread(&FiltersLine::processing, this) {}

	int sendFrame(AVFrame* _inFrame, int _inputID) {
		//if(!m_connected)
		//	return 0;
		if (_inputID == m_inputID) {
			m_inFrames.pushBack(_inFrame);
			return 0;
		}
		return -1;
	}

	int addOutFilter(CodecProperty &_inProperty, CodecProperty &_outProperty, std::string _other, int _taskId) {
		_outProperty.sourceID = _taskId;
		m_filterLine.push_back(std::make_shared<VSFilter>(VSFilter(_taskId)));
		return m_filterLine.back()->addVideoFilter(_inProperty, _outProperty, _other);
	}

	void prepareOutFilter(CodecProperty &_inProperty, CodecProperty &_outProperty, std::string _other, int _taskId) {
		_outProperty.sourceID = _taskId;
		m_filterLine.push_back(std::make_shared<VSFilter>(VSFilter(_taskId)));
		m_filterLine.back()->prepare(_inProperty, _outProperty, _other);
	}

	void initLine() {
		for(auto &item: m_filterLine) {
			item->open();
		}
	}

	std::shared_ptr<VSFilter> getTask(int _taskID) {
		for(auto &item: m_filterLine) {
			if(item->curTaskID() == _taskID) {
				return item;
			}
		}
		return std::shared_ptr<VSFilter>();
	}

	int getInputID() {
		return m_inputID;
	}

	void processFilter(std::shared_ptr<VSFilter>& _filter, std::list<std::pair<int, VSFrame>>& _outFrames,
			std::list<std::pair<int, VSFrame>>& _inFrames) { //todo настроить чтоб фрейм нормально очищался
		VSFrame frame;
		for (auto &procFrame: _inFrames) {
			//AndroidFileLogger::getInstance().writeSD("send to filter %ld", 10, 1, procFrame.second.unit->pts);
			_filter->sendFrame(procFrame.second, procFrame.first);
			while (_filter->resaveFrame(frame) == 0) {
				//AndroidFileLogger::getInstance().writeSD("recive from filter %ld", 10, 1, frame.unit->pts);
				_outFrames.push_back({_filter->curTaskID(), frame});
			}
		}
	}

	void processing() {
		std::list<std::pair<int, VSFrame>> outProcessFrame;
		std::list<std::pair<int, VSFrame>> curProcessFrame;
		int curID = m_inputID;
		int ret = 0;
		AVFrame *curFrame = av_frame_alloc();
		while (!m_stop) {
			if(m_connected) {
				if(m_outFrames.size() > 1) {
					m_filterCond.notify_one();
				}
				if(!m_inFrames.getFront(curFrame)) {
					CrossPlatformSleep(1);
					continue;
				}
				curProcessFrame.push_back({m_inputID, curFrame});
				av_frame_unref(curFrame);
				for (auto& filter: m_filterLine) {
					processFilter(filter, outProcessFrame, curProcessFrame);
					std::swap(outProcessFrame, curProcessFrame);
					outProcessFrame.clear();
					std::unique_lock<std::mutex> lock(m_inOut);
					m_outFrames.insert(m_outFrames.end(), curProcessFrame.begin(),
									   curProcessFrame.end());
				}
				curProcessFrame.clear();
			} else if(m_inFrames.getSize()) {
				for (auto &filter: m_filterLine) {
					filter->open();
				}
				m_connected = true;
			}
			CrossPlatformSleep(1); //ненужная задержка
		}
        av_frame_free(&curFrame);
	}

	int getOutFrames(std::list<std::pair<int, VSFrame>>& _outFrames) {
		std::unique_lock<std::mutex> lock(m_inOut);
		std::swap(_outFrames, m_outFrames);
		return _outFrames.size();
	}

	int getOutCount() {
		m_outFrames.size();
	}



	void stop() {
        if(!m_stop) {
			m_inOut.lock();
            m_stop = true;
			m_filterCond.notify_all();
			m_inOut.unlock();
			m_thread.join();
			AndroidFileLogger::getInstance().writeSD("filters stoped", 5, 1);
        }
	}

	void setConnected(bool _connected) {
		m_connected = _connected;
	}

	~FiltersLine() {
		stop();
	}

	std::list<std::shared_ptr<VSFilter>> &getLine() {
		return m_filterLine;
	}
	void wait() {
		std::unique_lock<std::mutex> lock(m_inOut);
		while(!m_stop && m_outFrames.empty()) {
			m_filterCond.wait(lock);
		}
	}
	bool isStop() {
		return m_stop;
	}
private:
	volatile bool m_stop = false;
	bool m_connected = false;
	int m_inputID = -1;
    CircularBuffer<AVFrame*> m_inFrames =
    		{CircularProp<AVFrame*>{ av_frame_alloc, av_frame_free, av_frame_ref, av_frame_unref, 0, 5 }};
    //todo временное решение, нужна зависимость размера буфера от фреймрейта(проблема задержка аудио на плеере)
    std::thread m_thread;
	std::mutex m_inOut;
	std::condition_variable       m_filterCond;
	std::list<std::pair<int, VSFrame>> m_outFrames;
    std::list<std::shared_ptr<VSFilter>> m_filterLine;
};

#endif
