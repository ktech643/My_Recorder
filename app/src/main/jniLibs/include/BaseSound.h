#ifndef BASESOUND_H
#define BASESOUND_H
#include "VS_Abstract.h"
#include <memory>
#include <cstring>
#include <list>


typedef std::unique_ptr<uint8_t> uint8_p;
template < typename T >
struct ACircularBuffer {
    ACircularBuffer(int _size) {
        data = std::unique_ptr<T>(new T[_size]);
        size = _size;
        unitSize = sizeof(T);
    }
    int freeSize() {
        if(curBegin <= curEnd)
            return size - curEnd + curBegin;
        else
            return curBegin - curEnd;
    }
    int usedSize() {
        return size - freeSize();
    }
    bool push(BYTE* _data, int _size) {
        if(_size%unitSize) {
            printf("wrong data size set %d\r\n", _size - _size%unitSize);
        }
        _size /= unitSize;
        if(freeSize() < _size) {
            printf("free size is low\r\n");
            return false;
        }
        T *curPoint;
        curPoint = data.get() + curEnd;
        int writeSize = std::min(_size, size - curEnd);
        memcpy(curPoint, _data, writeSize*unitSize);
        if(writeSize != _size) {
            memcpy(data.get(), _data + writeSize, _size - writeSize);
        }
        curEnd += writeSize;
        curEnd %= size;
        return true;
    }
    void clear() {
        curEnd = curBegin = 0;
    }
    std::list<std::pair<T*, int>> get(int _size) {
        std::list<std::pair<T*, int>> TList;
        if(usedSize() < _size) {
            printf("data size is low\r\n");
        } else {
            int readSize = std::min(_size, size - curBegin);
            TList.push_back(std::pair<T*, int>(data.get() + curBegin, readSize));
            if(readSize < _size) {
                TList.push_back(std::pair<T*, int>(data.get(), _size - readSize));
            }
            curBegin += _size;
            curBegin %= size;
        }
        return TList;
    }
    std::unique_ptr<T> data;
    int curBegin = 0;
    int curEnd = 0;
    int size;
    int unitSize;
};

class BaseSound : public AbstructSound {
public:
    BaseSound();
    virtual void    ResetTimer() override;
    virtual double  GetAudioTime() override;
    virtual bool    AddAndPlay(BYTE* pBuf, int Len, int64_t pts, double Secs, int PktNum) override;
    virtual int     GetPlayDiif() override;
    virtual int     SetVolume(int Level) override;
    virtual int     GetVolume() override;
    virtual int     Mute(int bMute) override;
    virtual void    Drop(uint32_t _dropWaitTime = 0) override;
    virtual int32_t Pause() override;
    virtual int32_t Stop() override;
    virtual int32_t Resume() override;
    virtual void    SetAudioTime(double Sec) override;
    virtual void    PrestartPause() override;
    virtual int     InitSound(CodecProperty CurParams);
    virtual void    Lock();
    virtual void    UnLock();
    virtual int32_t open() override;
protected:
    CodecProperty          m_curAudioProperty;
    PtsList                m_PtsList;
    std::mutex             m_BufferLock;
    Timer                  m_timer;
    ACircularBuffer<float> m_Buffer;
    MyMutex                m_Mutex;
    double                 m_sec = 0;
};
#endif // BASESOUND_H
