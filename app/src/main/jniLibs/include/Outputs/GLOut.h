#ifndef GL_OUT
#define GL_OUT
#include "../VS_Abstract.h"
class DLL_EXPORT GLOut : public AbstractOut {
public:
	virtual void DisplayCurFrame() override;
	virtual SourceType GetSourceType() override;
	virtual AbstractSource* GetCurentSource() override;
	virtual void SetSource(AbstractSource* _source, SourceType _type) override;
	virtual void AboutUpdate() {}
	virtual int Play() { return 0; }
	virtual void Stop() {}
protected:
	GLSource*	curSource;
	SourceType	type;
};
#endif //GL_OUT

