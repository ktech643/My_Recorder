#ifndef VIDEOPLAYER_H
#define VIDEOPLAYER_H
#include "../Sources/StreamSource.h"
#include <GLES3/gl3.h>
#include "../VSFilter.h"
#include "../texture.h"
#include "../shader.h"
#include <list>
#include <thread>
#include "../BaseSound.h"
#include "../OboeSound.h"

struct SourceStreamElement {
    AbstractSource* source = NULL;
    int sourceID = -1;
    CodecProperty sourceProp;
    int type;
    std::list<AVFrame*> frames;
    int maxSize = 40;
    int minToStart = 1;
    //int id() {
    //return source->sourceId;
    //}
    ~SourceStreamElement() {
        for(auto &frame:frames) {
            av_frame_free(&frame);
        }
    };
    std::mutex mut;
};

class VideoPlayer : public AbstractOut
{
public:

    VideoPlayer(int _id);
    virtual int  Play();
    void VideoThread();
    virtual void Stop();
    virtual void DisplayCurFrame();
    virtual void AboutUpdate();
    bool pause(int _timeout = 100);
    bool resume(int _timeout = 100);
    virtual AbstractSource *GetCurentSource(int _type) override;
    virtual void SetSource(AbstractSource* _source, int _type);
    virtual void SetSource(AbstractSource* _source, int _type, VSFilter* _curTask);
    void setGl(std::shared_ptr<Shader> _shader, std::shared_ptr<Texture> _texture[3]);
    void frameUpdate(CodecProperty &_stream, AVFrame *_frame);
    void removeFrame(int _type);
    void SourcePrepared(AbstractSource* _source);
    void videoPrepared(CodecProperty& _inProperty);
    void nextFrame();
    bool needUpdate();
    void setSourceIds(std::list<int> _sourceId);
    int GetId() override ;
    bool usedSource(int _source, int _type);
    void clear();
    void seek(int _timeout = 100);
    double getTime();
    void Screen(std::string _path);
    BYTE* getPoint(uint8_t* _data, int _w, int _h, int _linesize, uint8_t* _curArray);
    void setDisableAudio(bool _disable);
private:
    AVFrame* getVFirstFrame();
    SourceStreamElement vSource;
    SourceStreamElement aSource;
    GLuint			    displayTexture;
    std::unique_ptr<OboeSound>          sound;
    PlayerCommand   playCommand = PLAYER_WAIT;
    PlayerCommand   playState;
    int64_t         currentTime = -1;
    AVRational      framerate;
    timespec        firstTime;
    timespec        secondTime;
    VSFilter        preAudioFilter;
    std::shared_ptr<Shader>          shader;
    std::shared_ptr<Texture>         yuvTextures[3];
    VSFilter        preVideoFilter;
    std::list<int>  sourceIds;
    std::thread     vThread;
    AVRational      vTimeBase = {0, 0};
    bool            bNeedUpdate = false;
    int             id;
    bool            bAudio = false;
    bool            bVideo = false;
    std::vector<uint8_t> m_y;
    std::vector<uint8_t> m_u;
    std::vector<uint8_t> m_v;
    int messageCounter = 0;
    std::mutex      writeMutex;
    bool            bFirstVideo = false;
    bool            bNeedScreen = false;
    std::string     screenDir;
    CodecProperty   videoProperty;
    bool            disableAudio = false;
};

#endif // VIDEOPLAYER_H
