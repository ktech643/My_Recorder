#pragma once
#ifndef FF_FILTER
#define FF_FILTER
#include "VS_Reader.h"
#include "VS_Abstract.h"
#include <string>

struct TextOption {
	bool overlayEnabled = false;
	bool box;
	//Color boxcolor;
	std::string  boxcolor_expr;
	//Color fontcolor;
	std::string fontcolor_expr;
	std::string fontfile;
	std::string text; //"\'%{localtime\\:%X}\'";
	int x = -1;
	int y = -1;
	int fontsize = 26;
	/*TextOption() {
		memset(fontfile, '/0', 300);
		memset(fontcolor_expr, '/0', 20);
		memset(boxcolor_expr, '/0', 20);
		memset(text, '/0', 20);
	}*/
};

class VS_Filters{
public:
    VS_Filters();
    int AddAudioFilter(CodecProperty _inOption, AVSampleFormat _outFormat, int _sampleRate, int _frameSize);
	int AddVideoFilter(CodecProperty _inOption, CodecProperty _outOption, std::string _other = "");
	std::string generateFilterStr(CodecProperty _inOption, CodecProperty _outOption);
	bool addStringToFilterStr(std::string &_filterStr, std::string&& _key, std::string _value, bool _needDesp);
    std::string generateTextOverlay(TextOption _textOption);
	int sendFrame(AVFrame* _inFrame);
	int resaveFrame(AVFrame* _outFrame);
	int FilteringFrame(uint8_t** _frame, int _lineCount);
	AVFrame* FilteringFrame(AVFrame* _inFrame);
	static void SaveFrameAsPNG(CodecProperty &_property, AVFrame *pFrame, int FrameNo, const char* _path);
	AVFilterLink *getOutLink();
	AVRational getCurTimeBase();
	void clear();
    bool isInit();
    ~VS_Filters();
private:
	AVFilterContext *bufferSinkCtx = NULL;
	AVFilterContext *bufferSrcCtx = NULL;
	AVFilterGraph *filterGraph = NULL;
	AVFrame* inFrame = NULL;
	AVFrame* outFrame = NULL;
	bool bInit = false;
	AVRational curTimebase;

};

#endif
