#ifndef PICTURE_SOURCE
#define PICTURE_SOURCE
#include "../VS_Abstract.h"
class PictureSource : public GLSource {
public:
	PictureSource(const char* _path, GLuint _texture = 0);
	~PictureSource();
	bool IsReady() override {
		return picture != NULL;
	}
	void Start() override {

	}
	void SwitchFrom(SourceType _type);
	void PrepareToPlay(SourceType _type);
	void DisplayCurFrame() override;
private:
	uint8_t* picture = NULL;
	int w, h, chanels;
};
#endif //PICTURE_SOURCE