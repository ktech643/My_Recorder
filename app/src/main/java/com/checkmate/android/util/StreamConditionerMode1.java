package com.checkmate.android.util;

import android.content.Context;

import com.wmspanel.libstream.Streamer;

import java.util.Set;

class StreamConditionerMode1 extends StreamConditionerBase {

    private final long NORMALIZATION_DELAY = 1500; //Ignore lost packets during this time after bitrate change
    private final long LOST_ESTIMATE_INTERVAL = 10000; //Period for lost packets count
    private final long LOST_TOLERANCE = 4;             // Maximum acceptable number of lost packets
    private final long RECOVERY_ATTEMPT_INTERVAL = 60000;
    private int mMinBitate;

    StreamConditionerMode1(Context context) {
        super(context);
    }

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
                if (TEST_MODE && newBitrate == mMinBitate) {
                    mSimulateLoss = false;
                }
            }
        } else if (prevBitrate.bitrate != mBitrateHistory.firstElement().bitrate &&
                curTime - lastChange >= RECOVERY_ATTEMPT_INTERVAL) {
            long newBitrate = Math.min(mFullBitrate, prevBitrate.bitrate * 1415 / 1000);
            if (TEST_MODE && newBitrate == mFullBitrate) {
                mSimulateLoss = true;
            }
            changeBitrate(newBitrate);
        }
    }

    @Override
    public void start(Streamer streamer, int bitrate, Set<Integer> connections) {
        mFullBitrate = bitrate;
        mMinBitate = bitrate / 4;

        super.start(streamer, bitrate, connections);
        if (TEST_MODE) {
            mSimulateLoss = true;
        }
    }

}
