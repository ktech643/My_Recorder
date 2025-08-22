#ifndef WEB_SOURCE
#define WEB_SOURCE

#include "../VS_Abstract.h"
#include "../cef/web_core.h"

class WebSource : public GLSource {
public:
	WebSource(PageInfo _pageInfo, GLuint _texture = 0);
	void DisplayCurFrame() override
	{

	}
	bool IsReady() override {
		return true;
	}
	void Start() override {

	}
	void SwitchFrom(SourceType _type);
	void PrepareToPlay(SourceType _type);
	PageInfo pageInfo;
};

#endif //WEB_SOURCE