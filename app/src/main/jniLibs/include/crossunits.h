#ifndef TV_SERVER_CROSSUNITS_H
#define TV_SERVER_CROSSUNITS_H
#include <stdio.h>
#include <android/asset_manager.h>

#include "ffheader.h"
#include <pthread.h>
extern "C" { ;
    #include <android/log.h>
    #define LOG_TAG "player"
    #define LOGI(...)       __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
    #define LOGE(...)       __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
    #define INFO_PRINT(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
    #define ERR_PRINT(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
    #include "libavcodec/avcodec.h"
    #include "libavformat/avformat.h"
    #include "libavformat/avio.h"
    #include "libswscale/swscale.h"
    #include "libswresample/swresample.h"
    #include "libavutil/opt.h"
    #include "libavutil/imgutils.h"
    #include "libavresample/avresample.h"
}
typedef void*  (*ThreadProcFunc)(void *Parm);
void getTime(timespec &_time);
void CrossPlatformSleep(uint32_t millisec);
void DCrossPlatformSleep(double Sec);
int64_t CrossPlatformCreateThread(ThreadProcFunc Func, void *arg);
double NsDiff(struct timespec* time_stop, struct timespec* time_start);

#endif //TV_SERVER_CROSSUNITS_H
