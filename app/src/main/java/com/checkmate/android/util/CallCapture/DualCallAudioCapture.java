package com.checkmate.android.util.CallCapture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.Streamer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class DualCallAudioCapture {
    private static final String TAG = "DualCallAudioCapture";

    private final MediaProjection mProj;
    private final Streamer      mStreamer;
    private final Streamer      mRecorder;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    // Audio constants
    private static final int SR      = 16_000;
    private static final int CH_MONO = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCOD   = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mDownRec, mUpRec;
    private Thread      mMixThread;
    Context mcontect;
    public DualCallAudioCapture(Context context,MediaProjection projection,
                                Streamer streamer,
                                Streamer recorder) {
        mProj     = projection;
        mStreamer = streamer;
        mRecorder = recorder;
        mcontect = context;
    }

    public void start() {
        if (!mRunning.compareAndSet(false, true)) return;

        final int minBuf = AudioRecord.getMinBufferSize(SR, CH_MONO, ENCOD) * 2;
        if (minBuf < 0) {
            Log.e(TAG, "Unable to get a valid AudioRecord buffer size: " + minBuf);
            return;
        }

        // checkSelfPermission will tell you if the OS considers CAPTURE_AUDIO_OUTPUT granted
        int capOut = ActivityCompat.checkSelfPermission(mcontect, "android.permission.CAPTURE_AUDIO_OUTPUT");
        Log.d(TAG, "CAPTURE_AUDIO_OUTPUT = " + (capOut == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));

        // 1) Downlink via PlaybackCapture
        AudioPlaybackCaptureConfiguration cfg =
                new AudioPlaybackCaptureConfiguration.Builder(mProj)
                        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .build();

        if (ActivityCompat.checkSelfPermission(mcontect, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

        }
        mDownRec = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(ENCOD)
                        .setSampleRate(SR)
                        .setChannelMask(CH_MONO)
                        .build())
                .setBufferSizeInBytes(minBuf)
                .setAudioPlaybackCaptureConfig(cfg)
                .build();

        // 2) Uplink via mic
        mUpRec = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(ENCOD)
                        .setSampleRate(SR)
                        .setChannelMask(CH_MONO)
                        .build())
                .setBufferSizeInBytes(minBuf)
                .build();

        if (mDownRec.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "downlink AudioRecord failed to initialize: " + mDownRec.getState());
            mRunning.set(false);
            start();
            return;
        }
        if (mUpRec.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "uplink AudioRecord failed to initialize: " + mUpRec.getState());
            mRunning.set(false);
            start();
            return;
        }
        mDownRec.startRecording();
        mUpRec.startRecording();

        // 3) Mix thread
        mMixThread = new Thread(this::mixLoop, "DualCallMix");
        mMixThread.start();
    }

    public void stop() {
        if (!mRunning.compareAndSet(true, false)) return;

        if (mDownRec != null) { mDownRec.stop(); mDownRec.release(); }
        if (mUpRec   != null) { mUpRec.stop();   mUpRec.release();   }
        if (mMixThread != null) {
            mMixThread.interrupt();
        }

    }

    private void mixLoop() {
        try {
            AudioManager audioManager = (AudioManager) mcontect.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            // Initialize accumulators for DL and UL data
            ByteArrayOutputStream dlAccumulator = new ByteArrayOutputStream();
            ByteArrayOutputStream ulAccumulator = new ByteArrayOutputStream();
            byte[] tempBufDown = new byte[mDownRec.getBufferSizeInFrames()];
            byte[] tempBufUp = new byte[mUpRec  .getBufferSizeInFrames()];

            while (mRunning.get() && !Thread.interrupted()) {
                // Read DL data
                int rD = mDownRec.read(tempBufDown, 0, tempBufDown.length);
                if (rD > 0) {
                    dlAccumulator.write(tempBufDown, 0, rD);
                } else if (rD < 0) {
                    Log.e(TAG, "DL read error: " + rD);
                    break;
                }
                // Read UL data
                int rU = mUpRec.read(tempBufUp, 0, tempBufUp.length);
                if (rU > 0) {
                    ulAccumulator.write(tempBufUp, 0, rU);
                } else if (rU < 0) {
                    Log.e(TAG, "UL read error: " + rU);
                    break;
                }
                // Process accumulated data
                byte[] dlBytes = dlAccumulator.toByteArray();
                byte[] ulBytes = ulAccumulator.toByteArray();

                int dlSamples = dlBytes.length / 2;
                int ulSamples = ulBytes.length / 2;
                int framesToProcess = Math.min(dlSamples, ulSamples);

                if (framesToProcess > 0) {
                    ByteBuffer srcD = ByteBuffer.wrap(dlBytes).order(ByteOrder.LITTLE_ENDIAN);
                    ByteBuffer srcU = ByteBuffer.wrap(ulBytes).order(ByteOrder.LITTLE_ENDIAN);
                    byte[] stereoData = new byte[framesToProcess * 4]; // 4 bytes per stereo frame
                    ByteBuffer dst = ByteBuffer.wrap(stereoData).order(ByteOrder.LITTLE_ENDIAN);

                    for (int i = 0; i < framesToProcess; i++) {
                        dst.putShort(srcD.getShort());
                        dst.putShort(srcU.getShort());
                    }

                    // Write to streamer and file
                    mStreamer.writePcmData(stereoData);
                    mRecorder.writePcmData(stereoData);

                    // Update accumulators with remaining data
                    dlAccumulator.reset();
                    dlAccumulator.write(Arrays.copyOfRange(dlBytes, framesToProcess * 2, dlBytes.length));
                    ulAccumulator.reset();
                    ulAccumulator.write(Arrays.copyOfRange(ulBytes, framesToProcess * 2, ulBytes.length));
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "mixLoop error", e);
        } finally {
            stop();
        }
    }
}