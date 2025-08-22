//
// Created by steri on 15.10.2020.
//

#ifndef TV_SERVER_VS_CUSTOMIO_H
#define TV_SERVER_VS_CUSTOMIO_H

#include <stdint.h>
#include <unistd.h>
#include <stdio.h>
#include <string>
#include "aesEnc/aes.hpp"
#include <thread>
#include <mutex>
#include <condition_variable>
#include "crossunits.h"
#include <vector>
#include "crossunits.h"
#include <fstream>
#define ENC_BUFFER_SIZE 4096

/** Seek to an absolute offset. */
#define SEEK_SET_1 0
/** Seek relative to the current offset. */
#define SEEK_CUR_1 1
/** Seek relative to the end of the file. */
#define SEEK_END_1 2

int ReadFunc(void* ptr, uint8_t* buf, int buf_size);
int WriteFunc(void* ptr, uint8_t* buf, int buf_size);
int64_t SeekFunc(void* ptr, int64_t pos, int whence);


enum WorkerState {NONE_STATE, WORKER_WAIT_PROCESSING, WORKER_WORK, WORKER_DONE, WORKER_WAIT, WORKER_STOP, WORKER_CLOSE};

class Worker {
public:
    Worker(int _workerId) {
        m_workerId = _workerId;
        m_state = NONE_STATE;
    }
    virtual int workerThread() = 0;
    virtual bool stop() = 0;
    int getId() {
        return m_workerId;
    }
    WorkerState m_state;
protected:
    int m_workerId;
};


class DecryptWorker : public Worker {
public:
    DecryptWorker(int _workerId);
    virtual int workerThread();
    virtual bool stop();
    void GetData(uint8_t* _buf);
    void SetData(uint8_t* _buf, int _size, uint8_t* _outPoint, int _curOffset, int _curSize);
private:
    uint8_t* m_outPoint;
    int m_curOffset;
    int m_curSize;
    AES_ctx m_aes;
    uint8_t m_buffer[ENC_BUFFER_SIZE];
    std::condition_variable m_condVar;
    std::mutex m_mut;
};

using p_thread = std::shared_ptr<std::thread>;

struct WorkerItem {
    WorkerItem(int _workerId) : m_worker(_workerId), m_th(&DecryptWorker::workerThread, &m_worker) {}
    std::thread m_th;
    DecryptWorker m_worker;
    ~WorkerItem() {
        m_worker.stop();
        m_th.join();
    }
};

using p_workerItem = std::shared_ptr<WorkerItem>;

class MultiDecryptor {
public:
    MultiDecryptor(int _decryptorCount);
    void decryptedData(uint8_t *data, uint8_t* _outPointer, int _curOffset, int _curSize);
    void wait();
private:
    std::vector<p_workerItem> workers;
};


class VS_CustomIO {
public:
    VS_CustomIO();
    ~VS_CustomIO();
    virtual bool initCustomIO(const char* _filePath);
    virtual int FileWrite(unsigned char *buf, int buf_size);
    virtual int FileOpen();
    virtual int64_t FileSeek(int64_t pos, int whence);
    virtual int FileClose();
    virtual int FileRead(unsigned char *buf, int buf_size);
    void openOut();
private:
    FILE* m_file = NULL;
    std::string m_filePath;
    AES_ctx aes;
    bool bEncrypted;
    int RealFileSize;
    uint8_t m_buffer[ENC_BUFFER_SIZE];
    MultiDecryptor m_decryptor;
    std::ofstream out;
};


#endif //TV_SERVER_VS_CUSTOMIO_H
