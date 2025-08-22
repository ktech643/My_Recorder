package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.wmspanel.libstream.Streamer;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * StreamConditionerBase sets and maintains the stream bitrate and frame rate.
 * This version strictly uses the bitrate value passed to it, without internal calculation or enforcement.
 */
public abstract class StreamConditionerBase {

    protected final String TAG = "StreamConditioner";

    final class LossHistory {
        long ts;
        long audio;
        long video;

        LossHistory(long _ts, long _audio, long _video) {
            ts = _ts;
            audio = _audio;
            video = _video;
        }
    }

    final class BitrateHistory {
        long ts;
        long bitrate;

        BitrateHistory(long _ts, long _bitrate) {
            ts = _ts;
            bitrate = _bitrate;
        }
    }

    private int mCurrentBitrate;
    protected int mFullBitrate;
    protected Vector<LossHistory> mLossHistory;
    protected Vector<BitrateHistory> mBitrateHistory;
    protected double mCurrentFps;
    protected Streamer.FpsRange mCurrentRange;
    protected Context mContext;

    protected Streamer.FpsRange[] mFpsRanges;
    protected double mMaxFps;

    private Streamer mStreamer;
    private Set<Integer> mConnectionId;
    Runnable networkCheckRunnable;
    Handler handler;
    protected final boolean TEST_MODE = false;
    protected boolean mSimulateLoss = false; // Used by test mode to simulate packet loss

    StreamConditionerBase(Context context) {
        mConnectionId = new HashSet<>();
        mLossHistory = new Vector<>();
        mBitrateHistory = new Vector<>();
        mContext = context;
        mFullBitrate = 0;
        mCurrentBitrate = 0;
        mFpsRanges = new Streamer.FpsRange[0];

        mMaxFps = AppPreference.getInt(AppPreference.KEY.STREAMING_FRAME, 30);
        mCurrentRange = new Streamer.FpsRange((int) mMaxFps, (int) mMaxFps);
    }

    public void setFpsRanges(Streamer.FpsRange[] fpsRanges) {
        mFpsRanges = fpsRanges;
    }

    public void start(Streamer streamer, int bitrate, Set<Integer> connections) {
        mStreamer = streamer;
        mConnectionId = new HashSet<>(connections);
        mLossHistory.clear();
        mBitrateHistory.clear();
        long curTime = System.currentTimeMillis();
        mLossHistory.add(new LossHistory(curTime, 0, 0));
        mBitrateHistory.add(new BitrateHistory(curTime, bitrate));
        mCurrentBitrate = bitrate;
        mFullBitrate = bitrate; // Use exactly what is passed
        Streamer.FpsRange fpsRange = SettingsUtils.streamerfpsRange(mContext);
        if (fpsRange != null) {
            mMaxFps = fpsRange.fpsMax * 1.0;
        }
        mCurrentFps = mMaxFps;
        runTask();
        Log.d(TAG, "Stream started. Full bitrate set to: " + mFullBitrate);
        mStreamer.changeBitRate(bitrate); // Set exactly what is passed
    }

    public void changeBitrate(long newBitrate) {
        Log.d(TAG, "Requested bitrate change: " + newBitrate); // Log requested value
        long curTime = System.currentTimeMillis();
        mBitrateHistory.add(new BitrateHistory(curTime, newBitrate));
        if (SettingsUtils.adaptiveFps(mContext)) {
            updateFps(newBitrate);
        }
        mStreamer.changeBitRate((int) newBitrate); // Use exactly what is passed
        mCurrentBitrate = (int) newBitrate;
        Log.d(TAG, "Bitrate changed to: " + newBitrate); // Confirm actual change
    }

    public void stop() {
        if (mFullBitrate > 0) {
            updateFps(mFullBitrate);
        }
        mCurrentBitrate = 0;
        mStreamer = null;
        mConnectionId.clear();

        if (handler != null) {
            handler.removeCallbacks(networkCheckRunnable);
        }
    }

    void pause() {
        // Optional: implement if needed
    }

    private void runTask() {
        if (checkDelay() == 0 || checkInterval() == 0) {
            return;
        }
        handler = new Handler();
        networkCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (mStreamer == null || mConnectionId.size() == 0) {
                    return;
                }

                long audioLost = 0;
                long videoLost = 0;

                for (int id : mConnectionId) {
                    try {
                        audioLost += mStreamer.getAudioPacketsLost(id);
                        videoLost += mStreamer.getVideoPacketsLost(id);
                        videoLost += mStreamer.getUdpPacketsLost(id);
                    } catch (IllegalStateException e) {
                        // Handle or log the exception as needed
                        return;
                    }
                }

                if (TEST_MODE) {
                    LossHistory prevLost = mLossHistory.lastElement();
                    if (mSimulateLoss) {
                        audioLost = prevLost.audio + 3;
                        videoLost = prevLost.video + 3;
                    } else {
                        audioLost = prevLost.audio;
                        videoLost = prevLost.video;
                    }
                }

                check(audioLost, videoLost);

                // Re-run the task after the specified interval
                handler.postDelayed(this, checkInterval());
            }
        };
        handler.postDelayed(networkCheckRunnable, checkDelay());
    }

    public void resume() {
        if (mCurrentBitrate == 0) {
            return;
        }
        mCurrentBitrate = mFullBitrate;
        Streamer.FpsRange fpsRange = SettingsUtils.fpsRange(mContext);
        if (fpsRange != null) {
            mMaxFps = fpsRange.fpsMax * 1.0;
            mCurrentRange = fpsRange;
        } else {
            mMaxFps = 30.0;
            mCurrentRange = new Streamer.FpsRange(30, 30);
        }
        mCurrentFps = mMaxFps;
        runTask();
        mStreamer.changeBitRate(mFullBitrate);
    }

    public void addConnection(int connectionId) {
        mConnectionId.add(connectionId);
    }

    public void removeConnection(int connectionId) {
        mConnectionId.remove(connectionId);
    }

    public int getBitrate() {
        return mCurrentBitrate;
    }

    protected void check(long audioLost, long videoLost) {
        // Implement in subclass if needed
    }

    protected long countLostForInterval(long interval) {
        long lostPackets = 0;
        LossHistory last = mLossHistory.lastElement();
        for (int i = mLossHistory.size() - 1; i >= 0; i--) {
            if (mLossHistory.elementAt(i).ts < interval) {
                LossHistory h = mLossHistory.elementAt(i);
                lostPackets = (last.video - h.video) + (last.audio - h.audio);
                break;
            }
        }
        return lostPackets;
    }

    protected void changeBitrateQuiet(long newBitrate) {
        mStreamer.changeBitRate((int) newBitrate);
    }

    protected long checkInterval() {
        return 500;
    }

    protected long checkDelay() {
        return 1000;
    }

    public static StreamConditionerBase newInstance(Context context) {
        StreamConditionerBase conditioner = null;
        switch (SettingsUtils.adaptiveBitrate(context)) {
            case SettingsUtils.ADAPTIVE_BITRATE_MODE1:
                conditioner = new StreamConditionerMode1(context);
                break;
            case SettingsUtils.ADAPTIVE_BITRATE_MODE2:
                conditioner = new StreamConditionerMode2(context);
                break;
            case SettingsUtils.ADAPTIVE_BITRATE_OFF:
            default:
                break;
        }
        return conditioner;
    }

    protected void updateFps(long newBitrate) {
        if (mFpsRanges == null || mFpsRanges.length == 0) {
            return;
        }
        double bitrateRel = newBitrate * 1.0 / mFullBitrate;
        double relFps = mMaxFps;
        if (bitrateRel < 0.5) {
            relFps = Math.max(15.0, Math.floor(mMaxFps * bitrateRel * 2.0 / 5.0) * 5.0);
        }
        if (Math.abs(relFps - mCurrentFps) < 1.0) {
            return;
        }
        mCurrentFps = relFps;
        Streamer.FpsRange newRange = SettingsUtils.nearestFpsRange(mFpsRanges, Math.round(relFps), false);
        if (newRange.fpsMax == mCurrentRange.fpsMax && newRange.fpsMin == mCurrentRange.fpsMin) {
            return;
        }
        Log.d(TAG, "Changing FPS range to " + newRange.fpsMin + "..." + newRange.fpsMax);
        mStreamer.changeFpsRange(newRange);
        mCurrentRange = newRange;
    }
}