#ifndef STREAM_SOURCE
#define STREAM_SOURCE
#include "../VS_Abstract.h"

class StreamSource : public AbstractSource {
public:
	StreamSource(InputProperty& _prop, VS_AbstractPlayer* _manager = NULL, int _threadCount = 1);
	void DisplayCurFrame() {}
	~StreamSource();
	bool IsReady();
	void Start();
	void Stop();
	VS_Reader* GetReader();
	VS_Controller* GetController();
	int getInputStatus();
	void sendMessage(const char* _message);
    void sendError(Errors _error, const char* _message, ...);
    CodecProperty   getStreamProperty(int _streamType) override ;
    int     getState() override ;
	int onPrepared();
	bool isComplite();
private:
	VS_Reader       *reader = NULL;
	VS_Controller   *controller = NULL;
	MediaOutputProperty rawProp;
	bool			bComplite = false;
};
#endif //STREAM_SOURCE


