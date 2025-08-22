#ifndef GL_MANAGER
#define GL_MANAGER

#include "../Sources/VideoSource.h"
#include "../Sources/PictureSource.h"
#include "../Sources/SourceList.h"
#include "../Outputs/GLVideoOut.h"

#ifdef USE_CEF
#include "../Sources/WebSource.h"
#include "../Outputs/GLWebOut.h"
#endif


class DLL_EXPORT GLManager :	public VS_AbstractPlayer
{
public:
	GLManager();
	virtual int		AddSource(InputProperty& _prop, SourceType _type) override;
	virtual void	DeleteSource(int hSource) override;
	virtual void	Start(int hSource) override;
	virtual int		SwitchTo(int hSource) override;
	virtual int		IsReady(int hSource) override;
	virtual int		Play(int hSource) override;
	void				StopOut();
	virtual void	Stop() override;
	virtual void	DisplayCurFrame() override;
	virtual int    OnPrepared(int hSource) override;
	void			SetTexture(GLuint _text);
	void			SetWindowSize(const int _h, const int _w);
	void			sendMessage(const char* _message) override;
	void			sendError(Errors _error, const char* _message, ...) override;
	GLuint          GetTexture();
#ifdef USE_CEF
	void			OpenStartPage(char* _ch, StartPageType _type);
	void			CreateBrowser(PageInfo _startUrl);
	bool			BrowserInit(int* _exitCode);
	void			WebMouseClick(int x, int y, CefBrowserHost::MouseButtonType btn, bool mouse_up);
	void			WebKeyPressed(int _key);
#endif
	void			SetDecoderThreadCount(int _count);
	void		    SetSyncSetting(Synchronize type, UINT16 _port = 8888, const char* _addres = NULL);
	void		    StartSync();
	void		    StopSync();
	void			NextFrame();
	void			FrameUpdate(VS_Stream *Stream, AVFrame *Frame, int sourceId = -1) {};
	void			PacketUpdate(VS_Stream *Stream, AVPacket *Frame, int sourceId = -1) {};
	GlVideoPlayer*  GetGlOut();
#ifdef _WIN
	void			setHwnd(HWND _hwnd);
#endif
	~GLManager();
private:
	SourceList		Sources;
	GLSource*		curSource = NULL;
	HWND			hwnd = NULL;
	bool			textureIsGen = false;
	int				width = 0;
	int				height = 0;
	float			packPercent = 0.2;
	int				decoderThreadCount = 1;
	GLuint			displayTexture = 0;
	bool			bAudioInited;
	UdpServer*	    synchServer = NULL;
	UdpClient*	    synchClient = NULL;
	Synchronize	    synchronize;
	GlVideoPlayer	glOut;
#ifdef USE_CEF
	GLWebOut		webOut;
	bool			bWebAutoUpdate = true;
#endif // USE_CEF


	AbstractOut*	curOut = NULL;

};

#endif //GL_MANAGER