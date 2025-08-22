package com.checkmate.android.util.rtsp;

public class EncOpt {
    public int framerate = -1;
    public int width = -1;
    public int height = -1;
    public String codecName = "";
    public int bitrate = -1;
    public boolean disableAudio = false;
    public boolean useMic = false;
    public boolean checkMod() {
        EncOpt cc = new EncOpt();
        return (framerate != cc.framerate) ||
                (width != cc.width) ||
                (height != cc.height) ||
                (codecName != cc.codecName) ||
                (bitrate != cc.bitrate) ||
                (disableAudio);
    }
}
