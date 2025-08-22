#ifndef _FFHeader
#define _FFHeader

#include "../VS_Abstract.h"
#include "../Sources/StreamSource.h"
#include <map>
#include <queue>
#include "../VSFilter.h"
#include "../OboeSound.h"
#include "../ffencoder.h"
#include "../FrameBuffer.h"

using ffdictel = std::pair<std::string, std::string>;



enum FFState {WRITER_NONE, WRITER_WORKING, WRITER_STOP, WRITER_CLOSED, WRITER_WAIT, WRITER_ERROR};
class FFWriter : public AbstractOut
{
public:
    FFWriter(int _outID);
	void DisplayCurFrame()  override  {}
	void AboutUpdate()  override  {}
	AbstractSource* GetCurentSource(int _type)  override;
	void SetSource(AbstractSource* _source, int _type)  override;
	//void CheckSources(int _)
	int open(std::vector<AVCodecParameters*> _property = std::vector<AVCodecParameters*>(),
		std::string _output = std::string(), std::string _protocol = std::string(), int _outId = -1);
	void setDestination(std::string _protocolType, std::string _url);
	void setOutputCodecPar(CodecProperty _codecPar, int _type);
	int Prepare();
	int Play() override ;
	void Stop()  override ;
	bool SourceStoped();
	void closeCodecs();
	void closeOutput(bool _closeDep, bool _sendAbout);
	int packetUpdate(CodecProperty &_streamProperty, AVPacket* _packet);
    int frameUpdate(CodecProperty &_streamProperty, AVFrame* _frame);
    int checkAudio(int64_t _time);
	int processPacket(AVPacket* _packet, bool _isTrans = false);
	int processFrame(AVFrame* _frame, AVPacket *_pack, int _streamNum, int _type);
	int processing();
	int GetId();
	void addFormatDict(std::string _key, std::string _value);
	void changeOutputState(FFState _state);
	FFState getState();
	std::function<void(int,int)> OnOutputState = NULL;
	void SourcePrepared(AbstractSource *_source);
	void SourcePrepared(AbstractSource *_source, std::shared_ptr<VSFilter> _task);
	void PrepareDep(FFWriter* _writer);
	bool isSourcePrepared();
	//bool checkStreams();
	//void changeCodecPar(AVCodecParameters *_par, AVCodecID _codecID);
	bool checkRealtime();
	void setError();
    bool checkTime(int _time);
	int HandleICB();
	bool needEncode(int _streamType);
	int writePacket(AVPacket *_packet);
	std::function<void()> forceStop = NULL;
	std::function<void(int, VS_Stream*, AVFrame *)> aboutFilteringFrame = NULL;
	CodecProperty getCodecProperty(int _type);
	CodecProperty getCustomProp(int _type);
	bool isUsed(int _streamType, int _sourceID);
	void setDepWriter(FFWriter* _writer);
	FFWriter* getDepWriter();
	~FFWriter();
	bool isDep = false;
private:
    std::list<ffdictel> contextDictOpt;
	bool stoped = false;
	bool isStop = true;
	bool isReaderStop = false;
	bool isError = false;

	std::string outputUrl;
	std::string format;

	int outId = 0;
	//FILE * file = NULL;
	int64_t startTime = AV_NOPTS_VALUE;
	bool skipBeforeKey = false;
	AVFormatContext *context = nullptr;
	AVPacket *curPacket = av_packet_alloc();
	AVFrame *curFrame = av_frame_alloc();
	std::vector<CodecProperty> inputCodecs;
	std::mutex mut;


	StreamSource* vsource = NULL;
	bool asourcePrepared = false;
	int inaID = -1;

	StreamSource* asource = NULL;
	bool vsourcePrepared = false;
	int invID = -1;

	std::shared_ptr<std::thread> thr;
	FFState m_state = WRITER_NONE;
    int aindex = -1;
	int vindex = -1;
	bool clearBuffers = false;
	bool customStart = true;
	bool sourcePrepared = false;
	bool isRealTime = false;
	AVIOInterruptCB icb;
	int reconnectWaitTime = 5000;
	int timeout = 3000;
	int connectionWaitTime = 10000;
	int messageCounter[2] = {0,0};
    timespec pstartTime;
    timespec ptime;
    FFEncoder vencoder;
    FFEncoder aencoder;
    VSFilter afilter = {-1};
    CodecProperty vproperty;
	CodecProperty aproperty;
    AVRational defFramerate = {0, 0};
    InputBuffer buffer;
	int ErCount = 15;	//todo переделать
    //debug unit
    int maxVideoProcTime = 0;
    timespec pStartProcTime;
    std::mutex m_updateLock;
    FFWriter* depWriter = NULL;    //пока что только 1
    bool m_enableCustomSoundTime = 0;
    int m_customSoundTime = 0;
};

#endif
