package com.checkmate.android.util;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class AudioUtils {
    private static final String TAG = "AudioUtils";

    /**
     * Build an AudioRecord that prefers a USB mic if plugged in,
     * otherwise falls back to the default phone mic.  It will
     * choose a supported sample rate & channel mask automatically.
     *
     * @param ctx your ApplicationContext
     * @return a fully‑initialized AudioRecord
     */
    public static AudioRecord createUsbOrPhoneMic(Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);

        // 1) Look for any USB input device
        AudioDeviceInfo usbMic = null;
        for (AudioDeviceInfo dev : devices) {
            int t = dev.getType();
            if (t == AudioDeviceInfo.TYPE_USB_DEVICE
                    || t == AudioDeviceInfo.TYPE_USB_HEADSET
                    || t == AudioDeviceInfo.TYPE_USB_ACCESSORY) {
                usbMic = dev;
                break;
            }
        }

        // 2) Choose a rate & channel mask that the USB mic supports (or defaults)
        int chosenRate = 44100;
        int chosenMask = AudioFormat.CHANNEL_IN_MONO;
        if (usbMic != null) {
            int[] rates    = usbMic.getSampleRates();
            int[] counts   = usbMic.getChannelCounts();
            int[] encs     = usbMic.getEncodings();
            Log.i(TAG, "USB mic found: " + usbMic.getProductName());
            Log.i(TAG, " • rates:   " + Arrays.toString(rates));
            Log.i(TAG, " • channels:" + Arrays.toString(counts));
            Log.i(TAG, " • encodings:" + Arrays.toString(encs));

            // prefer 48k → 44.1k → first
            if (contains(rates, 48000))      chosenRate = 48000;
            else if (contains(rates, 44100)) chosenRate = 44100;
            else if (rates.length > 0)       chosenRate = rates[0];

            // stereo if supported
            if (contains(counts, 2)) chosenMask = AudioFormat.CHANNEL_IN_STEREO;
        } else {
            Log.i(TAG, "No USB mic, using phone mic");
        }

        // 3) Use 16‑bit PCM (universally supported)
        int format = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioRecord.getMinBufferSize(chosenRate, chosenMask, format);
        if (minBuf <= 0) {
            throw new IllegalArgumentException(
                    "Bad audio settings: " + chosenRate + "Hz, mask=" + chosenMask);
        }
        int bufBytes = minBuf * 2;  // double for safety

        // 4) Build the AudioRecord
        AudioRecord.Builder b = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(format)
                        .setSampleRate(chosenRate)
                        .setChannelMask(chosenMask)
                        .build())
                .setBufferSizeInBytes(bufBytes);

        AudioRecord ar = b.build();
        if (ar.getState() != AudioRecord.STATE_INITIALIZED) {
            ar.release();
            throw new RuntimeException("AudioRecord init failed");
        }

        // 5) Route to USB mic if available
        if (usbMic != null) {
            boolean ok = ar.setPreferredDevice(usbMic);
            Log.i(TAG, "Preferred USB mic? " + ok);
        }

        Log.i(TAG, String.format(
                "Mic @ %dHz, mask=%d, buf=%d", chosenRate, chosenMask, bufBytes));
        return ar;
    }

    private static boolean contains(int[] arr, int x) {
        if (arr == null) return false;
        for (int v : arr) if (v == x) return true;
        return false;
    }
}
