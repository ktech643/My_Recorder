#ifndef WRITE_OUT
#define WRITE_OUT
#include "../Sources/StreamSource.h"
#include "../Outputs/RawDataOut.h"
#include "../crossunit.h"
#include "writeoutunits.h"
#include "map"
#include "list"

struct NallUnit {
	NallUnit(int _id, uint32_t _len, uint8_t* _buf) {
		id = _id;
		len = _len;
		buf = new uint8_t[_len];
		memcpy(buf, _buf, len);
	}
	void Set(int _id, uint32_t _len, uint8_t* _buf) {
		id = _id;
		len = _len;
		if (buf)
			delete buf;
		buf = new uint8_t[_len];
		memcpy(buf, _buf, len);
	}
	NallUnit() {}
	int id = -1;
	uint32_t len = 0;
	uint8_t* buf = NULL;
	~NallUnit() {
		if(buf)
		delete buf;
	}
};

class VS_WriteOut : public RawDataOut
{
public:
	VS_WriteOut(int _id);
	int GetId();
	void Lock();
	void UnLock();
	virtual int  Play() override;
	void	setInputProperty(std::vector<CodecProperty>* _prop);
	void	setInputProperty(MediaOutputProperty& _prop);
	virtual void Stop() override;
	virtual void DisplayCurFrame() override;
	virtual void AboutUpdate() override;
	void FrameUpdate(VS_Stream* _stream, AVFrame *_frame) override;
	void FrameUpdate(int _streamIndex, AVFrame *Frame);
	int ProcessVideoFrame(AVStream * Stream, AVFrame **Frame);
	int ProcessAudioFrame(AVStream * Stream, AVFrame *Frame);
	AVFrame* ScaleFrame(int _streamIndex, AVFrame *Frame);
	AVFrame* ResampleFrame(int _streamIndex, AVFrame *Frame);
	void PacketUpdate(VS_Stream *Stream, AVPacket *Frame) override;
	std::vector<CodecProperty>* GetStreamParams() override;
	int PrepareWriter(std::vector<CodecProperty>* _propertys, AVFrame* _frame = NULL);
	int PrepareFromProp();
	int EncodeFrame(AVStream * Stream, AVFrame * Frame);		//work only inf outType == 1
	void SwsScaleFrame(int _streamIndex, AVFrame * Frame);
    void (__stdcall *AboutRescaleFrame)(FrameInfo& _frame) = NULL;
	bool CheckProperty(const CodecProperty&  _property);
	void SendFrameUpdate(AVStream * Stream, AVFrame * Frame);
	void SendPacketUpdate(AVStream * Stream, AVPacket * Frame);
	void SendScaleUpdate(AVStream* _stream, AVFrame * Frame);
    void(__stdcall *AboutFrameUpdate)(FrameInfo& _frame) = NULL;
	int InitVideoFilter(const CodecProperty& _inOption, AVCodecContext* _outCContext);
	void setToDict(const char* name, int options);
    void(__stdcall *AboutPacketUpdate)(PacketInfo& _packet);
	AboutErrorFunc aboutError = NULL;
	AboutMessageFunc aboutMessage = NULL;
	void WriteProperty(const CodecProperty& _property);
	int AddVideoStream(const CodecProperty& _inOption);
	int AddAudioStream(const CodecProperty& _inOption);
	void sendMessage(const char* _message, ...);
	void sendError(Errors _error, const char* _message, ...);
	//void InitHardware(AVPixelFormat _format);
	uint32_t reverse(uint32_t _x);
	int SetSPSAndPPSBuffer(AVCodecContext* _context, PacketInfo* _packet);
	uint32_t FoundIdrFrame(AVPacket * _packet);
	int ConvertFrame(AVFrame* _dstFrame, AVFrame* _srcFrame, SwsContext* _swsContext);
	void Free();
	void CloseWriter();
	bool IsPlayed();
	int InitAudioFilter(const CodecProperty& _inOption);
	void EncodeSoundBefore(int _all = false);
	int isRepack(int _i);
	AVPixelFormat getHWFormatFromHwContext(AVBufferRef * _hw_frames_ctx);
	AVPixelFormat getFormatFromHwContext(AVBufferRef * _hw_frames_ctx);
	int findNalUnitLenght(int _number, uint8_t * _srcBuffer, int _len, uint8_t * _dstBuffer = NULL);
	int InitHWFrameContext(DecodeContext* _decode, AVPixelFormat _format, AVPixelFormat _swFormat,
		int _wight, int _height, int _poolSize, int _frameType);
	~VS_WriteOut();
	bool needEncode = false;
	bool needEncodeA = false;
	bool needEncodeV = false;
	void RepackOnly();
	StreamControl GetControl(int i);
    MediaOutputProperty* GetProperty(int type);
    MediaOutputProperty videoProp;
    MediaOutputProperty audioProp;
    bool bSyncOut = false;
private:
	bool bPrepared = false;
	bool bDisable = true;
	std::vector<CodecProperty> inputOpt;
    std::vector <StreamControl> control;
	AVFormatContext* outContext = NULL;
	AVStream** streams = NULL;
	AVRational inAudioTb;
	int streamCnt = 0;
	int gotEncode;
	char error[300];
    MyMutex _mutex;
	AVPacket* pack = NULL;
	AVPacket* filterPacket = NULL;
	AVFrame* outFrame = NULL;
	AVFrame* videoFrame = NULL;
	AVFrame* videoFrameRGBA = NULL;
	AVFrame* audioFrame = NULL;
	AVFrame* hwFrame = NULL;
	SwrContext *swr_ctx;
	uint8_t *frameBuffer = NULL;
	uint8_t *frameBufferRGBA = NULL;
	SwsContext *preFilter = NULL;
	AVFrame*	preFilterFrame = NULL;
	AVFrame*	hwPreFilterFrame = NULL;
	SwsContext *preEncoder = NULL;
	AVFrame*	preEncoderFrame = NULL;
	AVFrame*	hwPreEncoderFrame = NULL;
	AVFrame*	resampledFrame = NULL;
	AVFrame*	encodeFrame = NULL;
	FFfilter	rgbaFilter;
	const AVBitStreamFilter* removeNallFilter = NULL;
	AVBSFContext* removeNallContext = NULL;
	const AVBitStreamFilter* changeSPSFilter = NULL;
	AVBSFContext* changeSPSContext = NULL;
	const AVBitStreamFilter* removeSeiFilter = NULL;
	AVBSFContext* removeSeiContext = NULL;
	AVDictionary* contextDict = NULL;
	char charBuffer[255];
	AVCodecContext* videoCodecContext = NULL;
	bool needHwFrame = false;
	const AVBitStreamFilter* adNallFilter = NULL;
	DecodeContext hwEncodeContext{ NULL, NULL }; //hardware encode
	AVBSFContext* avbsfContext = NULL;
	std::vector<StreamBufer> bufers;
	AVCodec* videoCodec = NULL;
	AVCodecContext* audioCodecContext = NULL;
	AVCodec* audioCodec = NULL;
	FrameInfo outVFrame;
	FrameInfo outAFrame;
	FrameInfo curOutFrame;
	AVFilterContext *bufferSinkCtx = NULL;
	AVFilterContext *bufferSrcCtx = NULL;
	AVFilterGraph *filterGraph = NULL;
	AVFilterContext *audioBufferSinkCtx = NULL;
	AVFilterContext *audioBufferSrcCtx = NULL;
	AVFilterGraph *audioFilterGraph = NULL;
	DecodeContext hwDecodeContext{ NULL, NULL };  //hardware decode
	AVFrame* lastFrame = NULL;
	double frameCoef = 0;
	AVRational frameTB;
	int64_t lastPts = AV_NOPTS_VALUE;
	int64_t lastWritePts = AV_NOPTS_VALUE;
	std::map<uint8_t, NallUnit> NallMap;
	int nallExist = -1;
	bool bIgnoreAudio = true;
	int64_t lastDts = AV_NOPTS_VALUE;
	int nVideoPack = 0;
	int nAudioPack = 0;
	int id;
	int lastAFrame = -11111111;
	int lastVFrame = -11111111;
	int prepareError = 0;
};

#endif //WRITE_OUT
