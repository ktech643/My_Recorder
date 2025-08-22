package com.checkmate.android.util.rtsp;

public class TextOverlayOption {
    public int bUseOverlay = 0;
    public int x0 = 0;
    public int y0 = 0;
    public int fontSize = 0;
    public String fontPath = "";
    public int bUseBox = 0;
    public String boxColor = "";
    public String fontColor = "";
    public String overlayText = "";
    public boolean check() {
        return (bUseOverlay == 1) && (!fontPath.isEmpty()) && (!overlayText.isEmpty());
    }
}
