#ifndef FF_HEADER
#define FF_HEADER

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavformat/avio.h"
#include "libavdevice/avdevice.h"
#ifdef _QSV
#include "libavcodec/qsv.h"

#include "libavutil/hwcontext.h"
#include "libavutil/hwcontext_internal.h"
#include "libavcodec/qsvdec.h"
#include "libavutil/hwcontext_qsv.h"
#endif
#include "libavutil/pixdesc.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include "libavutil/time.h"
}
#define VS_VIDEO   1
#define VS_AUDIO   2

#include <memory>
#include <functional>

template<typename T>
using del_unique_ptr = std::unique_ptr<T,std::function<void(T*)>>;


class VSPacket {
public:
    VSPacket() {
        unit = av_packet_alloc();
    }
    VSPacket(AVPacket* _unit) {
        unit = av_packet_alloc();
        av_packet_ref(unit, _unit);
    }
    ~VSPacket() {
        if(unit)
            av_packet_free(&unit);
    }
    operator AVPacket*() {
        return unit;
    }
    VSPacket(const VSPacket& _packet) {
        unit = av_packet_alloc();
        av_packet_ref(unit,_packet.unit);
    }
    VSPacket(VSPacket&& _packet) {
        std::swap(unit, _packet.unit);
    }
    AVPacket* unit = nullptr;
};

class VSFrame {
public:
    VSFrame() {
        unit = av_frame_alloc();
    }
    VSFrame(AVFrame* _unit) {
        unit = av_frame_alloc();
        av_frame_ref(unit, _unit);
    }
    ~VSFrame() {
        if(unit)
            av_frame_free(&unit);
    }
    operator AVFrame*() {
        return unit;
    }
    VSFrame(const VSFrame& _frame) {
        unit = av_frame_alloc();
        av_frame_ref(unit, _frame.unit);
    }
    VSFrame(VSFrame&& _frame) {
        unit = _frame.unit;
        _frame.unit = nullptr;
        //std::swap(unit, _frame.unit);
    }
    AVFrame* unit = nullptr;
};


struct DecodeContext {
    AVBufferRef *hw_device_ref;
    AVBufferRef *hw_frames_ctx;
};

#ifdef _QSV
static void mids_buf_free(void *opaque, uint8_t *data);
static int qsv_setup_mids(mfxFrameAllocResponse *resp, AVBufferRef *hw_frames_ref,
	AVBufferRef *mids_buf);
static AVBufferRef *qsv_create_mids(AVBufferRef *hw_frames_ref);
static enum AVPixelFormat qsv_map_fourcc(uint32_t fourcc);
static mfxStatus qsv_frame_alloc(mfxHDL pthis, mfxFrameAllocRequest *req,
	mfxFrameAllocResponse *resp);
static mfxStatus qsv_frame_free(mfxHDL pthis, mfxFrameAllocResponse *resp);
static mfxStatus qsv_frame_lock(mfxHDL pthis, mfxMemId mid, mfxFrameData *ptr);
static mfxStatus qsv_frame_unlock(mfxHDL pthis, mfxMemId mid, mfxFrameData *ptr);
static mfxStatus qsv_frame_get_hdl(mfxHDL pthis, mfxMemId mid, mfxHDL *hdl);
int ff_qsv_init_session_device1(AVCodecContext *avctx, mfxSession *psession,
	AVBufferRef *device_ref, const char *load_plugins);
int ff_qsv_init_session_frames1(AVCodecContext *avctx, mfxSession *psession,
	QSVFramesContext *qsv_frames_ctx,
	const char *load_plugins, int opaque);
int alloc_frame(AVBufferRef *hw_frames_ctx, QSVFrame *frame, mfxFrameInfo _info, QSVFramesContext* _qsvFrameCtx);
#endif


#endif // !FF_HEADER

