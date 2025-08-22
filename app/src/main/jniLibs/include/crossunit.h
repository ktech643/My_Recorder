#ifndef CROSSUNIT_H
#define CROSSUNIT_H

#ifdef _LIN
#include <pthread.h>
#else

#endif



class MyMutex {
public:
#ifdef _WIN
    MyMutex() {
        InitializeCriticalSection(&mutex);
    }
    ~MyMutex() {
        DeleteCriticalSection(&mutex);
    }
    void Lock() {
        EnterCriticalSection(&mutex);
    }
    void Unlock() {
        LeaveCriticalSection(&mutex);
    }
    CRITICAL_SECTION mutex;
#else
#ifdef _LIN
    MyMutex() {
        pthread_mutex_init(&mutex, NULL);
    }
    ~MyMutex() {
        pthread_mutex_destroy(&mutex);
    }
    void Lock() {
        pthread_mutex_lock(&mutex);
    }
    void Unlock() {
        pthread_mutex_unlock(&mutex);
    }
    pthread_mutex_t        mutex;
#endif
#endif

    // _WIN

};
#endif // CROSSUNIT_H
