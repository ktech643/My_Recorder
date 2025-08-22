#ifndef VS_ABSTRACT_MANAGER
#define VS_ABSTRACT_MANAGER

#if defined( _DLL )
#define DLL_EXPORT __declspec(dllexport)
#endif
#ifndef _WIN
#define __stdcall
#define DLL_EXPORT
#endif

#include "VS_Reader.h"
#include "CodecProperty.h"
#include "vector"

#define VS_INPUT 0;
#define VS_OUTPUT 1;

extern void* (*rdr_alloc)(size_t Size);
extern void(*rdr_free)(void *Buf);
extern VS_Sound *Sound;

bool operator ==(const AVRational& x, const AVRational& y);

bool operator !=(const AVRational& x, const AVRational& y);

bool operator <(const AVRational& x, const AVRational& y);

enum SourceType { VIDEO = 0, PICTURE = 1, URL = 2, HTML = 3, RTSP = 4, ST_SOUND, OTHER};
enum PlayerCommand { PLAYER_WAIT = 0, PLAYER_WORKING, PLAYER_CLOSE, PLAYER_PAUSE, PLAYER_RESUME, PLAYER_STOP, PLAYER_SEEKING, PLAYER_NONE };
enum StartPageType { LOAD_URL, LOAD_HTML, LOAD_OTHER };

struct PageInfo {
    PageInfo() {}
    PageInfo(const char * _ch, StartPageType _type) {
        type = _type;
        strcpy(url, _ch);
    }
    char url[512];
    StartPageType type;

};


#ifdef _WCHAR_MESSAGE
void getString(wchar* _outMessage, char* _inMessage, int _size, va_list& _va) {
    wchar_t  *wstring = new wchar_t[_size];
    mbstowcs(wstring, _inMessage, _size);
    vswprintf(wstring, 256, wmessage, ap);
}
#endif

class Timer {
public:
    void reset();
    double getTime();
    void pause();
    void resume();
private:
    bool bInit = false;
    double pauseTime = 0.0;
    timespec timerStart;
    timespec timerLastCheck;
};

template <typename T>
static bool checkCommandComplite(Timer &_timer, T* _data, T _value, int _timeout) {
    while ((*_data != _value) && _timer.getTime()*1000 < _timeout) {
        CrossPlatformSleep(2);
    }
    return (*_data == _value);
}

extern int bErrPrint;

struct AbstractProperty {
    char initialStr[300];
};

struct InputProperty : public AbstractProperty
{
    int w;
    int h;
    int decoder;
    char format[50] = "";
    char rtspTransport[50] = "";
    char rawOutPath[300] = "";
    int bAddStartCode = false;
    int bSkipBeforeKey = false;
    AVRational framerate;
    int bWriteRawOut = false;
    int bWriteOutSync = false;
    int bHWDecoding = false;
    int readCount = 0;
    int timeout = -1;
    int listen = 0;
    int reconnectWaitTime = 5000;
    int connectionWaitTime = -1;
    int reconnectCount = -1;
    char dshowVideoDeviceName[300] = "";
    char dshowAudioDeviceName[300] = "";
    int dshowVideoDeviceNumber = -1;
    int dshowAudioDeviceNumber = -1;
    int dshowVideoPin = -1;
    int rtbufsize = -1;
    char dshowVideoCodec[50] = "";
    int dshowAudioSampleRate = -1;
    int dshowAudioChanells = -1;
    int dshowAudioSampleSize = -1;
    char dshowVideoPixelFmt[50] = "";
    int bAboutPacketUpdate = false;
    int bAboutFrameUpdate = false;
    int bAboutHWFrameUpdate = false;
};

struct AbstractOutputProperty {
    int inputId = -1;
    char initialStr[300] = "";
};

struct MediaOutputProperty : public AbstractOutputProperty
{
    int w = -1;
    int h = -1;
    int encoder = 0;
    AVRational codecTimeBase = { 0 , 0 };
    AVRational streamTimeBase = { 0 , 0 };
    int bitrate = -1;
    int maxrate = -1;
    int minrate = -1;
    int buffsize = -1;
    int globalQuality = -1;
    int lookAhead = -1;
    int lookAheadDepth = -1;
    int vSync = 0;
    int crf = -1;
    int pixFormat = -1;
    int scaleType = SWS_BICUBIC; //see ffmpeg defines
    int outType = 1;	//file, raw_stream, nal_stream
    int bHardwareEncode = 0;
    int gop_size = -1;
    int max_b_frames = -1;
    int refs = -1;
    int maxDecFrameBuffering = -1;
    int deleteH264Info = 0;
    int scenecut = -1;
    int convertToRGBA = false;
    int useStartCode = false;
    int zeroLatency = false;
    int qsvScale = 0;
    char scaleW[100] = "";
    char scaleH[100] = "";
    char scaleOption[200] = "";
    int bitrateLimit = -1;
    //Audio property
    uint64_t channelLayout = 0;
    int aEncoder = 0;
    int aFormat = -1;
    int aNone = 0;
    int sample_rate = -1;
    int channels = -1;
    int aBitRate = -1;
    int removeNall = 0;
    int removeNallHeader = 0;
    int keyFrameNAL = 0;
    AVRational aTimeBase;
    char format[50] = "";
    char profile[50] = "";
    char preset[50] = "";

    MediaOutputProperty() {
        memset(scaleW, '\0', 100);
        memset(scaleH, '\0', 100);
        memset(scaleOption, '\0', 200);
        memset(format, '\0', 50);
        memset(profile, '\0', 50);
        memset(preset, '\0', 50);
    }
    ~MediaOutputProperty() {

    }
};

class AbstractSource {			//
public:
    virtual void	DisplayCurFrame() = 0;
    virtual			~AbstractSource() {};
    virtual int     getState() = 0;
    virtual CodecProperty getStreamProperty(int _streamType) = 0;
    int				sourceId = 0;
    SourceType		type = OTHER;
};

class AbstractOut {
public:
    virtual int  Play() = 0;
    virtual void Stop() = 0;
    virtual void DisplayCurFrame() = 0;
    virtual void AboutUpdate() = 0;
    virtual AbstractSource* GetCurentSource(int _type) = 0;
    virtual void SetSource(AbstractSource* _source, int _type) = 0;
    virtual int GetId() = 0;
};

#ifdef _USE_GL
class GLSource : public AbstractSource {
public:
    GLSource(GLuint _texture = 0) :
        texture(_texture) {}
    virtual	void DisplayCurFrame() {}
    virtual	~GLSource() {
        printf("delete GLSource:");
        if (bDelTexture)
            glDeleteTextures(1, &(texture));
    }
    virtual bool IsReady() { return true; }
    virtual void Start() {}
    virtual void SwitchFrom(SourceType _type) {}
    virtual void PrepareToPlay(SourceType _type) {}
    GLuint			texture = 0;
    bool			bDelTexture = false;
    SourceType		type;
};
#endif


class VS_AbstractPlayer
{
public:
    virtual int	 AddSource(InputProperty& _prop, SourceType _type) = 0;
    virtual void DeleteSource(int hSource) = 0;
    virtual void Start(int hSource) = 0;
    virtual int  SwitchTo(int hSource) = 0; //????????????? ?? ?????? ????????
    virtual int  IsReady(int hSource) = 0;	//???? ?????????? ?????????
    virtual int  Play(int hSource) = 0;		//???????? ??????????????? ?????????
    virtual void Stop() = 0;				//????????? ??????????????? ??????? ???? ??????????
    virtual void DisplayCurFrame() = 0;
    virtual void frameUpdate(CodecProperty &_streamProp, AVFrame *_frame) = 0;
    virtual void packetUpdate(CodecProperty &_streamProp, AVPacket *_frame) = 0;
    virtual void sendMessage(const char* _message) = 0;
    virtual void sendError(Errors _error, const char* _message, ...) = 0;
    virtual int  OnPrepared(int hSource) = 0;

};

class AbstructSound {
public:
    virtual void    ResetTimer() = 0;
    virtual double  GetAudioTime() = 0;
    virtual bool    AddAndPlay(BYTE* pBuf, int Len, int64_t pts, double Secs, int PktNum) = 0;
    virtual int     GetPlayDiif() = 0;
    virtual int     SetVolume(int Level) = 0;
    virtual int     GetVolume() = 0;
    virtual int     Mute(int bMute) = 0;
    virtual void    Drop(uint32_t _dropWaitTime = 0) = 0;
    virtual int32_t    Pause() = 0;
    virtual int32_t    Stop() = 0;
    virtual int32_t    Resume() = 0;
    virtual void    SetAudioTime(double Sec) = 0;
    virtual void    PrestartPause() = 0;
    virtual int32_t open() = 0;
};

class AbstructFrameTask {
public:
    AbstructFrameTask(int _taskID) : m_taskID(_taskID) {}
    virtual int sendFrame(AVFrame* _inFrame, int _inputID) = 0;
    virtual int resaveFrame(AVFrame* _outFrame) = 0;
    int curTaskID() { return m_taskID;}
protected:
    int m_taskID = -1;
};


class AndroidFileLogger {
public:
    static AndroidFileLogger& getInstance();
    void open(const char* _path);
    void write(const char* _message, ...);
    void fflog(int _lvl, const char *_message);
    void writeSD(const char* _message, int _space, int _dim, ...);
    void ffwriteSD(const char* _message, int _space, int _dim, int _err, ...);
    std::string getTime();
    void close();
    bool isOpen();
    ~AndroidFileLogger();
private:
    std::mutex mut;
    std::string path;
    std::string fflogstr;
    FILE* m_file = NULL;
};


#endif