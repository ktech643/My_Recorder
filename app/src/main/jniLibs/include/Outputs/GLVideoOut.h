#ifndef GL_VIDEO_OUT
#define GL_VIDEO_OUT
#include "GLOut.h"
#include "../Sources/VideoSource.h"
class DLL_EXPORT GlVideoPlayer : public GLOut {
public:
	virtual int Play() override;
	void NextFrame();
	void PlayFrames();
	void SetPlayerState(PlayerCommand _state, int _waitMs);
	void Stop() override {
		if (hPlayThread != NULL) {
			SetPlayerState(PLAYER_WAIT, 2000);
		}
	};
	void StartPlayThread();
#ifdef _WIN
	HANDLE          hPlayThread = NULL;
#else
	pthread_t	        hPlayThread = NULL;
	timespec            PrevFrameTime;
#endif // _WIN
	~GlVideoPlayer();
	VS_Reader*		GetCurReader() { return CurReader; }
	VS_Controller*  GetCurController() { return CurController; }
	void			SetDisplayType(bool _toGL);
private:
	bool			textureIsGen = false;
	bool			needDisplay = false;
	int				width;
	int				height;
	float			packPercent = 0.2;
	GLuint			displayTexture;
	MyMutex			playMut;
	PlayerCommand   playCommand;
	PlayerCommand   playState;
	VS_Reader*		CurReader;
	VS_Controller*  CurController;
};
#endif //GL_VIDEO_OUT