class Converter {
private:
	SwsContext *swsContext = NULL;
	AVFrame *frame = NULL;
	AVFrame *inFrame = av_frame_alloc();
	int srcW = 0;
	int srcH = 0;
	int dstW = 0;
	int dstH = 0;
	int format = -1;
public:

	int InitSwsContext(int _format, int _srcW, int _srcH, int _dstW, int _dstH) {
		swsContext = sws_getContext(_srcW, _srcH, (AVPixelFormat)_format, _dstW, _dstH,
			AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);
		if (swsContext == NULL)
			return -1;
		format = _format;
		srcW = _srcW;
		srcH = _srcH;
		dstW = _dstW;
		dstH = _dstH;
		frame = av_frame_alloc();
		frame->width = _dstW;
		frame->height = _dstH;
		frame->format = AV_PIX_FMT_RGBA;
		av_image_fill_linesizes(frame->linesize, (AVPixelFormat)frame->format, frame->width);
		av_image_fill_linesizes(inFrame->linesize, (AVPixelFormat)format, _srcW);
		return 0;
	}

	int ConvertToRgba(unsigned char** srcBuf, unsigned char *outBuf, int _format, int _srcW, int _srcH, int _dstW, int _dstH) {
		if (swsContext != NULL && (_srcW != srcW || _srcH != srcH || _dstW != dstW || _dstH != dstH || _format != format)) {
			sws_freeContext(swsContext);
			swsContext = NULL;
			av_frame_free(&frame);
		}
		if (swsContext == NULL && InitSwsContext(_format, _srcW, _srcH, _dstW, _dstH) < 0)
			return -1;

		frame->data[0] = outBuf;
		sws_scale(swsContext, (const uint8_t * const *)srcBuf, inFrame->linesize, 0, srcH, frame->data, frame->linesize);
		frame->data[0] = NULL;
		return 0;
	}

	~Converter() {
		if (swsContext != NULL)
			sws_freeContext(swsContext);
		if (frame != NULL)
			av_frame_free(&frame);
		if (inFrame != NULL)
			av_frame_free(&frame);
	}
};