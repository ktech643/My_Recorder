#ifndef TV_SERVER_FORMATTEST_H
#define TV_SERVER_FORMATTEST_H

#include "ffheader.h"
#include <vector>
#include <string>
#include "CodecProperty.h"

class FormatTest {
public:
    FormatTest();
    void addStream(AVCodecParameters* _par);
    void addStreams(std::vector<AVCodecParameters*> _pars);
    void removeStream(AVCodecParameters* _par);
    int check(std::string _path, std::string _format, bool isRealTime);
    AVCodecID getDefaultCodec(std::string _path, std::string _format, AVMediaType _type);
    void addStream(CodecProperty& _par);
private:
    uint8_t buffer[100000]; // internal buffer for ffmpeg
    int bufferSize = 100000;
    std::vector<AVCodecParameters*> m_codecPars;
};


#endif //TV_SERVER_FORMATTEST_H
