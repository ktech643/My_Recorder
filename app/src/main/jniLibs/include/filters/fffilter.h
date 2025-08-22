#ifndef FFFILTER_H
#define FFFILTER_H


#include "../VS_Abstract.h"
extern "C" {
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
}
#include <string>
#include "libswscale/swscale.h"
#include <libavutil/hwcontext.h>
#include <atomic>
#include <memory>

static int SaveFrameAsPNG(AVFrame *pFrame, int FrameNo, const char* _path);

struct FrameDecodeData {
    int (*post_process)(void *logctx, AVFrame *frame);
    void *post_process_opaque;
    void (*post_process_opaque_free)(void *opaque);
    void *hwaccel_priv;
    void (*hwaccel_priv_free)(void *priv);
};

class FFfilter{
    enum PixelFormat
    {
        LUMINANCE_PIXEL_FORMAT,
        BGRA_PIXEL_FORMAT,
        UNKNOWN_PIXEL_FORMAT
    };
public:
    FFfilter();
    int InitFormat(CodecProperty _inOpt, AVPixelFormat _format);
    int FilteringFrame(AVFrame* _inFrame, AVFrame* _outFrame);
    void Free();
    int IsInit();
    int saveAsPNG(AVFrame* _frame, int _sourceId);
    CodecProperty getProp();
    ~FFfilter();
private:
    AVFilterContext *bufferSinkCtx = NULL;
    AVFilterContext *bufferSrcCtx = NULL;
    AVFilterGraph   *filterGraph = NULL;
    bool              bIsCudaResource;
    int               iFlip = 0;
    size_t nv12Pitch, rgbaPitch;
    int               wInBytes;
    bool    g_bUpdateCSC = true;
    bool    g_bUpdateAll    = false;
    CodecProperty inProperty;
    PixelFormat e_PixFmt;
    AVFrame *r24 = av_frame_alloc();
    std::shared_ptr<std::atomic_int> m_workTime;
    FFfilter *m_toRgb24 = NULL;
    AVFrame *m_swFrame = av_frame_alloc();
    bool m_isInit = false;
    int buffSize = 0;
    MyRect m_rect;

};

#endif // FFFILTER_H
