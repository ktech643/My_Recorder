package com.checkmate.android.util;

import android.media.AudioRecord;
import android.util.Log;

import com.wmspanel.libstream.Streamer;

import static android.media.AudioRecord.READ_BLOCKING;

public class UsbMicThread extends Thread {
    private static final String TAG = "UsbMicThread";

    private final AudioRecord mAudioRecord;
    private final Streamer mStreamer, mRecorder;
    private final byte[] mBuffer;

    /**
     * @param audioRecord  already‑initialized AudioRecord
     * @param streamer     your WMSPanel Streamer for live audio
     * @param recorder     your WMSPanel Streamer for local recording
     */
    public UsbMicThread(
            AudioRecord audioRecord,
            Streamer streamer,
            Streamer recorder
    ) {
        mAudioRecord = audioRecord;
        mStreamer    = streamer;
        mRecorder    = recorder;
        // We'll read in chunks equal to the AudioRecord buffer
        mBuffer = new byte[audioRecord.getBufferSizeInFrames() * 2];
    }

    @Override
    public void run() {
        try {
            mAudioRecord.startRecording();
            while (!isInterrupted()) {
                // ⚠️ Must use READ_BLOCKING or Android complains
                int len = mAudioRecord.read(mBuffer, 0, mBuffer.length, READ_BLOCKING);
                if (len > 0) {
                    if (mStreamer != null) mStreamer.writePcmData(mBuffer);
                    if (mRecorder != null) mRecorder.writePcmData(mBuffer);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Mic loop failed", e);
        } finally {
            try { mAudioRecord.stop(); } catch (Exception ignored) {}
            mAudioRecord.release();
        }
    }
}
