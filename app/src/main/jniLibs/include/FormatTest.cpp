//
// Created by steri on 21.03.2021.
//

#include "FormatTest.h"

FormatTest::FormatTest() {

}

static int IOWriteFunc(void *data, uint8_t *buf, int buf_size) {
    return buf_size;
}

static int64_t seek(void *opaque, int64_t offset, int whence) {
    return 1;
}



void FormatTest::addStream(AVCodecParameters *_par) {
    m_codecPars.push_back(_par);
}

int FormatTest::check(std::string _path, std::string _format, bool isRealTime) {
    if(isRealTime)
        return  0;          //to do c реалтайм потоками надо действовать иначе...
    int ret = 0;
    AVFormatContext *context = NULL;
    ret = avformat_alloc_output_context2(&context, NULL, NULL, _path.c_str());
    AVIOContext *ioCtx = NULL;
    if(ret < 0)
        return ret;
    for(int i = 0; i < m_codecPars.size(); ++i) {
        avformat_new_stream(context, avcodec_find_encoder(m_codecPars[i]->codec_id));
        avcodec_parameters_copy(context->streams[i]->codecpar, m_codecPars[i]);
    }
    if(!isRealTime) {
        ioCtx = avio_alloc_context(
                buffer, bufferSize, // internal buffer and its size
                1,            // write flag (1=true, 0=false)
                (void *) this,  // user data, will be passed to our callback functions
                NULL,
                IOWriteFunc,            // no writing
                seek
        );
        context->pb = ioCtx;
    }
    ret = avformat_init_output(context, NULL);
    if(ioCtx) {
        avio_context_free(&ioCtx);
        context->pb = NULL;
    }
    if(context)
        avformat_free_context(context);
    return ret;
}

void FormatTest::addStreams(std::vector<AVCodecParameters *> _pars) {
    m_codecPars = _pars;
}

void FormatTest::removeStream(AVCodecParameters *_par) {
    m_codecPars.erase(std::find(m_codecPars.begin(), m_codecPars.end(), _par));
}

AVCodecID FormatTest::getDefaultCodec(std::string _path, std::string _format, AVMediaType _type) {
    int ret = 0;
    AVOutputFormat *format = av_guess_format(NULL, _path.c_str(), NULL);
    if(ret < 0)
        return AV_CODEC_ID_NONE;
    if(_type == AVMEDIA_TYPE_VIDEO) {
        return format->video_codec;
    }
    if(_type == AVMEDIA_TYPE_AUDIO) {
        return format->audio_codec;
    }
    return AV_CODEC_ID_NONE;
}
