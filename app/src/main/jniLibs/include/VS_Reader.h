/* ffmpeg plugin for OpenSceneGraph implemented from scratch -
 * Copyright: Michael Korneev (mchl.cori@gmail.com)
 * VS_Library by Michal Korneev (BSD License)
 *
 * Do what you want with my library, just point my name as author
*/
#ifndef VS_READER_H_DEF
#define VS_READER_H_DEF
//#include <Windows.h>
#include "crossunits.h"
#include "VS_Sound.h"
#include "errors.h"
#include "VS_CustomIO.h"
#ifdef _WIN

//#include "../WinPlayer/SOIL2/SOIL2.h"
#include "TimerList.h"
#ifdef _VS
#pragma warning(disable:4996)
#endif
#else
//#include <GL/gl.h>
#include <semaphore.h>
#include <pthread.h>
#include <stdint.h>
#include "errors.h"
#include "CodecProperty.h"
#include "VS_Abstract.h"
//#include "VS_Abstract.h"

//#include <map>
typedef  unsigned int DWORD;
typedef  uint8_t  BYTE;
enum SeekState {SEEK_WAIT_FIRST, SEEK_FLUSH_DECODER, SEEK_COMPLITE};
#endif

extern "C"
{
#ifdef _WINDOWS
   #define __STDC_CONSTANT_MACROS
#endif

#    define av_warn_unused_result __attribute__((warn_unused_result))

#ifdef __ANDROID__

#else

/*#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavformat/avio.h"
#include "libavdevice/avdevice.h"
#ifdef USE_QSV
#include "libavcodec/qsv.h"
#include "libavutil/hwcontext_qsv.h"
#include "libavcodec/qsv_internal.h"
#endif // USE_QSV


#include "libavutil/hwcontext.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>*/

#define INFO_PRINT( ...) if(bPrintInfo)printf(__VA_ARGS__)
#define ERR_PRINT(...)  if(bErrPrint)fprintf(stderr, __VA_ARGS__)


#endif

#ifdef _WIN
    //void SetRav1(AVRational *rav, int sample_rate);
    //AVRational AvTimeBase();
#else
    #include "libavresample/avresample.h"
#endif
}
#define VS_UKNOWN  0
#define VS_ANY    -1

#define VS_INT16I  1
#define VS_INT32I  2
#define VS_FLOATN  3

class VS_Reader;
class VS_Cache;
class VS_Writer;
#ifdef _WIN
typedef void(*AboutErrorFunc)(int error, const char* _str, int _deviseType, int _devise);
typedef void(*AboutMessageFunc)(const char* str, int _deviseType, int _devise);
#else
typedef void(*AboutErrorFunc)(int error, const char* _str, int _deviseType, int _devise);
typedef void(*AboutMessageFunc)(const char* str, int _deviseType, int _devise);
#endif

enum SourceState {SOURCE_EMPTY, SOURCE_PREPARED, SOURCE_WORKING, SOURCE_WAIT, SOURCE_ERROR, SOURCE_END};

struct DShowOption {
	char pixFmt[50];
	int w = -1;
	int h = -1;
	AVRational framerate = {0,0};
	int sampleRate = -1;
	int sampleSize = -1;
	int chanells = -1;
	char dshowVideoDeviceName[300];
	char dshowAudioDeviceName[300];
	int dshowVideoDeviceNumber = -1;
	int dshowAudioDeviceNumber = -1;
	int dshowVideoPin = -1;
	int rtbufsize = -1;
	char vcodec[50];
};

#ifdef _LIN

#ifndef _NO_NV
#include "Cuda/dynlink_nvcuvid.h" // <nvcuvid.h>
#include "Cuda/dynlink_cuda.h"    // <cuda.h>
#include "Cuda/dynlink_builtin_types.h"
//#include "../Cuda/helper_functions.h"
#include "Cuda/helper_cuda_drvapi.h"
#include "Cuda/dynlink_cudaGL.h"  // <cudaGL.h>

#include "nv_helper.h"
typedef void *CUDADRIVER;
#else
#endif
#endif


class VS_Cache;



struct MyRect {
    MyRect() {}
    MyRect(int _x0, int _x1,int _y0, int _y1) :
        x0(_x0), x1(_x1), y0(_y0), y1(_y1)  {

    }
    int x0;
    int x1;
    int y0;
    int y1;
};

class ModAudioBuff {
public:
    uint8_t* current = NULL;
    int      maxLen = 0;
    float coef = 0;
    float newCoef = 0;
    float frameMod = 0;
    uint8_t* last = NULL;
    uint32_t chanelCount = 0;
    uint32_t byteCount = 0;
    int len = 0;
    bool bFirstframe = false;
    ModAudioBuff() {}
    ~ModAudioBuff() {
        if(current != NULL)
            delete current;
        if(last != NULL)
            delete last;
    }

    void setCurrent(int _curLen) {

    }

    void setAudioParams(AVSampleFormat _audioType, uint32_t _chanelCount, int _curLen) {
        if(last != NULL)
            delete last;
        if(current != NULL)
            delete current;
        switch(_audioType) {
        case AV_SAMPLE_FMT_S32:
        case AV_SAMPLE_FMT_S32P:
        case AV_SAMPLE_FMT_FLTP:
            byteCount = 4;
            break;
        case AV_SAMPLE_FMT_S16P:
        case AV_SAMPLE_FMT_S16:
            byteCount = 2;
            break;
        }
        chanelCount = _chanelCount;
        maxLen = _curLen;
        bFirstframe = false;
        current = new uint8_t[_curLen * byteCount * _chanelCount];
        last = new uint8_t[byteCount * _chanelCount];
        memset(last, 0, byteCount * _chanelCount);
    }

    int calcFrameCount()    {
        return maxLen*coef + 1;
    }

    int fillFrame(uint8_t* _data, AVSampleFormat _audioType, int _frameLen)   {
        float k = 0;
        int num = 0;
        float ost = 0;
        int lastEll = 0;
        if(_audioType == AV_SAMPLE_FMT_S32 || _audioType == AV_SAMPLE_FMT_S32P) {
            int32_t* f1 = 0;
            int32_t* f2 = 0;
            int32_t* iCurent = 0;
            int32_t* newData = (int32_t*)_data;
            int32_t newItem;
            iCurent = (int32_t*)current;
                for(int chanel = 0; chanel < chanelCount; ++chanel)
                    for(int i = 0; i < _frameLen; ++i) {
                        k = frameMod + coef*i;
                        num = int(frameMod + coef*i);
                        ost = k - (float)num;
                        if(k < 1.0 && bFirstframe) {
                            f1 = (int32_t*)last + chanel;
                            f2 = newData + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            iCurent[i*chanelCount + chanel] = newItem ;
                            } else {
                            if(bFirstframe)
                                num--;
                            f1 = newData + num*chanelCount + chanel;
                            f2 = newData + (num + 1)*chanelCount + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            iCurent[i*chanelCount + chanel] = newItem;
                        }
                     }
                     if(!bFirstframe)
                         bFirstframe = true;
                frameMod = ost + coef;
                memcpy(last, _data + chanelCount * num * byteCount, chanelCount * byteCount);
                return num + 1;
        }
        else if(_audioType == AV_SAMPLE_FMT_S16P || _audioType == AV_SAMPLE_FMT_S16) {
            int16_t* f1 = 0;
            int16_t* f2 = 0;
            int16_t* iCurent = 0;
            int16_t* newData = (int16_t*)_data;
            int16_t newItem;
            iCurent = (int16_t*)current;
                for(int chanel = 0; chanel < chanelCount; ++chanel)
                    for(int i = 0; i < _frameLen; ++i) {
                        k = frameMod + coef*i;
                        num = int(frameMod + coef*i);
                        ost = k - (float)num;
                        if(k < 1.0 && bFirstframe) {
                            f1 = (int16_t*)last + chanel;
                            f2 = newData + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            iCurent[i*chanelCount + chanel] = newItem ;
                            } else {
                            if(bFirstframe)
                                num--;
                            f1 = newData + num*chanelCount + chanel;
                            f2 = newData + (num + 1)*chanelCount + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            //if(newItem != *f1 || i != num)
                            //    printf("whron\r\n");
                            iCurent[i*chanelCount + chanel] = newItem;
                        }
                     }
                     if(!bFirstframe)
                         bFirstframe = true;
                frameMod = ost + coef;
                memcpy(last, _data + chanelCount * num * byteCount, chanelCount * 2);
                return num + 1;
        }
        else if(_audioType == AV_SAMPLE_FMT_FLTP) {     //don't work need other algorithm for fltp
            float* f1 = 0;
            float* f2 = 0;
            float* iCurent = 0;
            float* newData = (float*)_data;
            float newItem;
            iCurent = (float*)current;
                for(int chanel = 0; chanel < chanelCount; ++chanel)
                    for(int i = 0; i < _frameLen; ++i) {
                        k = frameMod + coef*i;
                        num = int(frameMod + coef*i);
                        ost = k - (float)num;
                        if(k < 1.0 && bFirstframe) {
                            f1 = (float*)last + chanel;
                            f2 = newData + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            iCurent[i*chanelCount + chanel] = newItem ;
                            } else {
                            if(bFirstframe)
                                num--;
                            f1 = newData + num*chanelCount + chanel;
                            f2 = newData + (num + 1)*chanelCount + chanel;
                            newItem = *f1 - ost * (*f1 - *f2);
                            iCurent[i*chanelCount + chanel] = newItem;
                            //if(newItem != *f1 || i != num)
                            //    printf("whron\r\n");
                            iCurent[i*chanelCount + chanel] = newItem;
                        }
                     }
                     if(!bFirstframe)
                         bFirstframe = true;
                frameMod = ost + coef;
                memcpy(last, _data + chanelCount * num * byteCount, chanelCount * byteCount);
                return num + 1;
        }
    }
};
#ifdef _STREAM_MANAGER
class VS_AbstractPlayer;
#endif // _STREAM_MANAGER
#ifdef _MANAGER
typedef class GlVideoPlayer;
#endif // _Manager




class VS_Controller{
friend class VS_Reader;
friend class AndroidSound;
#ifdef _WIN
       friend DWORD WINAPI     AudioThread(void* lpParam);
#else
       friend void*            AudioThread(void* lpParam);
#endif

public: //mingw doesn't recognize "friend" construction
        int                    bWasVideo;
        int                    bWasAudio;
        double                 last_audio_secs;
        double                 win_audio_secs;
public:
        VS_Controller();
        ~VS_Controller();
virtual void                   OnStream(VS_Reader *Reader, int Type, int StreamIndex);
virtual void                   OnPrepared(VS_Reader *Reader);
virtual void                   Reset();
virtual int                    CuvidIsNotReady();
virtual int                    IsTickFrame(VS_Cache *Cache);
virtual int                    IsMain(VS_Cache *Cache);
virtual double                 GetAudioTime();
virtual void                   OnClose(VS_Sound *Sound){};
virtual void                   ShutdownSound();
virtual void                   PauseSound(int waitPauseSetMs = 0);
virtual void                   ResumeSound();
virtual void                   DropSound(uint32_t _dropWaitTime = 0);
virtual int                    GetVolume();
virtual void                   SetVolume(int Vol);
virtual int                    Mute(int bMute);
virtual void                   SetAudioTime(double Sec);
#ifdef _LIN
        void                   SetPrestartPause();
        void                   WaitStartSound();
        void                   DropAudioTimer();
#endif // _LIN
        void                   IsAudioComplete();
        void                   SetAsCurrentSound();
        void                   DisableSound();
        void				   EditSoundSpeed(float _coef);
        void				   SetDefaultFreq();
        void                   Destructor();
        ModAudioBuff           GetBuff();
        void				   DropAndPause();
        void				   ContinueAfterDrop();
        bool                   bClearAudioBuff;
        void                   ClearBuffers(int waitTime = 0);
        int					   BytesInSec = 0;
#ifdef _WIN
        HWND                   hwnd;
        //HANDLE                 hAudioThread;
        int                    iWinCmd;
        //что то для картинок

#else
        //pthread_t              hAudioThread;  //now it's global
#endif
        VS_Cache               *VideoCache;
        VS_Cache               *AudioCache;
        bool				   bAudioEnd = false;			//audio закончилось. скипаем фреймы.
        int                    iSyncType;       //==1 video to audio, == 0 audio to video
        VS_Controller          *SoundNext;      //for the sound chain
        //int                    iClosedSound;    //==1 close the audio thread, ==2 the audio thread is closed
        PARAMS       SoundParams;
        bool                   bLockAudio;
        int                    iSeeking;
        int                    bDisabling;
        int                    iTargetMode;
        unsigned char*         image;
        unsigned			   w, h;
        bool				   multiVideo;    //onli if we have 2 and more Reader with one texture
        bool                   isWait = false;
        bool				   isLoop = false;};

class TPacket{
public:
        TPacket(){};
        ~TPacket() {};
        int                    Type;
        unsigned char          *Buf;
        unsigned char          **aBufs;  //right(and others) sound channel
        int                    Len;
        int                    RLen;
        int64_t                pts;
        double                 time;
        int                    bReset;   //первый пакет нового звука
        int                    bNew = false;
        int                    bReady = false;
        int                    iPktCopied;  //==1 the packet isn't decoded and copied for repacking, ==2 the packet is not
//decoded and prepared for nvdec, ==3 pkt is added in nvdec queue
        int                    PktNum;
        AVPacket               pkt;      //if we need to store the source packet
        #ifndef _NO_NV
        CUVIDPARSERDISPINFO    *NvDispInfo;
        #endif
};


class VS_Stream{
public:
        VS_Stream();
        AVCodecContext         *c_context;
        int                    iType;
        int                    Index;
        int                    Width;
        int                    Height;
		int64_t				   newReconnectPts = AV_NOPTS_VALUE;
		int64_t				   startPts = 0;
		int64_t				   PtsOffset = -1;
		int64_t				   PktPtsOffset = -1;
        AVRational             time_base;
		AVRational             codec_time_base;
        AVRational             framerate;
        VS_Cache               *FirstCache;
        int64_t sync_pts = -1;
        time_t sync_time = 0;
};
class VS_Cache{
friend class                   NV_Helper;
friend class                   VS_Reader;
friend class                   VE_Reader;
friend class                   VD_Reader;
friend class                   VS_Controller;
//friend class                   VS_Manager;
friend class                   VS_Writer;
protected:
        VS_Cache               *Next;
        timespec               enano_sleep_packet;
        int                    aNext;
        int                    iMulti;
        int                    bEnableCuvid;        //use nvcuvid, if it's possible, use the virtual function IsCuvidEnable for setting the property
#ifndef _NO_NV
static  int CUDAAPI            NvHandleVideoSequence(void *pUserData, CUVIDEOFORMAT *pFormat);
static  int CUDAAPI            NvHandlePictureDecode(void *pUserData, CUVIDPICPARAMS *pPicParams);
static  int CUDAAPI            NvHandlePictureDisplay(void *pUserData, CUVIDPARSERDISPINFO *pPicParams);
#endif

        void                   FreeCurentPacket(int iPrevNum);
        void                   ProcessSizes(int WidthW, int HeightW);
        void                   NVDecodeLastFrames();
        void                   FF_DecodeLastFrames();
        void                   LockLL();
        void                   UnlockLL();
        void                   CheckSeek();
        int                    bFreeingCache;
        int                    bCacheBusy;
        int                    bNvdecBusy;

public:
        VS_Cache();
        ~VS_Cache();
        void                   GetNextFrame();
        int                    bExtTexture;        int                    Lock(int Str, int Type); //==0 for FreeBuffers function, ==1 for busying cache, ==2 for busying nvdec
        void                   Unlock(int Str, int Type);
        int                    IsCuvidEnabled(){return bEnableCuvid;};
        int                    AddFrame(int64_t pts, double Time, AVFrame *Frame);
        int                    AddPacket(int64_t pts, double Time, AVPacket *Pack);
        void                   ProcessAudioPacket(int64_t pts, AVFrame *Frame);
        void                   ProcessVideoPacket(int64_t pts, AVFrame *Frame);
        int					   PackCount();
        void                   SetOutputSize(int WidthW, int HeightW);
        void                   AllocBuffers();
        void                   FreeBuffers();
        void				   DecodeOnePack();
        void				   CheckCacheToSync();
        void				   FreeOlderPack();
        DWORD                  ProcessOnePack(int64_t pts, AVFrame* Frame, int packNum);
        int                    ConvertAudio(uint8_t **input, int Len, int bPlanar);
        int                    ConvertAlloc(int Channels, int Rate, enum AVSampleFormat SampleFmt);
        int                    Init();
        int                    InitDecoder();
        void                   InitCuvid();
        void				   DropPack(int _count);
        void                   CalcCorners(int Index, int CurRow, int CurColumn, int Rows, int Columns);
        void                   ProcessTextures();
        int                    DisplayCurFrame();
        void                   DisplayPrevFrame();
        int                    WriteCurFrameTo(const char *FileName);
        int                    GetFrameDiff();
        int					   GetVideoWidth();
        int					   GetVideoHeight();
        VS_Reader*			   GetPlayingReader();
        double				   GetDiffWithSync();
        double				   ReserchByLoop(double _diff);
        DWORD                  *GetCurFrameBuf();
        bool                    ChechSoundType(AVFrame *Frame);
        VS_Reader              *Reader;
        int                    cLen;
        int                    cFirst;
        int                    cLast;
        int                    iNum;
        AVRational             v_rational;
        AVFrame                *DstFrame;
        int                    iTargetMode;    //==0 target sizes equal to source sizes, ==1 target sizes are iTargetW/iTargetH with keeping ratio, ==2 nearest larger power of two for source sizes, ==3 target sizes are iTargetW/iTargetH without keeping ratio
        uint64_t               iTargetW;       //target Width/Height
        uint64_t               iTargetH;
        int                    SrcWidth;
        int                    SrcHeight;
        SwsContext             *img_convert_ctx;
        TPacket                **cPackets;
        int                    bMain;
        int                    iScale;    //0 - sws_scale 24, 1 - sws_scale 32 and memcpy, 2 - sws_scale to the frame buf, 3 - no sws_scale
        int                    iPlayerMode;
        unsigned char          *cPrevBuf;
        int                    bSwapRGB;
        int                    bVFlip;
        int                    bHFlip;
        int                    iRepack;
        int                    iOff;
        int                    aPrev;
        int                    cNext;
        int64_t                pvs;
        int64_t                prev_diff;
        int                    Tag;       //для внешних классов
        int                    bDisplaying;
        int                    bCustomCuvidInit;
        int                    bInit;
        int                    SampleRate;
        int                    ChannelNumber;
        int                    SampleSize;
        int                    TargetFormat;
        uint8_t                *DstFrameBuf;
        VS_Stream              *Stream;
        float                  Corners[4][3];
        int                    int_id;
        MyRect                 showedRect;
        bool                   bShowRect = false;
        DWORD*                 showedImageBuf = NULL;
        int                    row;
        int                    col;
        int                    CnvChannels;
        int                    CnvRate;
        int                    CurAudioPts;
        AVSampleFormat         CnvSampleFmt;
        BYTE                   *aConvertedBuf;
        int                    aConvertedBufSize;
        int                    aConvertedRBufSize;
        int                    bCuvidExists;        //cuvid was detecte
        #ifndef _NO_NV
        NV_Helper              *Helper;
        #endif
        #ifdef _WIN
        HANDLE                 timer;
        CRITICAL_SECTION       CriticalSection;
        #else
        timespec               nano_sleep_packet;
        pthread_mutex_t        CriticalSection;
        #endif
        int                    bRGBA;               // RGBA or just RGB
        int                    DisplayType;         // ==0 returns buffer, == 1 glTexImage2D->glTexSubImage2D, == 2 NV arb
        int                    CurFrameNum;         //the number of output frames with ProcessFirstPacket/FinishFirstPacket functions
        int                    iCuvidTried;
        int                    bError;              //the stream is impossible to use, f.e codec not found
        int                    bSetReset;           //==1 set reset flag on the first packet
        int                    PktCount;
        int                    cSize;
        double                 duration;            //the duration of the stream
};


class VS_Carrier{
public:
                               VS_Carrier(){pBuf = NULL;};
        //full container
        virtual int            GetStreamCounter(){return -1;};
        virtual int            GetStreamId(int Id){return -1;};
        virtual int            GetStreamCodec(int Id){return -1;};
        virtual int            GetStreamMainOpions(int *Width,int *Height, double *fps, double *dbl_duration){return -1;};
        virtual int            FillPacket(AVPacket *packet){return -1;};
        virtual int            GetStreamType(int Id){return -1;};
        virtual int            IsKeyFrame(){return -1;};
        //network and file stream
        virtual int            Open(VS_Reader *Reader){return 0;};
        virtual int            ReadHdr(VS_Reader *Reader, uint8_t *Buf, int Size){return 0;};  //for network it can be usual read or read the cache stored header
        virtual int            GetHdrSize(VS_Reader *Reader){return 0;};
        virtual int            Read(VS_Reader *Reader, uint8_t *Buf, int Size){return 0;};
        virtual int            Close(VS_Reader *Reader){return 0;};
        virtual int            GetWidth(){return 0;};
        virtual int            GetHeight(){return 0;};
        virtual int            IsRgba(){return 0;}   //if it returns 0, means we need to take the color format from ffmpeg codec, otherwise it's raw rgba frame
        virtual int            ReadFrame(VS_Reader *Reader){return 0;};
        virtual int            ReadPacket(VS_Reader *Reader, uint8_t* PacketHdrRet, uint8_t **BufRet){return 0;};
        uint8_t                *pBuf;  //should be set after ReadFrame
};
class VS_TCPCarrier : public VS_Carrier{
private:
        DWORD                  *Hdr;
        int                    bHeader;
        int                    HdrSize;   //the complete hdr size, including the first two dword's
public:
                               VS_TCPCarrier(){bHeader = false; hSock = 0; Hdr = NULL;};
        virtual int            Open(VS_Reader *Reader);
        virtual int            GetHdrSize(VS_Reader *Reader);
        virtual int            ReadHdr(VS_Reader *Reader, uint8_t *Buf, int Size);  //for network it can be usual read or read the cache stored header
        virtual int            Read(VS_Reader *Reader, uint8_t *Buf, int Size);
        virtual int            Close(VS_Reader *Reader);
        virtual int            GetWidth();
        virtual int            GetHeight();
        virtual void           sendError(const char* _error) {}
        virtual int            ReadFrame(VS_Reader *Reader){return 0;};
        int                    hSock;
};

class VS_EventCarrier : public VS_Carrier{
private:
        DWORD                  *Hdr;
        DWORD                  PacketHdr[8*4];
        BYTE                   *PacketBuf;
        int                    PacketSize;
        int                    bHeader;
        int                    HdrSize;  //the complete hdr size, including the first two dword's
        int                    bPacket;
        #ifdef _LIN
        sem_t                  sem;
        pthread_mutex_t        CriticalSection;
        #endif
public:
                               VS_EventCarrier();
        virtual int            Open(VS_Reader *Reader){return 1;};
        virtual int            ReadHdr(VS_Reader *Reader, uint8_t *Buf, int Size);  //for network it can be usual read or read the cache stored header
        virtual int            GetHdrSize(VS_Reader *Reader){return HdrSize;};
        virtual int            ReadPacket(VS_Reader *Reader, uint8_t* PacketHdrRet, uint8_t **BufRet);
        virtual int            Close(VS_Reader *Reader){return 1;};
        virtual int            GetWidth();
        virtual int            GetHeight();
        //virtual int            ReadFrame(VS_Reader *Reader){return 0;};
                int            PostHdr(uint8_t *Buf, int Size);
                int            Post(uint8_t *PktHdr, uint8_t *Buf, int Size);
                void           Lock();
                void           Unlock();
                int            bLockPacket;
};

enum	Synchronize {
    NON_SYNCHRONIZE = 0, SERVER_SYNC, CLIENT_SYNC
};
enum ThreadHandle {READ_THREAD = 0, PLAY_THREAD, NV_THREAD};

class Converter {
private:
	SwsContext *swsContext = NULL;
	AVFrame *frame = NULL;
	AVFrame *inFrame = av_frame_alloc();
	int w = 0;
	int h = 0;
	int format = -1;
public:
	void ConvertYuvToRgb(unsigned char** yuv, unsigned char *rgba, int _format, int _w, int _h) {
		if (swsContext != NULL && (_w != w || _h != h || _format != format)) {
			sws_freeContext(swsContext);
			swsContext = NULL;
			av_frame_free(&frame);
		}
		if (swsContext == NULL) {
			swsContext = sws_getContext(_w, _h, (AVPixelFormat)_format, _w, _h,
				AV_PIX_FMT_RGBA, SWS_BILINEAR, NULL, NULL, NULL);
			frame = av_frame_alloc();
			frame->width = _w;
			frame->height = _h;
			frame->format = AV_PIX_FMT_RGBA;
			frame->linesize[0] = _w * 4;
			inFrame->linesize[0] = _w;
			inFrame->linesize[1] = _w / 2.0;
			inFrame->linesize[2] = _w / 2.0;
		}
		frame->data[0] = rgba;
		sws_scale(swsContext, (const uint8_t * const *)yuv, inFrame->linesize, 0, _h, frame->data, frame->linesize);
		frame->data[0] = NULL;
	}
	~Converter() {
		if (swsContext != NULL)
			sws_freeContext(swsContext);
		if (frame != NULL)
			av_frame_free(&frame);
		if (inFrame != NULL)
			av_frame_free(&frame);
	}
};


class VS_Reader		{
friend int VS_Reader_ReadFunc(void* ptr, uint8_t* buf, int buf_size);
friend int64_t VS_Reader_SeekFunc(void* ptr, int64_t pos, int whence);
friend int VS_Reader_WriteFunc(void *ptr, unsigned char *buf, int buf_size);
#ifdef _WIN
friend DWORD WINAPI playing_frames(void * Reader);
friend DWORD WINAPI reading_frames(void * Reader);
#else
friend void *playing_frames(void * Reader);
friend void *reading_frames(void * Reader);
friend void *nv_frames(void * Reader);
#endif

friend class VS_Cache;
friend class VS_System;
friend class VS_WriteOut;
protected:
        FILE*         _file = NULL;
        VS_Stream              **Streams;
        std::vector<CodecProperty> streamProperty;
        int                    StreamCnt;
        #ifdef _WIN
        HANDLE                 hPlayThread;
        HANDLE                 hReadThread;
        HANDLE                 timer;
        HANDLE				   playTimer;
        CRITICAL_SECTION       rCriticalSection;
        CRITICAL_SECTION       SeekCriticalSection;
        CRITICAL_SECTION       PrepareCriticalSection;
        #else
        pthread_t              hPlayThread;
        pthread_t              hReadThread;
        pthread_t              hNvThread;
        pthread_mutex_t        rCriticalSection;
        pthread_mutex_t        SeekCriticalSection;
        pthread_mutex_t        PrepareCriticalSection;
        #endif
        virtual int            FileOpen();
        virtual int            FileSeek(int64_t pos, int whence);
        virtual int            FileRead(unsigned char *buf, int buf_size);
        virtual int            FileWrite(unsigned char *buf, int buf_size);
        void                   Lock(int Type, int StrNum);   //0 - system semaphore for free/alloc operations, 1 - seek semaphore, 2 - read one frame/prepare operations
        void                   Unlock(int Type, int StrNum);
        void                   SeekLL(VS_Stream *Stream, double Sec, int Flags);
        int                    SetTimeFromPts(int64_t pts, VS_Stream *Stream);
        double                 CalcTimeFromPts(int64_t pts, VS_Stream *Stream);
		int					   ReOpen();
        int                    FF_Prepare();
        int                    Cntr_Prepare();
        int                    Cntr_Read(AVPacket *packet);
        int                    FullCntr_Prepare();
        int                    FullCntr_Read(AVPacket *packet);
        int                    CheckSeekEnd(int64_t pts, double Time);
		void				   SetStartPts(int64_t _pts);
        int                    iSeekingAudio;
        int                    iSeekingVideo;
        int                    SeekFlags;
		uint8_t*			   buffer = NULL;
		int					   packetDuration = 0;
        int                    SeekIndex;
        int                    iSetSound;
		void				   flushDecoders();
        double                 iSoundTime;
        double                 SeekTime;
        double                 PrevSeekTime;
        double                 dbl_duration;
        double				   Time = 0;
        bool				   bSaveToBuff;
        bool				   bWaitLastFrame = false;
        int                    iSeekDone;
        int                    bDisabling; //now we're disabling
        int64_t                sv_pts;
        double                 cmd_dif;
        int					   SkipPacketCount = 0;
        VS_Cache               *PlayCache;  //usually it's to Controller->VideoCache

		int64_t				   ptsCounter = 0;
		int64_t				   ptsOffset = 0;
        #ifndef _NO_NV
        CUVIDPARSERDISPINFO    *CurNvDispInfo;  //Current frame in video memory to display, ==NULL if there is no frame
        int                    PrevPicIndex;
        void                   NVFrames();
        #endif
        int					   ProcessingAudio();
        int				       ProcessingVideo();
        int					   ProcessingAudioPts(int _pts, int _time);
        int							ProcessingVideoPts(int _pts, int _time);
        void						SendToCache(int pts, int Time);
		int							PrepareBitFilter(int _streamId);
		const AVBitStreamFilter			*StartCodeFilter;
		char					error[300];
		AVBSFContext				*StartCodeContext = NULL;
        void						Free();
        void						Loop();
virtual void						OnFree(){};
public:
        VS_Reader();
        ~VS_Reader();
#ifdef _MANAGER
		GlVideoPlayer*			   GetManager() {
            return Manager;
        }
#endif
#ifdef _WIN
        UdpServer*			   synchServer;
        UdpClient*			   synchClient;
        Synchronize			   synchronize;
#endif
        void changeState(SourceState _state) {
            state = _state;
            if(OnInputState != NULL)
                OnInputState(sourceId, state);
        }
        void AboutReconnect(bool _res) {
            if(OnReconnect != NULL)
                OnReconnect(sourceId, _res);
        }

        void AboutInputState(int _state) {
            if(OnInputState != NULL)
                OnInputState(sourceId, _state);
        }

        std::function<void(int _socket, int _net)> SelectNetwork = NULL;

        std::function<void(int,bool)> OnReconnect = NULL;
        std::function<void(int,int)> OnInputState = NULL;
        virtual void           OnPlayStart() { printf("play started\n"); }
        bool                   isPlay;
#ifdef _WIN
        HANDLE*
#else
        pthread_t*
#endif
        GetThreadHandle(ThreadHandle _handleType);
        VS_Stream*             getStream(int _type);
        CodecProperty          getStreamProperty(int _type);
        VS_Stream*             getStreamByNum(int _Num);
		static int			   SaveFrameAsPNG(AVCodecContext *pCodecCtx, AVFrame *pFrame, int FrameNo, const char* _path);
        VS_Stream*             GetStream(int _type);
        int                    bPlayFrames;
        void                   NextFrame();
        static void            ToLogBin(uint8_t *fmt, uint64_t size, int _file);
        static void            ToLog(const char* fmt, ...);
		void				   sendError(Errors _error, const char* _message, ...);
		void				   sendMessage(const char* _message);
        void                   AdFlush();
        int                    ReadOneFrame();
        void                   ReadingStream();
        void                   PlayFrames();     //inside PlayingThread
        AVRational             getFramerate();
        void                   PrintCurTime(char *Buf);
        int                    GetCurTimeInSecs();
        int                    PutFrame();
virtual void                   FrameUpdated(VS_Cache *Cache){};
virtual void                   FrameUpdated(VS_Stream *_Stream, AVFrame *Frame);
virtual void                   PacketUpdated(VS_Stream *_Stream, AVPacket *Frame);
virtual int                    Prepare(VS_Carrier *CarrierW, int Flag){Carrier = CarrierW; return Prepare();};
virtual int                    Prepare();
        int                    Tick();
		int					   GetStatus();
		void                   setNewConnectionPts(int _reconnectionTime, AVRational _timeBase); //reconnection time
		int hwdecode_packet(DecodeContext *decode, VS_Stream* _streeam,
			AVFrame *frame, AVFrame *sw_frame,
			AVPacket *pkt);
        int                    bLocStart = 0;
        SeekState              seekState = SEEK_COMPLITE;
        int64_t                firstPtsAfterSeek = AV_NOPTS_VALUE;
        VS_Cache               *AddCacheForStream(int bMain, int StreamIndex);
        void                   SetOutputSize(int StreamIndex, int Tag, int *WidthW, int *HeightW, int Mode, int Flags);
        void                   GetOutputSize(int StreamIndex, int Tag, int *WidthW, int *HeightW);
        VS_Cache               *FindCache(int bMain, int iType);
        void                   SeekForward(double Sec);
        void                   SeekBackward(double Sec);
        void                   Seek(double Sec);
        void                   DecodeYUV420SP(uint32_t *RgbBuf, uint8_t *Yuv420sp, int Width, int Height);
        int                    WritePngFile(const char *FileName, uint32_t *pBuf, int Width, int Height);
        int                    ClearStreams();
        MyRect                 GetShowedRect();
        void                   SetShowedRect(int _x0, int _x1, int _y0, int _y1);
        double                 GetDuration(){return dbl_duration;};
virtual int                    IsCuvidEnabled(VS_Cache *Cache){return 0;};
virtual void                   Start();
virtual void                   Disable(bool disableSound = true);              //kill the reading thread, free resources, set the waiting state
		int					   AddStartCode();
		AVPacket*			   bitPacket = NULL;
        int                    IsDisabled(){return iDisabled;};
        int                    HandleICB();
        int                    Pause(int Flag, int timeout);        //==0 play, ==1 pause, ==2 xor pause
        void                   NVSendEndOfStream();
        double                 fps;
        int64_t                iSeekTs;
        int                    (*OnEnd)();
        bool                   isRealtime();
        void                   (*OnSeek)(VS_Reader *Reader);
#ifdef _LIN
        timespec               readStartTime;
#else 
		LARGE_INTEGER			readStartTime;
#endif

        void                   (*ReleaseExtThreads)();
        void                   (*ExtOnPrepared)(VS_Reader *PThis);
        int                    iLive;
		int					   minLookAhead = 16;
		DecodeContext		   decode{ NULL }; //hardware decode
        int                    iWasSeek;
        int                    iDisabled;  //==1 by default, the video is disabled, use Start to run it
        int                    threadCount = 1;
		int					   sourceId;
        bool                   isSeek;
        unsigned int abcounter = 0;
        bool				   isLoop;
        bool				   LoopCheck = false;
		bool				   bFFmpegQsvDecoder = false;
        SourceState			   state = SOURCE_EMPTY;
		int					   readerWait = 0;
		int                    bWriteFrames;
		int					   needEncodeA = false;
		int					   needEncodeV = false;
        int                    bPrepared;
        int                    bWriting;
        int                    iReadingOneFrame;
        int                    iReading;         //==0 not reading and was not started, ==1 reading, ==2 was reading and error happend, ==3 was reading and EOF finished
        int                    bPreparing;
        int                    iFreeing;
        int                    bFinished;
        int                    bAudio;
		int					   readCount = 0;
		int					   curReadCount = 0;
		AVRational			   framerate = { 0,0 };
        AVRational			   curFramerate = { 0,0 };
		int					   curReconnectCount = 0;
		int					   reconnectCount = 0;
		int					   timeout = 0;
		int					   autoPrepare = 0;
		int					   connectionWaitTime = 0;
        int                    bDelayedDecoding;
		int					   bUseStartCode = 0;
        int                    bVideo;
        int                    bNoTickThread;
        int                    iPlayFrames;
        int64_t                cust_fl;
        double				   dispTime;
        unsigned char          *cust_buf = NULL;
        unsigned int           cust_size;
		bool				   skipBeforeKey = false;
		int64_t                prevPktDts = AV_NOPTS_VALUE;
		int64_t                pktMaxDuration = AV_NOPTS_VALUE;
        int                    bEnd;            //the stream is finished
        int                    bCustomReading;  //reading the stream from somewhere
        int                    bUdpMode;
        int                    bIgnoreVideo;
        int                    bIgnoreAudio;
        int                    bMainAudio;      //главной является аудио дорожка
        int                    bPause;
        int                    bCustomCuvidInit;
        AVFrame                *Frame;
		AVFrame				   *hwFrame = NULL;
        double                 cur_secs;
        AVIOInterruptCB        icb;
        AVPacket               packet;
		int					   bAboutFrameUpdate = false;
		int					   bAboutPacketUpdate = false;
		int					   bAboutHWFrameUpdate = false;
        int                    DecodedLen;
        int                    bStopped;
        int                    bCommonVDecoder;
        int64_t                disp_pts;
        double                 fps_interval;
        double                 cur_delay;
        double                 duration;
        double                 audio_secs;
        double                 prev_audio_secs;
        char                   Source[512];
		char                   format[512] = "";
		char				   rtspTransport[50] = "";
        int                    bFirst;
        int                    bStartAudio;
        int64_t                BasePts = 0;
        bool				   bFileLog;
        bool				   bPtsSyncPause;
        bool                   SkipWrongSeekPack = false;
        int                    bSeekHard;      //находить время, как можно точнее
        int                    iMode;          //==1 frame by frame mode
        int                    iNvAfterSeek;
        int64_t                iNvTimeStamp;
        int                    iContainer;     //the own container, 1 - file, 2 - use VS_Carrier->Read, 3 - use VS_Carrier->ReadPacket with the processing thread, 4 - use VS_Carrier->ReadPacket without the processing thread, 5 - full container that controls the stream, assumed using of PutFrame
        VS_Carrier             *Carrier;
        bool                   playPaused;
        int                    fl;
        AVFormatContext        *context;
        VS_Controller          *Controller;
        AVDictionary           *opts = NULL;
		int					   skeepOld = false;
        bool				   bLoop;
        //std::map<int, AVPacket*>   packMap;
        int64_t				   lastPTS;
        int64_t				   lastPacketDTS;
        int64_t				   SeekPts;
        bool                   bEndSended;
		int					   bUseDShow = false;
		DShowOption			   dshowOpt;
		int					   listen = 0;
        bool                   LastPackNeedSended = false;
        bool                   PrepareLastPack = false;
        //double                 cmd_dif;
        bool                   bExternPlayer = false;
        bool                   bCanDecodedVideo = true;
		bool				   isReconnect = false;
#ifdef _MANAGER
		GlVideoPlayer		   *Manager;
#endif
#ifdef _STREAM_MANAGER
		VS_AbstractPlayer	   *rawOut = NULL;
#endif
        virtual				   void ClipFrame(uint8_t *_buf, int _len, int _pts) {
            printf("frame cliped pts: ", _pts);
        }
        int					   firstClipPts = -1;//обязательнно установить ignoreaudio
        int					   lastClipPts = -1;
        bool				   bCliped = false;
        int                    oneFrameRead = false;
#ifdef _WIN
        TimerList			   timerList;
        LARGE_INTEGER          PrevFrameTime;  //время, когда случился предыдущий кадр
		LARGE_INTEGER		   ptime;
#else

        timespec               PrevFrameTime;
        timespec               ptime;
        std::shared_ptr<VS_CustomIO> customIO;
#endif
};

typedef void(__stdcall *fflog)(int _logLevel, const char* _ch);

class ffloger {
private:
    int		logLevel;
    fflog   toLog = NULL;
    ffloger() {}
    ffloger(const ffloger&) = default;
    ffloger& operator=(ffloger&) = default;
public:

    static ffloger &getInstance();
    void setFFmpegCallback(int _logLvl, fflog _log);
    int getLogLevel();
    fflog getCallback();
    void send(int _lvl, const char* _ch);
};

#endif

