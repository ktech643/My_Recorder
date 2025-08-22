/* ffmpeg plugin for OpenSceneGraph implemented from scratch -
 * Copyright: Michael Korneev (mchl.cori@gmail.com)
 * VS_Library by Michal Korneev (BSD License)
 *
 * Do what you want with my library, just point my name as author
*/
#ifndef _VS_Sound_H_
#define _VS_Sound_H_
#ifndef _ANDROID

#ifndef _WIN
#include <unistd.h>
#endif
#include <stdint.h>
#include "errors.h"
#ifdef _WIN
/////////////
// LINKING //
/////////////
#pragma comment(lib, "dsound.lib")
#pragma comment(lib, "dxguid.lib")
#pragma comment(lib, "winmm.lib")


//////////////
// INCLUDES //
//////////////
//#include <windows.h>
#include "udp.h"
#include <mmsystem.h>
#include <dsound.h>
#include <stdio.h>
#else
#include <alsa/asoundlib.h>
#include "crossunit.h"
#include <stdio.h>
#include <pthread.h>
#include <stdint.h>
typedef  unsigned int DWORD;
typedef  uint8_t  BYTE;
#endif

class VS_Controller;
class VS_Manager;
class ModAudioBuff;
class MyMutex;

struct PtsItem
{
    PtsItem()              = default;
    PtsItem(const PtsItem& _item) = default;
    uint64_t m_pts = 0;
    uint64_t m_cur = 0;
    double m_time = 0;
    PtsItem* next = NULL;
};

class PtsList {
public:
    PtsList();
    void PushBack(PtsItem _item);
    PtsItem* GetCurrent();
    void Pop();
    void Lock();
    void Unlock();
    void clear();
    MyMutex* mutex;
    PtsItem* first = NULL;
    PtsItem* last = NULL;
    ~PtsList();
};

class VS_Sound
{
friend class             VS_Controller;
#ifdef _LIN
typedef struct PTS_ITEM{
int64_t                  Off;
int64_t                  Pts;
double                   Time;                                   //in seconds
int                      PktNum;
PTS_ITEM                 *Next;
}PTS_ITEM;

#endif
public:
typedef struct PARAMS{
char                     *sDevice;
int                      iUseFloat;
int                      iFrames;
int                      iThrScheme;
int                      iAvailMin;
int                      bBigBuf;
int                      BufSize;                                //common size of the second buffer
int                      SampleRate;
int                      ChannelNumber;
int                      SampleSize;
int                      TargetFormat;
int                      StartDelay;
int                      StopDelay;
}PARAMS;

#ifndef _WIN
friend void*             VS_SoundThread(void * Sound);
friend 
#ifdef _WIN
       DWORD WINAPI
#else
       void*
#endif
                         AudioThread(void* lpParam);
#endif
public:
	VS_Sound();
	~VS_Sound();

    int                  AddController(VS_Controller *Controller);
    int                  PrepareWinSecBuf(PARAMS *CurParams);
    int                  SwitchTo(VS_Controller *Controller);
    void                 SwitchFrom(VS_Controller *Controller);
    void                 RemoveController(VS_Controller *Controller);
    void                 Lock();
    void                 Unlock();
	void                 Shutdown();
	int                  bIsInited;
	int                  bIsPlaying;
	bool                 AddAndPlay(BYTE* pBuf, int Len, int64_t pts, double Secs, int PktNum);        //if pBuf == 0, writing silence
	int                  GetPlayDiif();
	int                  GetPlayingPosition();
	int                  SetVolume(int Level);
	int                  GetVolume();
	int                  Mute(int bMute);
	int                  IsMuted();
    int                  SetWritePosOnInc(int Time);
    int                  InitSound(PARAMS *CurParams, char *sCurDevice, int bFirst);
    void                 Drop(uint32_t _dropWaitTime = 0);
	bool                 Play();
	bool                 Pause();
	bool				 bDropBeforAdd = false;
	void                 Stop();
    void                 Resume();
	void				 SkipController();
	void				 StopAudioThread();
	bool				 bStopAudioThread = false;
    void                 SetAudioTime(double Sec);

	int                  StartTime;
	bool				 bPause;
    bool				 bSkipController = false;
#ifdef _WIN	
    HWND                 m_hwnd;                                 //для какого окна задавать настройки звука
    int                  iWinResumed;
    int                  iWinPosition;
#endif
    void                 PrestartPause();
private:
	int                  CurOff;                                 //текущее смещение в буфере
protected:
	int                  SvSoundVol;
	int                  bIsMuted;
    DWORD                CurrentPlayCursor, CurrentWriteCursor;  //для линукса, конструируем кольцевой буфер сами с помощью отдельного потока
    PARAMS               NoCtrlParms;                             //used if there is not any controller
    bool                 useConvert = false;
#ifdef _WIN
	bool                 InitializeDirectSound();
	void                 ShutdownDirectSound();
	void                 ShutdownBuffer();
	IDirectSound8        *m_DirectSound;
	IDirectSoundBuffer   *m_primaryBuffer;
	IDirectSoundBuffer8  *m_secondaryBuffer;
	WAVEFORMATEX         waveFormat;	
#else
    void                 Process();                              //ALSA работает только в одном треде, как OpenGL
	void                 ResetTimer();
    double               GetAudioTime();
	snd_pcm_t            *pcm_handle;
    snd_pcm_hw_params_t  *hwparams;
    snd_pcm_sw_params_t  *swparams;
    unsigned int         bps;
    snd_pcm_uframes_t    frames;
    uint8_t              *RingBuf;
    int                  PrevRingBufSize;
    int                  bThread;     
    double               audio_secs;
    int                  added_samples;
    int                  ResetOff;                                //если не равен -1, то показывает что текущая аудиодорожка заканчивается и начинается другая
    int                  bWriting;    
    int                  SS;           
    int                  bPcmStarted;
    int                  PrevSS;
public:
    int                  bNeedPrepare;
    int                  iPrepared;
    int                  bPreparing;
    int                  iCmd;                                   //==0 playing, ==1 to set pause, ==2 paused, ==4 drop
    int                  bUsePts;
    PtsList              m_ptsList;
    int                  prevCmd;
    bool                 dropTimer = false;
    int                  bPrestartPause = false;
    int                  bSoundPlay = false;
    PTS_ITEM             *FirstPts;
    PTS_ITEM             *LastPts;
    bool                 timerStop;
    timespec            ft = {0,0};
    timespec            st = {0,0};
    timespec            tt = {0,0};
    int                 itCount = 0;
#endif
public:  //mingw doesn't recognize protected correctly (friend construction doesn't work)
#ifdef _WIN	    
    CRITICAL_SECTION     CriticalSection;
#else
    pthread_mutex_t      CriticalSection;
#endif  
    VS_Controller        *FirstController;
    VS_Controller        *CurController; 
    int                  PrevPktNum;
	bool				 bWait;
    ModAudioBuff*        compressedBuff = NULL;
};

#else //ifndef _ANDROID

#include "crossunit.h"
#include "unistd.h"
#include <list>
//#include <pthread.h>
#include <stdint.h>
typedef  unsigned int DWORD;
typedef  uint8_t  BYTE;

class VS_Controller;
class VS_Manager;
class ModAudioBuff;
class MyMutex;




struct PtsItem
{
    PtsItem()              = default;
    PtsItem(const PtsItem& _item) = default;
    int64_t m_pts = 0;
    int m_cur = 0;
    double m_time = 0;
};

class PtsList {
public:
    PtsList() {}
    void PushBack(PtsItem _item) { items.push_back(_item);}
    PtsItem* GetCurrent() {
        if(!items.empty())
            return &items.front();
        return nullptr;
    }
    void Pop() {items.pop_front();}
    void Lock() {mutex.Lock();}
    void Unlock() {mutex.Unlock();}
    void clear() {items.clear();}
    MyMutex mutex;
    std::list<PtsItem> items;
    PtsItem* last = NULL;
    ~PtsList() {}
};

struct PARAMS{
    char                     *sDevice;
    int                      iUseFloat;
    int                      iFrames;
    int                      iThrScheme;
    int                      iAvailMin;
    int                      bBigBuf;
    int                      BufSize;                                //common size of the second buffer
    int                      SampleRate;
    int                      ChannelNumber;
    int                      SampleSize;
    int                      TargetFormat;
    int                      StartDelay;
    int                      StopDelay;
};

class VS_Sound
{
    friend class             VS_Controller;
public:

    //void* AudioThread(void* lpParam) {return nullptr};
public:
    VS_Sound() {}
    ~VS_Sound() {}

    void                 ResetTimer() {}
    double               GetAudioTime() { return 0.0; }

    int                  AddController(VS_Controller *Controller) { return true; }
    int                  SwitchTo(VS_Controller *Controller) { return true; }
    void                 SwitchFrom(VS_Controller *Controller) {}
    void                 RemoveController(VS_Controller *Controller) {}
    void                 Lock() {}
    void                 Unlock() {}
//    void                 Shutdown();

    bool                 AddAndPlay(BYTE* pBuf, int Len, int64_t pts, double Secs, int PktNum) { return true; }        //if pBuf == 0, writing silence
    int                  GetPlayDiif() { return 0; }
//    int                  GetPlayingPosition();
    int                  SetVolume(int Level) { return Level; }
    int                  GetVolume() { return 1; }
    int                  Mute(int bMute) { return bMute; }
//    int                  IsMuted();
//    int                  SetWritePosOnInc(int Time);
//    int                  InitSound(PARAMS *CurParams, char *sCurDevice, int bFirst);
    void                 Drop(uint32_t _dropWaitTime = 0) {}
//    bool                 Play();
    bool                 Pause() { return false; }

    void                 Stop() {}
    void                 Resume() {}
//    void				 SkipController();
//    void				 StopAudioThread();

    void                 SetAudioTime(double Sec) {}

//    int                  StartTime;

    void                 PrestartPause() {}
public:
//    int                  bNeedPrepare;
                                 //==0 playing, ==1 to set pause, ==2 paused, ==4 drop
//    int                  bUsePts;

//    int                  prevCmd;
//    bool                 dropTimer = false;
//    int                  bPrestartPause = false;

//    PTS_ITEM             *FirstPts;
//    PTS_ITEM             *LastPts;
//    bool                 timerStop;
//    timespec            ft = {0,0};
//    timespec            st = {0,0};
//    timespec            tt = {0,0};
//    int                 itCount = 0;
public:  //mingw doesn't recognize protected correctly (friend construction doesn't work)
//    pthread_mutex_t      CriticalSection;
//    VS_Controller        *FirstController;

//    int                  PrevPktNum;
    bool				 bDropBeforAdd = false;
    bool				 bStopAudioThread = false;
    bool				 bPause = false;
    bool				 bSkipController = false;
    int                  iPrepared = 1;
    int                  bPreparing = 0;
    int                  iCmd;
    PtsList              m_ptsList;
    VS_Controller        *CurController = nullptr;
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
    ModAudioBuff*        compressedBuff = NULL;
};
#endif //ifndef _ANDROID

#endif
