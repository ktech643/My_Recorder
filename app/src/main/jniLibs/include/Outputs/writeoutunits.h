#ifndef WRITEOUTUNITS_H
#define WRITEOUTUNITS_H
#include "../ffheader.h"
#include "list"

struct FrameInfo
{
    uint8_t** frame = NULL;
    int* linesize;
    int h;
    int w;
    int pts;
    int pixFormat;
    int deviceID;
    int deviceType;
    int packType;
    AVRational timebase;
};

struct PacketInfo
{
    uint8_t* frame;
    int len;
    int pts;
    int deviceID;
    int deviceType;
    int packType;
    uint8_t *sps = NULL;
    int spsLen = 0;
    uint8_t *pps = NULL;
    int ppsLen = 0;
    int isKey = 0;
    AVRational timebase;
    PacketInfo() {
        if (sps)
            delete sps;
        if (pps)
            delete pps;
    }
};

struct StreamControl
{
    bool bNeedResample = false;
    bool bNeedScale = false;
    bool bNeedEncode = false;
    bool bNeedRepack = false;
};


struct StreamBufer {
    int streamIndex;
    bool mainSynck = false;
    std::list<AVFrame*> frameList;
    std::list<AVPacket*> packetList;
};


#endif // WRITEOUTUNITS_H
