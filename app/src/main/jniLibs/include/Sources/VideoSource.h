#ifndef VIDEO_SOURCE
#define VIDEO_SOURCE

#include "../VS_Abstract.h"

struct VideoInitProp{
	int w = 0;
	int h = 0;
	char* path = NULL;
	char* format = NULL;
	int threadCount = NULL;
	int udpMode = 0;
	GLuint texture = 0;
	HWND hwnd = 0;
	UdpClient* synchClient = NULL;
	UdpServer* synchServer = NULL;
	Synchronize synch = NON_SYNCHRONIZE;
};

class VideoSource : public GLSource {
public:
	VideoSource(VideoInitProp& _initProperty);
	VideoSource(VS_Reader* _reader, VS_Controller* _contrller, GLuint _texture);
	VS_Controller  *GetController() { return controller; }
	VS_Reader      *GetReader() { return reader; }
	void DisplayCurFrame();
	bool IsReady() override {
		return videoCache->GetFrameDiff() < videoCache->cLast / 10.0;
	}
	void Start() override;
	void SwitchFrom(SourceType _type);
	void PrepareToPlay(SourceType _type);

	~VideoSource();
private:
	VS_Reader       *reader = NULL;
	VS_Cache		*videoCache = NULL;
	VS_Controller   *controller = NULL;
};
#endif //VIDEO_SOURCE