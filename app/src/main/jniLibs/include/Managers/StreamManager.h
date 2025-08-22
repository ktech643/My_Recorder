#ifndef RTSP_MANAGER
#define RTSP_MANAGER
#include <iostream>
#include "../Outputs/writeoutunits.h"
#include "../Sources/StreamSource.h"
#include "../Sources/SourceList.h"
#include "../Outputs/VideoPlayer.h"
#include "../Outputs/FFWriter.h"
#include "map"

#ifdef max
#undef max
#endif

#ifdef min
#undef min
#endif

enum OutputType {
	NONE_OUT = -1,
	GL_OUT,
	STREAM_OUT,
	RAW_OUT
};

struct OutputsList {
	std::list<AbstractOut*> outputs;
	OutputType type = NONE_OUT;
	int currentNum = 0;
	OutputsList(OutputType _type) : type(_type) {

	}
	AbstractOut* findOut(int _id)  {
		for(auto& item: outputs)
		if(item->GetId() == _id) {
			return item;
		}
		return nullptr;
	}
};

class DLL_EXPORT StreamManager : public VS_AbstractPlayer {
public:
	StreamManager();
	int	 AddSource(InputProperty& _prop, SourceType _type) override;
	void DeleteSource(int hSource) override;
	void Start(int hSource) override;
	int  SwitchTo(int hSource) override;
	int  IsReady(int hSource) override;
	int  Play(int hSource) override;
	int  OnPrepared(int hSource) override;
	VideoPlayer* getPlayer(int _playerId);
	void Stop(int hSource);
	void Stop() override;
	void DisplayCurFrame() override;
	void DisplayCurFrame(int _playerId);
	bool needUpdate(int _playerId);
	int  AddOut(std::string _outPath, int protType, int _sourceId, int _mainOut = -1);
	int  AddGlPlayer(std::shared_ptr<Shader> _shader, std::shared_ptr<Texture> _texture[3], std::list<int> _sourceIds);
	void frameUpdate(CodecProperty &_streamProp, AVFrame *_frame) override;
	std::function<void(int, bool)> onReconnect = NULL;
	std::function<void(int, int)>  onInputStateChange = NULL;
	std::function<void(int, int)>  onOutputStateChange = NULL;
	std::function<void(int,int)> selectNetwork = NULL;
	std::function<void()> forceStop = NULL;
	std::function<void(int _sourceID, VS_Stream* _stream, AVFrame*)> aboutFilteringFrame = NULL;
    void (__stdcall *AboutFrameUpdate)(FrameInfo& _frame) = NULL;
	void packetUpdate(CodecProperty &_streamProp, AVPacket *_frame) override;
    void(__stdcall *AboutRescaleFrame)(FrameInfo& _frame) = NULL;
	//void AboutPacketUpdate(byte* _frame, int _len, int _inputID, int _packType);
	void getInputProperty(int _inputId, InputProperty * _prop);
	int getInputState(int hInput);
    void (__stdcall *AboutPacketUpdate)(PacketInfo& _packet) = NULL;
	AboutErrorFunc aboutError = NULL;
	AboutMessageFunc aboutMessage = NULL;
	void sendMessage(const char* _message) override;
	void sendError(Errors _error, const char* _message, ...) override;
	void seekFile(int _hSource, double _timeOffset);
	int OutUsed(int _id);
    void SetRescaleCallback(void(__stdcall *_AboutRescaleFrame)(FrameInfo& _frame));
	void StopOut();
	void StopOut(int hOut);
	void useMicOut(int _sourceID = -1);
	AbstractOut* getOut(int _outID);
	void muteAudio(bool mute);
	void pushThread(int _sourceId);
	VideoPlayer* getCurGlPlayer();
	void setTextOpt(TextOption _option);
	void setPlayerOpt(CodecProperty _prop);	//todo подцепить к плееру????
	void checkFilterData();
	std::mutex& getGlMutext();
	~StreamManager();
  static const char* getVersion();
private:
	int						      managerId = 0;
	SourceList					  Sources;
	StreamSource*				  curSource = NULL;
	bool						  bUseOut = false;
	std::vector<OutputsList>	  outputTypes;
	int							  globOutCounter = 100;
	VideoPlayer*                  curGlPlayer = NULL;
	std::mutex                    curGLPlayerMut;
	std::unique_ptr<FiltersLine>  filtersLine;
	TextOption                    textOption;
	CodecProperty				  playerOption;
	std::unique_ptr<std::thread>  checkFilter;
	std::shared_ptr<OboeSound>    micSound;
	std::mutex                    frameMutex;
    std::mutex                    packetMutex;
};


#endif //RTSP_MANAGER
