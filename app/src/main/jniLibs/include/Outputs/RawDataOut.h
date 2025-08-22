#ifndef RAWDATA_OUT
#define RAWDATA_OUT
#include "../VS_Abstract.h"
class RawDataOut : public AbstractOut {
public:
	virtual int  Play() { return 0; };
	virtual void Stop() {};
	virtual void DisplayCurFrame() {};
	virtual void AboutUpdate() {};
	virtual void FrameUpdate(VS_Stream *Stream, AVFrame *Frame);
	virtual void PacketUpdate(VS_Stream *Stream, AVPacket *Frame);
	AbstractSource* GetCurentSource();
	void SetSource(AbstractSource* _source, SourceType _type);
	virtual std::vector<CodecProperty>* GetStreamParams();
	SourceType GetSourceType();
protected:
	AbstractSource*	curSource;
	SourceType	type;
};

#endif //RAWDATA_OUT