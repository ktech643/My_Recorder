package com.checkmate.android.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;

import java.util.Arrays;

public class MicThread extends Thread {
    private static final String TAG = "MicThread";

    public Streamer mStreamer;
    public Streamer mRecorder;
    private AudioConfig mConfig;
    private AudioRecord mAudioRecord;

    public MicThread(AudioConfig config) {
        super();
        mConfig = config;
    }

    @Override
    public void run() {
        try {
            final int audioSource = mConfig.audioSource;
            final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            final int sampleRate = mConfig.sampleRate;
            final int channelCount = mConfig.channelCount;

            final int channelConfig = channelCount == 1 ?
                    AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;

            final int bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    audioFormat);

            if (bufferSize <= 0) {
                throw new Exception("Invalid buffer size");
            }

            mAudioRecord = new AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize * 4);

            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new Exception("AudioRecord initialization failed");
            }

            final int sampleSize = channelCount * 2; // 16-bit samples
            final int SAMPLES_PER_FRAME = 1024; // AAC frame size

            final byte[] audioBuffer = new byte[SAMPLES_PER_FRAME * sampleSize];

            mAudioRecord.startRecording();

            while (!isInterrupted()) {
                final int audioInputLength = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);

                if (audioInputLength > 0) {
                    if (mStreamer != null) {
                        mStreamer.writePcmData(audioBuffer);
                    }
                    if (mRecorder != null) {
                        mRecorder.writePcmData(audioBuffer);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in MicThread: " + Log.getStackTraceString(e));
        } finally {
            // Ensure resources are released in the finally block
            cleanup();
        }
    }

    private void cleanup() {
        if (mAudioRecord != null) {
            try {
                // Make sure we stop and release the AudioRecord properly
                mAudioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage());
            } finally {
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }

        // If mStreamer or mRecorder need to be closed or cleaned up, do so here
        if (mStreamer != null) {
            // Example: mStreamer.release(); // assuming a release method exists
            mStreamer = null;
        }

        if (mRecorder != null) {
            // Example: mRecorder.release();
            mRecorder = null;
        }
    }
}