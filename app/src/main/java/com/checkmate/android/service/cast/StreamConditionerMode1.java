package com.checkmate.android.service.cast;

import com.wmspanel.libstream.Streamer;

import java.util.Set;

public class StreamConditionerMode1 extends StreamConditionerBase {

    private final long NORMALIZATION_DELAY = 1500; //Ignore lost packets during this time after bitrate change
    private final long LOST_ESTIMATE_INTERVAL = 10000; //Period for lost packets count
    private final long LOST_TOLERANCE = 4;             // Maximum acceptable number of lost packets
    private final long RECOVERY_ATTEMPT_INTERVAL = 60000;
    private int mInitBitrate;
    private int mMinBitate;

    @Override
    protected void check(long audioLost, long videoLost) {
        long curTime = System.currentTimeMillis();
        LossHistory prevLost = mLossHistory.lastElement();
        BitrateHistory prevBitrate = mBitrateHistory.lastElement();
        long lastChange = Math.max(prevBitrate.ts, prevLost.ts);
        if (prevLost.audio != audioLost || prevLost.video != videoLost) {
            //Log.d(TAG, "Lost packets " + audioLost + "/" + videoLost);
            long dtChange = curTime - prevBitrate.ts;
            if (prevBitrate.bitrate <= mMinBitate || dtChange < NORMALIZATION_DELAY) {
                return;
            }
            mLossHistory.add(new LossHistory(curTime, audioLost, videoLost));
            long estimatePeriod = Math.max(prevBitrate.ts + NORMALIZATION_DELAY, curTime - LOST_ESTIMATE_INTERVAL);
            if (countLostForInterval(estimatePeriod) >= LOST_TOLERANCE) {
                long newBitrate = Math.max(mMinBitate, prevBitrate.bitrate * 1000 / 1414);
                changeBitrate(newBitrate);
            }
        } else if (prevBitrate.bitrate != mBitrateHistory.firstElement().bitrate &&
                curTime - lastChange >= RECOVERY_ATTEMPT_INTERVAL) {
            long newBitrate = Math.min(mInitBitrate, prevBitrate.bitrate * 1415 / 1000);
            changeBitrate(newBitrate);
        }
    }

    @Override
    public void start(Streamer streamer, int bitrate, Set<Integer> connections) {
        mInitBitrate = bitrate;
        mMinBitate = bitrate / 4;
        super.start(streamer, bitrate, connections);
    }

}
