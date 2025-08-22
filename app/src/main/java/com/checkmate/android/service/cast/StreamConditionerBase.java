package com.checkmate.android.service.cast;

import android.os.Handler;

import com.wmspanel.libstream.Streamer;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

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

    Runnable networkCheckRunnable;
    Handler handler;
    private int mCurrentBitrate;
    protected Vector<LossHistory> mLossHistory;
    protected Vector<BitrateHistory> mBitrateHistory;

    private Streamer mStreamer;
    private Set<Integer> mConnectionId;

    StreamConditionerBase() {
        mConnectionId = new HashSet<>();
        mLossHistory = new Vector<>();
        mBitrateHistory = new Vector<>();
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
        if (checkDelay() > 0 && checkInterval() > 0) {
          /*  mCheckNetwork = new TimerTask() {
                @Override
                public void run() {
                    if (mStreamer == null || mConnectionId.size() == 0) {
                        return;
                    }
                    long audioLost = 0;
                    long videoLost = 0;
                    for (int id : mConnectionId) {
                        audioLost += mStreamer.getAudioPacketsLost(id);
                        videoLost += mStreamer.getVideoPacketsLost(id);
                        videoLost += mStreamer.getUdpPacketsLost(id);
                    }
                    check(audioLost, videoLost);
                }
            };
            mCheckTimer = new Timer();
            mCheckTimer.schedule(mCheckNetwork, checkDelay(), checkInterval());*/


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

                    check(audioLost, videoLost);

                    // Re-run the task after the specified interval (e.g., checkInterval())
                    handler.postDelayed(this, checkInterval());
                }
            };
            handler.postDelayed(networkCheckRunnable, checkDelay());
        }
    }

    public void stop() {
        mStreamer = null;
        mConnectionId.clear();
        if (handler!=null) {
            handler.removeCallbacks(networkCheckRunnable);
        }
    }

    public void addConnection(int connecitonId) {
        mConnectionId.add(connecitonId);
    }

    public void removeConnection(int connecitonId) {
        mConnectionId.remove(connecitonId);
    }

    public int getBitrate() {
        return mCurrentBitrate;
    }

    protected void check(long audioLost, long videoLost) {

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

    public void changeBitrate(long newBitrate) {
        long curTime = System.currentTimeMillis();
        mBitrateHistory.add(new BitrateHistory(curTime, newBitrate));
        mStreamer.changeBitRate((int) newBitrate);
        mCurrentBitrate = (int) newBitrate;
        //Log.d(TAG, "Changing bitrate to " + newBitrate);
    }

    public void changeBitrateQuiet(long newBitrate) {
        mStreamer.changeBitRate((int) newBitrate);
    }

    protected long checkInterval() {
        return 500;
    }

    protected long checkDelay() {
        return 1000;
    }

}
