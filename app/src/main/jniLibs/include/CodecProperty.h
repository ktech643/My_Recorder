//
// Created by steri on 30.04.2021.
//

#ifndef TV_SERVER_CODECPROPERTY_H
#define TV_SERVER_CODECPROPERTY_H

#include <vector>
#include "ffheader.h"

class CodecProperty {
public:
    void diffOne(int &_destValue, int _sourceValue, int _defValue);
    void diffCodec(CodecProperty _source);
    bool changeOne(int &_destValue, int _sourceValue, int _defValue);
    bool changeCodec(CodecProperty _source);
    void configureCodecPar(AVCodecParameters* _codecPar);
    void configureFromPar(AVCodecParameters* _codecPar);
    enum HWAccelType {HWACCEL_NONE = -1, HWACCEL_NV, HWACCEL_QSV, HWACCEL_MEDIACODEC};
    DecodeContext* decodeContext = NULL;
    //abstructSource
    int sourceID = -1;

    //streamopt
    int type = -1;
    bool disable = false;
    bool decoded = true;
    bool encoded = false;
    int64_t startTime = 0;
    int64_t duration = 0;
    AVRational codecTimeBase = {0,0};
    AVRational streamTimeBase = { 0,0 };
    int streamIndex = -1;

    //defCodecOption
    int codecID = 0;
    int format = -1;
    int bitRate = -1;
    int bitRateToler = 4000000;
    std::vector<uint8_t> extradata;
    int level = 0;
    AVRational sampleAspectRatio = {0, 1};
    //int len = -1;

    //videoOpt
    int w = -1;
    int h = -1;
    HWAccelType hwCodecType = HWACCEL_NONE;
    //int swformat = -1;
    int profile = FF_PROFILE_UNKNOWN;
    AVRational framerate = { 0,0 };
    int maxBFrames = -1;
    //int gopSize = -1;
    AVFieldOrder fieldOrder = AV_FIELD_UNKNOWN;
    AVChromaLocation chromaLoc = AVCHROMA_LOC_UNSPECIFIED;
    int gopSize = -1;

    //audioOpt
    int sampleRate = -1;
    int channels = 0;
    int channelLayout = 0;
    int sampleSize = -1;

    bool compare(CodecProperty _prop);
    CodecProperty& operator=(const CodecProperty&) = default;
    CodecProperty(const CodecProperty&) = default;
    CodecProperty(CodecProperty&&) = default;
    CodecProperty(AVCodecParameters* _codecPar);
    CodecProperty(AVStream* _stream, AVFormatContext* _context = NULL);
    CodecProperty() { }
    ~CodecProperty() { }


};


#endif //TV_SERVER_CODECPROPERTY_H
