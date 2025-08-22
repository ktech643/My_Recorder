package com.checkmate.android.util.rtsp;

import android.os.Parcel;
import android.os.Parcelable;

public class PushOptParc implements Parcelable {
    public EncOpt pushOpt = new EncOpt();
    public EncOpt writeOpt = new EncOpt();
    public TextOverlayOption overlay = new TextOverlayOption();

    @Override
    public int describeContents() {
        return 0;
    }

    public static void writeEncOptToParcel(Parcel out, EncOpt opt) {
        out.writeInt(opt.width);
        out.writeInt(opt.framerate);
        out.writeInt(opt.height);
        out.writeInt(opt.bitrate);
        out.writeString(opt.codecName);
        out.writeInt(opt.disableAudio ? 1 : 0);
    }

    public static void readEncOptToParcel(Parcel in, EncOpt opt) {
        opt.width = in.readInt();
        opt.framerate = in.readInt();
        opt.height = in.readInt();
        opt.bitrate = in.readInt();
        opt.codecName = in.readString();
        opt.disableAudio = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        writeEncOptToParcel(out, pushOpt);
        writeEncOptToParcel(out, writeOpt);

        out.writeInt(overlay.x0);
        out.writeInt(overlay.y0);
        out.writeInt(overlay.fontSize);
        out.writeString(overlay.boxColor);
        out.writeString(overlay.fontPath);
        out.writeString(overlay.overlayText);
        out.writeString(overlay.fontColor);
        out.writeInt(overlay.bUseBox);
        out.writeInt(overlay.bUseOverlay);
    }
    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<PushOptParc> CREATOR = new Parcelable.Creator<PushOptParc>() {
        public PushOptParc createFromParcel(Parcel in) {
            return new PushOptParc(in);
        }

        public PushOptParc[] newArray(int size) {
            return new PushOptParc[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values


    public PushOptParc(Parcel in) {
        readEncOptToParcel(in, pushOpt);
        readEncOptToParcel(in, writeOpt);
        overlay.x0 = in.readInt();
        overlay.y0= in.readInt();
        overlay.fontSize= in.readInt();
        overlay.boxColor = in.readString();
        overlay.fontPath = in.readString();
        overlay.overlayText = in.readString();
        overlay.fontColor = in.readString();
        overlay.bUseBox = in.readInt();
        overlay.bUseOverlay = in.readInt();
    }
    public PushOptParc() {

    }
}
