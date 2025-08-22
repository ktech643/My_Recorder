//
// Created by steri on 09.07.2020.
//

#ifndef TV_SERVER_OboeSound_H
#define TV_SERVER_OboeSound_H

#include "BaseSound.h"
#include <condition_variable>
#include <mutex>
#include <thread>
#include "oboe/Oboe.h"

#define NANOS_PER_MICROSECOND ((int64_t)1000)
#define NANOS_PER_MILLISECOND (NANOS_PER_MICROSECOND * 1000)
#define NANOS_PER_SECOND      (NANOS_PER_MILLISECOND * 1000)

enum SoundState {SOUND_CLOSE, SOUND_STOP, SOUND_OPEN, SOUND_PLAY, SOUND_PAUSE, SOUND_RESUME, SOUND_ERROR};

class OboeSound : public BaseSound, public AbstractSource {
public:
    enum OboeDuration {OBOE_OUTPUT = 0, OBOE_INPUT};
    OboeSound(int _id);
    virtual ~OboeSound();
    int             getState() override;
    CodecProperty   getStreamProperty(int _streamType) override;
    //void            setSharingMode(aaudio_sharing_mode_t requestedSharingMode);
    //void            setPerformanceMode(aaudio_performance_mode_t requestedPerformanceMode);
    virtual int     SetVolume(int Level) override;
    virtual int     GetVolume() override;
    virtual int     Mute(int bMute) override;
    virtual void    Drop(uint32_t _dropWaitTime = 0) override;
    void            Processind();
    bool            command(SoundState _command, int timeout);
    oboe::Result         open(OboeDuration _duration = OBOE_OUTPUT);
    bool            getData(AVFrame* _frame, int _size);
    oboe::Result close();
    //aaudio_result_t prime();
    oboe::Result Start();
    int Stop() override ;
    int Pause() override ;
    int Resume() override ;
    //int waitUntilPaused();
    // Flush the stream. AAudio will stop calling your callback function.
    int flush();
    double GetAudioTime() override ;
    void setCurAudioTime(double _time) ;
    bool isOpen();
    void clear();
    bool isPaused();
    bool isSilence();
    bool AddAndPlay(BYTE* pBuf, int Len, int64_t pts, double Secs, int PktNum) override ;
    int writeToStream(int _framesPerWrite, int _timeout);
    int readFromStream(int _framesPerRead, int _timeout);
    virtual void	DisplayCurFrame() {};
    void aboutFrameUpdate(AVFrame* _frame);
    void setManager(VS_AbstractPlayer* _manager);
    void setMainSource(AbstractSource* _source);
private:
    oboe::AudioStream         *m_Stream = nullptr;
    VS_AbstractPlayer         *m_manager = nullptr;
    AbstractSource            *mainSource = nullptr;
    int64_t                   m_curAudioPts = 0;
    bool                      m_resetTimestamp = false;
    int                       m_direction = OboeDuration::OBOE_OUTPUT;
    int32_t                   audioBufferSize = 0;
    volatile SoundState                m_state = SOUND_STOP;
    volatile SourceState               m_sourceState = SOURCE_WAIT;
    bool                      bUsePts = true;
    std::condition_variable   condVar;
    std::thread          audioThread;
    bool                 bClosed = true;
    bool				 bDropBeforAdd = false;
    bool				 bStopAudioThread = false;
    bool				 bPause = false;
    bool				 bSkipController = false;
    int                  iPrepared = 1;
    int                  bPreparing = 0;
    int                  iCmd;
    int                  bSoundPlay = false;
    int                  ResetOff = 0;
    int                  CurOff = 0;
    int                  SS = 0;
    bool                 useConvert = false;
    int                  bIsMuted = 0;
    double               audio_secs = 0;
    int                  bIsInited = 1;
    int                  bIsPlaying = 1;
    bool				 bWait = false;
    //ModAudioBuff*        compressedBuff = NULL;
    bool                 bOpen = false;
    bool                 bStop = true;
    bool                 bDisable = false;
    bool                 bFlushed = false;
    int32_t              curCounter = 0;
    bool                 isOnlineStream = false;
    int                  emptyFrameCount = 0;
    int                  audioBad = false;
    std::vector<float>   emptyFrame;
    int                  framesPerBurst;
};


#endif //TV_SERVER_OboeSound_H
