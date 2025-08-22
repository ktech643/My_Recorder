//
// Created by steri on 23.04.2021.
//

#ifndef TV_SERVER_FFBITSTREAMFILTER_H
#define TV_SERVER_FFBITSTREAMFILTER_H

#include "ffheader.h"
#include <list>
#include <string>
#include <variant>
#include <mutex>

using VariantValue = std::variant<std::string,int,AVRational>;

struct OptItem {
    std::string key = "";

    VariantValue value = 0;
    enum OptType{STRING_OPT = 0, INT_OPT, AVRATIONAL_OPT};
    int setOption(void* _obj, bool _flag = 0) {
        int ret = 0;
        switch (value.index()) {
            case STRING_OPT:
                ret = av_opt_set(_obj, key.c_str(), std::get<std::string>(value).c_str(), _flag);
                break;
            case INT_OPT:
                ret = av_opt_set_int(_obj, key.c_str(), std::get<int>(value), _flag);
                break;
            case AVRATIONAL_OPT:
                ret = av_opt_set_q(_obj, key.c_str(), std::get<AVRational>(value), _flag);
                break;
        }
        return ret;
    }
    std::string getStrValue() {
        switch (value.index()) {
            case STRING_OPT:
                return std::get<std::string>(value);
            case INT_OPT:
                return std::to_string(std::get<int>(value));
            case AVRATIONAL_OPT:
                return std::to_string(av_q2d(std::get<AVRational>(value)));
        }
        return "";
    }
    OptItem(std::string _key, VariantValue _value) : key{_key}, value{_value} {}
    OptItem(OptItem &&_opt) = default;
    OptItem(const OptItem &_opt) = default;
};


using OptionList = std::list<OptItem> ;

class FFBitStreamFilter {
public:
    FFBitStreamFilter(int _id);
    FFBitStreamFilter(FFBitStreamFilter&&) = default;
    FFBitStreamFilter(const FFBitStreamFilter&) = delete;
    ~FFBitStreamFilter();
    int sendPacket(VSPacket& _packet);
    int receivePacket(VSPacket& _packet);
    int initFilter(std::string _bsfname, OptionList _optList, AVCodecParameters *_par = NULL);
    int clear();
    int getID();
private:
    int m_ID = 0;
    AVBSFContext* m_context = nullptr;
    OptionList m_optList;
    std::string m_name = "";
};

using FFBitStreamFilter_ptr = std::unique_ptr<FFBitStreamFilter>;


class FFBitStreamFilterLine {
public:
    int sendPacket(AVPacket* _packet);
    int processing();
    int processOneLine(FFBitStreamFilter_ptr& _lineItem,
                       std::list<VSPacket>& _outList, AVPacket *_inPacket);
    int addLineItem(std::string _bsfname, std::list<OptItem> _items, AVCodecParameters *_par = NULL);
    int receivePacket(AVPacket* _packet);
    ~FFBitStreamFilterLine();
    bool isInit();
private:
    //std::mutex m_inputLock;
    bool m_bInit = false;
    std::list<VSPacket> m_inputPackets;
    std::list<VSPacket> m_resultPackets;
    std::list<FFBitStreamFilter_ptr> m_line;
};


#endif //TV_SERVER_FFBITSTREAMFILTER_H
