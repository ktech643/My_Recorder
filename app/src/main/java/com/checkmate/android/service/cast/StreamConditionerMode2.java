package com.checkmate.android.service.cast;

import com.wmspanel.libstream.Streamer;

import java.util.Set;

public class StreamConditionerMode2 extends StreamConditionerBase {

    private long NORMALIZATION_DELAY = 2000; //Ignore lost packets during this time after bitrate change
    private long LOST_ESTIMATE_INTERVAL = 10000; //Period for lost packets count
    private long LOST_BANDWITH_TOLERANCE_FRAC = 300000;
    private final double[] BANDWITH_STEPS = {0.2, 0.25, 1.0 / 3.0, 0.450, 0.600, 0.780, 1.000};
    private long[] RECOVERY_ATTEMPT_INTERVALS = {15000, 60000, 60000 * 3};
    private long DROP_MERGE_INTERVAL = BANDWITH_STEPS.length * NORMALIZATION_DELAY * 2; //Period for bitrate drop duration

    private int mFullSpeed;
    private int mStep;

    @Override
    public void start(Streamer streamer, int bitrate, Set<Integer> connections) {
        mFullSpeed = bitrate;
        mStep = 2;
        int startBitrate = (int) Math.round(mFullSpeed * BANDWITH_STEPS[mStep]);
        super.start(streamer, startBitrate, connections);
        changeBitrateQuiet(startBitrate);
    }

    @Override
    protected void check(long audioLost, long videoLost) {
        long curTime = System.currentTimeMillis();
        LossHistory prevLost = mLossHistory.lastElement();
        BitrateHistory prevBitrate = mBitrateHistory.lastElement();
        if (prevLost.audio != audioLost || prevLost.video != videoLost) {
            //Log.d(TAG, "Lost packets " + audioLost + "+" + videoLost);
            long dtChange = curTime - prevBitrate.ts;
            if (mStep == 0 || dtChange < NORMALIZATION_DELAY) {
                return;
            }
            mLossHistory.add(new LossHistory(curTime, audioLost, videoLost));
            long estimatePeriod = Math.max(prevBitrate.ts + NORMALIZATION_DELAY, curTime - LOST_ESTIMATE_INTERVAL);
            long lost_tolerance = prevBitrate.bitrate / LOST_BANDWITH_TOLERANCE_FRAC;
            if (countLostForInterval(estimatePeriod) >= lost_tolerance) {
                long newBitrate = Math.round(mFullSpeed * BANDWITH_STEPS[--mStep]);
                changeBitrate(newBitrate);
            }
        } else if (prevBitrate.bitrate < mFullSpeed && canTryToRecover()) {
            //Log.d(TAG, "Increasing bitrate");
            long newBitrate = Math.round(mFullSpeed * BANDWITH_STEPS[++mStep]);
            changeBitrate(newBitrate);
        }
    }

    private boolean canTryToRecover() {
        long curTime = System.currentTimeMillis();
        int len = mBitrateHistory.size();
        int numDrops = 0;
        int numIntervals = RECOVERY_ATTEMPT_INTERVALS.length;
        long prevDropTime = 0;
        for (int i = len - 1; i > 0; i--) {
            BitrateHistory last = mBitrateHistory.elementAt(i);
            BitrateHistory prev = mBitrateHistory.elementAt(i - 1);
            long dt = curTime - last.ts;
            if (last.bitrate < prev.bitrate) {
                if (prevDropTime != 0 && prevDropTime - last.ts < DROP_MERGE_INTERVAL) {
                    continue;
                }
                if (dt <= RECOVERY_ATTEMPT_INTERVALS[numDrops]) {
                    return false;
                }
                numDrops++;
                prevDropTime = last.ts;
            }

            if (numDrops == numIntervals || curTime - last.ts >= RECOVERY_ATTEMPT_INTERVALS[numIntervals - 1]) {
                break;
            }
        }

        return true;
    }

    protected long checkInterval() {
        return 2000;
    }

    protected long checkDelay() {
        return 2000;
    }

}
