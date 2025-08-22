//
// Created by steri on 01.04.2021.
//

#ifndef TV_SERVER_FFENCODER_H
#define TV_SERVER_FFENCODER_H

#include "VS_Abstract.h"
#include "ffheader.h"
#include "vscodec.h"


class FFEncoder {
public:
    enum EncodeState {ENC_NONE, ENC_PREPARED, ENC_OPENED, ENC_ERROR};
    FFEncoder();
    bool initEncoder(CodecProperty _prop, bool _useMediaCodec = false);
    bool getPars(AVCodecParameters* _par);
    CodecProperty getProperty();
    ~FFEncoder();
    void close();
    int sendFrame(AVFrame* _frame);
    int resavePacket(AVPacket* _packet);
    bool isInit();
    bool isUsed();
    bool isOpen();
    bool open();
private:
    bool useMediaCodec = false;
    EncodeState m_state = ENC_NONE;
    CodecProperty m_curProperty;
    AVCodecContext *m_context = NULL;
    AVCodec *m_codec = NULL;
    VSCodec codec;

};


#endif //TV_SERVER_FFENCODER_H
