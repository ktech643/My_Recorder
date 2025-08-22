package com.checkmate.android.util;

import com.wmspanel.libstream.Streamer;

public final class ConnectionStatistics {
    private long startTime;
    private long prevTime;
    private long prevBytes;
    private long duration;
    private long bps;
    private long videoPacketsLost;
    private long audioPacketsLost;
    public long udpPacketsLost;
    private boolean packetLossIncreased;

    public long getBandwidth() {
        return bps;
    }

    public long getDuration() {
        return duration;
    }

    public long getTraffic() {
        return prevBytes;
    }

    public boolean isPacketLossIncreasing() {
        return packetLossIncreased;
    }

    public void init(Streamer streamer, int connectionId) {
        if (streamer == null) {
            return;
        }

        long time = System.currentTimeMillis();
        startTime = time;
        prevTime = time;
        prevBytes = streamer.getBytesSent(connectionId);
    }

    public void update(Streamer streamer, int connectionId) {
        if (streamer == null) {
            return;
        }

        final long curTime = System.currentTimeMillis();
        final long bytesSent = streamer.getBytesSent(connectionId);
        final long timeDiff = curTime - prevTime;
        if (timeDiff > 0) {
            bps = 8 * 1000 * (bytesSent - prevBytes) / timeDiff;
        } else {
            bps = 0;
        }
        prevTime = curTime;
        prevBytes = bytesSent;
        duration = (curTime - startTime) / 1000L;

        packetLossIncreased = false;
        if (audioPacketsLost != streamer.getAudioPacketsLost(connectionId) ||
                videoPacketsLost != streamer.getVideoPacketsLost(connectionId) ||
                udpPacketsLost != streamer.getUdpPacketsLost(connectionId)) {
            audioPacketsLost = streamer.getAudioPacketsLost(connectionId);
            videoPacketsLost = streamer.getVideoPacketsLost(connectionId);
            udpPacketsLost = streamer.getUdpPacketsLost(connectionId);
            packetLossIncreased = true;
        }
    }

    public float getPacketLossRate(Streamer streamer, int connectionId) {
        if (streamer == null) return 0f;

        long audioSent = streamer.getAudioPacketsSent(connectionId);
        long videoSent = streamer.getVideoPacketsSent(connectionId);
        long udpSent = streamer.getUdpPacketsSent(connectionId);

        long totalSent = audioSent + videoSent + udpSent;
        long totalLost = audioPacketsLost + videoPacketsLost + udpPacketsLost;

        if (totalSent == 0) return 0f;

        return (float) totalLost / totalSent;
    }

}