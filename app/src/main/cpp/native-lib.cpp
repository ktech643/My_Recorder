#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include "Managers/StreamManager.h"
#include <android/log.h>
#include <android/multinetwork.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#define LOG_TAG "native-lib"
#define INFO_PRINT(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ERR_PRINT(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern AboutErrorFunc AboutError;
extern AboutMessageFunc AboutMessage;

std::vector<std::string> errlog;

static const GLfloat squareVertices[] = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f,  1.0f,
        1.0f,  1.0f,
};
GLfloat textureVertices[] = {
        0, 1, 1, 1, 0, 0, 1, 0
};

GLint ATTRIB_VERTEX = 0;
GLint ATTRIB_TEXTUREPOSITON = 1;
std::shared_ptr<StreamManager> manager(new StreamManager());

std::shared_ptr<Texture> yuvTexture[3];
std::shared_ptr<Shader>  shader;

static void Error (int error, const char* _str, int _deviseType, int _devise) {
    ERR_PRINT("NO:%d DT:%d D:%d -> %s", error, _deviseType, _devise, _str);
    if(errlog.size() < 300) {
        errlog.push_back(_str);
    }
}

int curLogLvl = 32;
void fflogcall(int _logLevel, const char* _ch) {
    if(_logLevel > curLogLvl)
        return;
    AndroidFileLogger::getInstance().fflog(_logLevel, _ch);
}

static void Error (int error, const std::string &str, long value) {
    ERR_PRINT("[APP NATIVE LIB] ERRNO:%d VAL:%ld -> %s", error, value, str.c_str());
}

static void Message (const std::string &str, long value) {
    INFO_PRINT("[APP NATIVE LIB] VAL:%ld -> %s", value, str.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_checkmate_android_util_MainActivity_StopVideo(JNIEnv *_jni, jobject /* this */, jint _sourceId) {
    int sourceId = _sourceId;
    manager->Stop(sourceId);
    manager->DeleteSource(sourceId);
    return true;
}

static void printGLString(const char *name, GLenum s) {
    const char *v = (const char *) glGetString(s);
    INFO_PRINT("GL %s = %s\n", name, v);
}

static void prepareTexture(std::string name, std::shared_ptr<Texture>& _texture){
    if(_texture.get() == NULL)
        _texture.reset(new Texture());
    _texture->initTexture();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_checkmate_android_util_MainActivity_InitGL(JNIEnv *env, jobject thiz) {
    printGLString("Version", GL_VERSION);
    printGLString("Vendor", GL_VENDOR);
    printGLString("Renderer", GL_RENDERER);
    printGLString("Extensions", GL_EXTENSIONS);
    INFO_PRINT("collor is set");
    glEnable(GL_BLEND);
    glEnable(GL_DEPTH_TEST);
    INFO_PRINT("gl inited");
    shader.reset(new Shader([&](bool error, const char *msg){
        if(error) Error(0, msg, 0);
        else Message(msg, 0);
    }));

    ATTRIB_VERTEX = shader->get_attrib_location("position");
    if(ATTRIB_VERTEX < 0) {
        Error(1, "Attribute not found (ATTRIB_VERTEX)", ATTRIB_VERTEX);
        return false;
    }
    ATTRIB_TEXTUREPOSITON = shader->get_attrib_location("inputTextureCoordinate");
    if(ATTRIB_TEXTUREPOSITON < 0) {
        Error(1, "Attribute not found (ATTRIB_TEXTUREPOSITON)", ATTRIB_TEXTUREPOSITON);
        return false;
    }
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    prepareTexture("yTexture",yuvTexture[0]);
    prepareTexture("uTexture",yuvTexture[1]);
    prepareTexture("vTexture",yuvTexture[2]);
    shader->dropCurTexture();
    ffloger::getInstance().setFFmpegCallback(curLogLvl, fflogcall); //todo перенести в другую часть кода
    return true;
}

bool isLoaded = false;

extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_UpdateGl(JNIEnv *env, jobject thiz) {
    int playerId;
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glClearColor(0,0,0,1);
    std::unique_lock lock(manager->getGlMutext());
    manager->getCurGlPlayer() == NULL ? playerId = -1 : playerId = manager->getCurGlPlayer()->GetId();
    if(playerId == -1) {
        if(isLoaded)
            isLoaded = false;
        return;
    }
    if(!isLoaded) {
        if(!manager->needUpdate(playerId)) {
            return;
        } else {
            isLoaded = true;
        }
    }
    if(!shader->bind())
        return;
    if(manager->needUpdate(playerId)) {
        manager->DisplayCurFrame(playerId);
        shader->activeTexture("yTexture", yuvTexture[0]->texture_id());
        shader->activeTexture("uTexture", yuvTexture[1]->texture_id());
        shader->activeTexture("vTexture", yuvTexture[2]->texture_id());
        shader->dropCurTexture();
    }
    glEnableVertexAttribArray(ATTRIB_TEXTUREPOSITON);
    glVertexAttribPointer(ATTRIB_TEXTUREPOSITON, 2, GL_FLOAT, 0, 0, textureVertices);
    glEnableVertexAttribArray(ATTRIB_VERTEX);
    glVertexAttribPointer(ATTRIB_VERTEX, 2, GL_FLOAT, 0, 0, squareVertices);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray(ATTRIB_VERTEX);
    glDisableVertexAttribArray(ATTRIB_TEXTUREPOSITON);
    GLenum error = glGetError();
    if(error) {
        ERR_PRINT("GL ERROR %d", error);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_DelGL(JNIEnv *env, jobject thiz) {
    shader.reset();
    yuvTexture[0].reset();
    yuvTexture[1].reset();
    yuvTexture[2].reset();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_ChangeGL(JNIEnv *env, jobject thiz, jint _w, jint _h) {
    glViewport(0,0,_w,_h);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_checkmate_android_util_MainActivity_PauseVideo(JNIEnv *env, jobject thiz) {
    // TODO: implement PauseVideo()
    VideoPlayer* player = manager->getCurGlPlayer();
    if(player == NULL)
        return false;
    return player->pause(1000);
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_checkmate_android_util_MainActivity_ResumeVideo(JNIEnv *env, jobject thiz) {
    // TODO: implement ResumeVideo()
    VideoPlayer* player = manager->getCurGlPlayer();
    if(player == NULL)
        return false;
    return player->resume(1000);
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_Seek(JNIEnv *env, jobject thiz, jdouble _time, jint _sourceId) {
    if(_sourceId > -1)
        manager->seekFile(_sourceId, _time);
}


JNIEnv *jenvIn = NULL;
JNIEnv *jenvOut = NULL;
jclass clsIn = NULL;
jclass clsOut = NULL;
jmethodID onConnectionState = NULL;
jmethodID onInputState = NULL;
jmethodID onOutputState = NULL;
jmethodID forceStop = NULL;
JavaVM *jnvm = NULL;
net_handle_t netOne = 0;
net_handle_t netTwo = 0;
void selectNetwork(int _socket, int _id) {
    int ret = 0;

    if(_id == 0) {
        AndroidFileLogger::getInstance().writeSD("Try selected network %d", 2, 1, netOne);
        ret = android_setsocknetwork(netOne, _socket);
    }
    else {
        AndroidFileLogger::getInstance().writeSD("Try selected network %d", 2, 1, netTwo);
        ret = android_setsocknetwork(netTwo, _socket);
    }
    if(ret < 0) {
        AndroidFileLogger::getInstance().writeSD("Select network fail, java error %d", 2, 1, errno);
    } else
        AndroidFileLogger::getInstance().writeSD("Select network success", 2, 1);
}

void OnRecconect(int _hSource, bool _succes) {
    jenvIn->CallStaticVoidMethod(clsIn, onConnectionState, _hSource, _succes);
}

void OnChangeInputState(int _hSource, int _state) {
    jenvIn->CallStaticVoidMethod(clsIn, onInputState, _hSource, _state);
};

void ForceStop() {
    jenvOut->CallStaticVoidMethod(clsOut, forceStop);
}

void OnChangeOutputState(int _hOut, int _state) {
    /*if(!outputAttached) {
        jnvm->AttachCurrentThread(&jenvOut, NULL);
        clsOut = jenvIn->FindClass("com/tvapp/tvserver/MainActivity");
        onOutputState = jenvIn->GetStaticMethodID(clsOut, "AboutChangeOutputState", "(II)V");
        outputAttached = true;
    }*/
    jenvOut->CallStaticVoidMethod(clsOut, onOutputState, _hOut, _state);
};

extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_setCallbacks(JNIEnv *env, jobject thiz) {
    env->GetJavaVM(&jnvm);

    manager->onReconnect = OnRecconect;
    manager->forceStop = ForceStop;
    manager->onInputStateChange = OnChangeInputState;
    manager->onOutputStateChange = OnChangeOutputState;

}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_SetMute(JNIEnv *env, jobject thiz, jboolean _mute) {
    manager->muteAudio(_mute);
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_setNetworks(JNIEnv *env, jobject thiz, jlong _network1,
                                                 jlong _network2) {
    netOne = (net_handle_t )_network1;
    netTwo = (net_handle_t )_network2;
    // TODO: implement setNetworks()
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_PullVideo(JNIEnv *env, jobject thiz, jint _sourceId) {
    jenvOut = env;
    clsOut = jenvOut->GetObjectClass(thiz);;
    onOutputState = jenvOut->GetStaticMethodID(clsOut, "AboutChangeOutputState", "(II)V");
    forceStop = jenvOut->GetStaticMethodID(clsOut, "FroceStop", "()V");
    manager->pushThread(_sourceId);
    AndroidFileLogger::getInstance().writeSD("push closed", 2, 4);
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_openLog(JNIEnv *env, jobject thiz, jstring _path) {
    std::string  path = env->GetStringUTFChars(_path, 0);
    if(!AndroidFileLogger::getInstance().isOpen()) {
        time_t     now = time(0);
        struct tm  tstruct;
        char       buf[80];
        tstruct = *localtime(&now);
        strftime(buf, sizeof(buf), "%m-%d_%H_%M_%S", &tstruct);
        path += "/log_" + std::string(buf) + ".txt";

        AndroidFileLogger::getInstance().open(path.c_str());
        int er = errno;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_writeLog(JNIEnv *env, jclass thiz, jstring _message,
                                              jint _space, jint _desc) {
    std::string  message = env->GetStringUTFChars(_message, 0);
    AndroidFileLogger::getInstance().writeSD(message.c_str(), _space, _desc);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_checkmate_android_util_MainActivity_AddSource(JNIEnv *env, jobject thiz, jstring _video,
                                               jint _protocol_type) {
    InputProperty prop;
    const char *nativeString = env->GetStringUTFChars(_video, 0);
    const int protType = _protocol_type;
    strcpy(prop.initialStr, nativeString);
    prop.reconnectWaitTime = 5000;
    prop.timeout = 10000;
    prop.connectionWaitTime = 20000;
    prop.reconnectCount = -1;
    prop.readCount = 0;
    prop.bAboutFrameUpdate = true;
    prop.bAboutPacketUpdate = true;
    prop.framerate = {0,0};
    if(!_protocol_type)
        strcpy(prop.rtspTransport, "udp");
    else
        strcpy(prop.rtspTransport, "tcp");
    manager->selectNetwork = selectNetwork;
    return manager->AddSource(prop, RTSP);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_checkmate_android_util_MainActivity_StartSource(JNIEnv *env, jobject thiz, jint _sourceId) {
    jenvIn = env;
    clsIn = jenvIn->GetObjectClass(thiz);;
    onConnectionState = jenvIn->GetStaticMethodID(clsIn, "AboutReconnect", "(IZ)V");
    onInputState = jenvIn->GetStaticMethodID(clsIn, "AboutChangeInputState", "(II)V");
    const int sourceID = _sourceId;
    manager->Start(sourceID);
    return true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_checkmate_android_util_MainActivity_AddDepOut(JNIEnv *env, jobject thiz, jint _sourceId,
                                               jstring _out, jint _protType, jstring _segTime, jint _depOutID) {
    const char *outP = env->GetStringUTFChars(_out, 0);

    const int sourceID = _sourceId;
    const char *segTime = env->GetStringUTFChars(_segTime, 0);
    int outID = -1;
    if(outP != NULL && outP[0] != '\0')
        outID = manager->AddOut(outP, _protType, sourceID, _depOutID);
    else {
        env->ReleaseStringUTFChars(_out, outP);
        env->ReleaseStringUTFChars(_segTime, segTime);
        return -1;
    }
    if(segTime != NULL && segTime[0] != '\0') {
        FFWriter *writer = NULL;
        if(_depOutID != -1)
            writer = ((FFWriter *)manager->getOut(_depOutID))->getDepWriter();
        else
            writer = (FFWriter *)manager->getOut(outID);
        if(writer == NULL) {
            AndroidFileLogger::getInstance().writeSD("Can't find FFWriter with id %d main id %d", 0,
                                                     1, outID, _depOutID);
            return -1;
        }
        writer->addFormatDict("strftime", "1");
        if(segTime[0] != '\0' && segTime != NULL) {
            writer->addFormatDict("segment_time", segTime);
            writer->addFormatDict("reset_timestamps", "1");
        }
    }
    env->ReleaseStringUTFChars(_out, outP);
    env->ReleaseStringUTFChars(_segTime, segTime);
    return outID;
}

void setCharArray(JNIEnv *env, jstring _str, char *_ch) {
    const char * ch = NULL;
    std::string str;
    ch = env->GetStringUTFChars(_str, 0);
    str = ch;
    memcpy(_ch, str.c_str(), str.size());
    env->ReleaseStringUTFChars(_str, ch);
}

void setString(JNIEnv *env, jstring _str, std::string &_stdstr) {
    const char * ch = NULL;
    ch = env->GetStringUTFChars(_str, 0);
    _stdstr = ch;
    env->ReleaseStringUTFChars(_str, ch);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_Screen(JNIEnv *env, jobject thiz, jstring _dir) {
    VideoPlayer* player = manager->getCurGlPlayer();
    if(player == NULL)
        return;
    const char *outP = env->GetStringUTFChars(_dir, 0);
    player->Screen(outP);
    env->ReleaseStringUTFChars(_dir, outP);
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_SetEncoderOpt(JNIEnv *env, jobject thiz, jint _out_id, jobject _pushOpt) {
    CodecProperty prop;
    prop.type = VS_VIDEO;
    jclass cls = env->GetObjectClass(_pushOpt);
    jfieldID fid;
    int intMember;
    fid = env->GetFieldID(cls,"framerate", "I");
    if((intMember = env->GetIntField(_pushOpt, fid)) != -1)
        prop.framerate = AVRational{intMember, 1};

    fid = env->GetFieldID(cls, "width", "I");
    if((intMember = env->GetIntField(_pushOpt, fid)) != -1) {
        prop.w = intMember;
    }

    fid = env->GetFieldID(cls, "height", "I");
    if((intMember = env->GetIntField(_pushOpt, fid)) != -1) {
        prop.h = intMember;
    }

    fid = env->GetFieldID(cls, "bitrate", "I");
    if((intMember = env->GetIntField(_pushOpt, fid)) != -1) {
        prop.bitRate = intMember;
    }
    prop.hwCodecType = CodecProperty::HWACCEL_MEDIACODEC;
    fid = env->GetFieldID(cls, "codecName", "Ljava/lang/String;");
    std::string codecName = "";
    setString(env, static_cast<jstring>(env->GetObjectField(_pushOpt, fid)), codecName);
    if(!codecName.empty()) {
        AVCodec *codec = avcodec_find_encoder_by_name(codecName.c_str());
        if(codec != NULL)
            prop.codecID = codec->id;
    }
    if(_out_id == -1) {
        manager->setPlayerOpt(prop);
        return;
    }
    FFWriter *writer = (FFWriter *) manager->getOut(_out_id);
    if(writer == NULL)
        return;
    writer->setOutputCodecPar(prop, VS_VIDEO);

    CodecProperty aprop;
    prop.type = VS_AUDIO;
    fid = env->GetFieldID(cls, "disableAudio", "Z");
    aprop.disable = env->GetBooleanField(_pushOpt, fid);
    writer->setOutputCodecPar(aprop, VS_AUDIO);
    return;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_checkmate_android_util_MainActivity_AddGLPlayer(JNIEnv *env, jobject thiz, jint _source_id) {
    const int sourceID = _source_id;
    return manager->AddGlPlayer(shader, yuvTexture, std::list<int>{_source_id});
}
/*public boolean bUseOverlay = false;
public int x0 = 0;
public int y0 = 0;
public int fontSize = 0;
public String fontPath;
public boolean bUseBox;
public String boxColor;
public String fontColor;
public String overlayText;*/


extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_addOverlayToOut(JNIEnv *env, jobject thiz, jobject _over, jint _outID) {
    jclass cls;
    jfieldID fid;
    TextOption textOption;
    cls = env->GetObjectClass(_over);
    fid = env->GetFieldID(cls,"bUseOverlay", "I");
    textOption.overlayEnabled = env->GetIntField(_over, fid);

    fid = env->GetFieldID(cls,"x0", "I");
    textOption.x = env->GetIntField(_over, fid);

    fid = env->GetFieldID(cls,"y0", "I");
    textOption.y = env->GetIntField(_over, fid);

    fid = env->GetFieldID(cls,"fontSize", "I");
    textOption.fontsize = env->GetIntField(_over, fid);

    fid = env->GetFieldID(cls,"fontPath", "Ljava/lang/String;");
    setString(env, static_cast<jstring>(env->GetObjectField(_over, fid)), textOption.fontfile);

    fid = env->GetFieldID(cls,"fontColor", "Ljava/lang/String;");
    setString(env, static_cast<jstring>(env->GetObjectField(_over, fid)), textOption.fontcolor_expr);

    fid = env->GetFieldID(cls,"overlayText", "Ljava/lang/String;");
    setString(env, static_cast<jstring>(env->GetObjectField(_over, fid)), textOption.text);

    fid = env->GetFieldID(cls,"bUseBox", "I");
    textOption.box = env->GetIntField(_over, fid);

    fid = env->GetFieldID(cls,"boxColor", "Ljava/lang/String;");
    setString(env, static_cast<jstring>(env->GetObjectField(_over, fid)), textOption.boxcolor_expr);

    manager->setTextOpt(textOption);

}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_GlPlayerDisableAudio(JNIEnv *env, jobject thiz, jint _glOutID) {
    VideoPlayer *player = static_cast<VideoPlayer*>(manager->getOut(_glOutID));
    player->setDisableAudio(true);
}extern "C"
JNIEXPORT void JNICALL
Java_com_checkmate_android_util_MainActivity_SetMicMode(JNIEnv *env, jobject thiz, jint _source_id) {
    manager->useMicOut(_source_id);
}