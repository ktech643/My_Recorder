//
// Created by steri on 12.04.2021.
//

#ifndef TV_SERVER_VSCODEC_H
#define TV_SERVER_VSCODEC_H

#include "ffheader.h"
#include "VS_Abstract.h"
#include <media/NdkMediaCrypto.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaError.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaMuxer.h>
#include <media/NdkMediaExtractor.h>
#include "FFBitStreamFilter.h"

class VSCodec {
public:
    int initEncoder();
    void setCurProperty(CodecProperty &_codecProp);
    CodecProperty getProperty();
    int fillAVCodecPar(AVCodecParameters* _codecPar);
    int sendFrame(AVFrame* _frame);
    int resavedPacket(AVPacket* _pack);
    int copyFrameToBuf(uint8_t *data, size_t size, AVFrame *frame);
    bool addBSF(std::string &&_bsfFilter, std::list<OptItem> &&_opt);
    void processBSF(AVPacket* _packet);
    void free();
    ~VSCodec();
private:

    CodecProperty m_curProperty;
    AMediaCodecBufferInfo m_bufferInfo;
    AMediaCodec* m_codec = NULL;
    int m_waitTime = 10000;
    AMediaFormat* m_format = NULL;
    std::list<int64_t> m_dtsList;
    std::vector<char> m_extraData;
    FFBitStreamFilterLine m_bsfLine;
};


#endif //TV_SERVER_VSCODEC_H
