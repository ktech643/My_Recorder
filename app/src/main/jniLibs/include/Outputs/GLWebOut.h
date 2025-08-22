#ifndef GL_WEB_OUT
#define GL_WEB_OUT
#include "GLOut.h"
#include "../Sources/WebSource.h"

class DLL_EXPORT GLWebOut : public AbstractOut {
public:

	GLWebOut();
	int  Play() override;
	void AboutUpdate() override {

	}
	AbstractSource* GetCurentSource() override;

	void SetSource(AbstractSource* _source, SourceType _type) override;

	bool CreateBrowser(PageInfo _startUrl, int _w, int _h, HWND _hwnd, GLuint _texture);

	void SetStartPage(PageInfo _startUrl);

	void OpenStartPage();

	void mouseClick(int x, int y, CefBrowserHost::MouseButtonType btn, bool mouse_up);

	void keyPressed(int _key);

	virtual void Stop();;

	SourceType GetSourceType();

	bool	BrowserInit(int* _exitCode);
	bool	CloseBrowser();

	void DisplayCurFrame()  override;

private:
	PageInfo		startPage = { "www.goolge.com", LOAD_URL };
	
	SourceType		type;
#ifdef USE_CEF
	WebSource*		curSource;
	WebCoreManager* webManager;
	WebCore*	    browser;
#endif //USE_CEF
};
#endif //GL_WEB_OUT