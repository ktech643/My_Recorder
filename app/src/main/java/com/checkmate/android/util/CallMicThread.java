package com.checkmate.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;

import java.util.Arrays;


public class CallMicThread extends Thread {
    private static final String TAG = "MicThread";
    private Context mContext; // Add context reference

    public Streamer mStreamer;
    public Streamer mRecorder; // if you want a second streamer for local file or something
    private AudioConfig mConfig;
    private AudioRecord mAudioRecord;
    public float mVolumeBoost = 2.0f; // Increase if needed, e.g. 2.0f
    public boolean isRecording = false;
    public CallMicThread(AudioConfig config) {
        super();
        mConfig = config;
    }
    public CallMicThread(Context context,AudioConfig config) {
        super();
        mConfig = config;
        mContext = context;
    }

    public CallMicThread(Context context, AudioConfig config,boolean misRecording) {
        super();
        mContext = context.getApplicationContext();
        mConfig = config;
        isRecording = misRecording;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        try {
            // Use VOICE_RECOGNITION to capture both sides if phone is on speaker
            final int audioSource = mConfig.audioSource;
            final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            final int sampleRate = mConfig.sampleRate;    // e.g. 44100 or 16000
            final int channelCount = mConfig.channelCount; // e.g. 1 or 2
            final int channelConfig = (channelCount == 1)
                    ? AudioFormat.CHANNEL_IN_MONO
                    : AudioFormat.CHANNEL_IN_STEREO;

            final int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            if (minBufferSize <= 0) {
                throw new Exception("Invalid buffer size: " + minBufferSize);
            }
            final int bufferSizeInBytes = minBufferSize * 4; // padding

            mAudioRecord = new AudioRecord(audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes);

            // Disable AEC using audio session
            int sessionId = mAudioRecord.getAudioSessionId();
            if (isRecording) {
                setAudioEffect(sessionId,false);
            }else {
                setAudioEffect(sessionId,false);
            }

            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new Exception("AudioRecord initialization failed");
            }

            final short[] audioBuffer = new short[minBufferSize / 2];
            mAudioRecord.startRecording();

            while (!isInterrupted()) {
                final int audioInputLength = mAudioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (audioInputLength > 0) {
                    short[] processedAudio = Arrays.copyOf(audioBuffer, audioInputLength);

                    // Optionally apply volume boost
                    if (mVolumeBoost != 1.0f) {
                        processedAudio = applyVolumeBoost(processedAudio, mVolumeBoost);
                    }

                    // Convert to bytes
                    byte[] byteData = shortsToBytes(processedAudio);
                    // 🔥 LIVE LOGGING for Debugging
//                    if (isSilent(processedAudio, audioInputLength)) {
//                        Log.d(TAG, "[Audio Debug] Silence detected...");
//                    } else {
//                        Log.d(TAG, "[Audio Debug] Audio detected! (Voice/Call)");
//                    }
                    // Write to your Streamer (RTMP, etc.)
                    if (mStreamer != null) {
                        mStreamer.writePcmData(byteData);
                    }
                    // If you also have a separate recorder:
                    if (mRecorder != null) {
                        mRecorder.writePcmData(byteData);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in MicThread: " + Log.getStackTraceString(e));
        } finally {
            cleanup();
        }
    }

    private void setAudioEffect(int sessionId, boolean flag) {
        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler aec = AcousticEchoCanceler.create(sessionId);
            if (aec != null) {
                aec.setEnabled(flag);
                Log.d(TAG, "Disabled AEC for recording");
            }
        }

        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl agc = AutomaticGainControl.create(sessionId);
            if (agc != null) {
                agc.setEnabled(flag);
                Log.d(TAG, "Disabled AGC for recording");
            }
        }
    }


    private short[] applyVolumeBoost(short[] samples, float factor) {
        short[] boosted = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            float boostedSample = samples[i] * factor;
            boosted[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, boostedSample));
        }
        return boosted;
    }

    private boolean isSilent(short[] buffer, int length) {
        int threshold = 300; // Tune this if needed
        int silentSamples = 0;
        for (int i = 0; i < length; i++) {
            if (Math.abs(buffer[i]) < threshold) {
                silentSamples++;
            }
        }
        float silentRatio = (float) silentSamples / length;
        return silentRatio > 0.90f; // >90% samples are near zero => silent
    }


//    private short[] applyVolumeBoost(short[] samples, float factor) {
//        short[] boosted = new short[samples.length];
//        for (int i = 0; i < samples.length; i++) {
//            float sample = samples[i] * factor;
//            if (sample > Short.MAX_VALUE) {
//                sample = Short.MAX_VALUE;
//            } else if (sample < Short.MIN_VALUE) {
//                sample = Short.MIN_VALUE;
//            }
//            boosted[i] = (short) sample;
//        }
//        return boosted;
//    }

    private byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];
        for (int i = 0; i < shorts.length; i++) {
            bytes[i * 2] = (byte) (shorts[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((shorts[i] >> 8) & 0xFF);
        }
        return bytes;
    }

    // MicThread.java
    private void cleanup() {
        if (mAudioRecord != null) {
            try {
                mAudioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage());
            } finally {
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }

        // Reset audio streamers
        mStreamer = null;
        mRecorder = null;

        // Reset audio manager state
        if (mContext != null) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
            }
        }
    }
}