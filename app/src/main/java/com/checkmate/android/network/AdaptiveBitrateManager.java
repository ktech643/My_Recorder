package com.checkmate.android.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.util.CrashLogger;
import com.checkmate.android.util.DynamicSettingsManager;
import com.checkmate.android.util.UserFeedbackManager;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Advanced network quality monitoring and adaptive bitrate management
 * Automatically adjusts streaming quality based on real-time network conditions
 */
public class AdaptiveBitrateManager {
    private static final String TAG = "AdaptiveBitrateManager";
    private static AdaptiveBitrateManager instance;
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Network quality tracking
    private final Queue<NetworkSample> networkSamples = new LinkedList<>();
    private static final int SAMPLE_WINDOW_SIZE = 30; // 30 samples
    private static final long SAMPLE_INTERVAL_MS = 1000; // 1 second
    
    // Bitrate adaptation parameters
    private static final double QUALITY_THRESHOLD_EXCELLENT = 0.9;
    private static final double QUALITY_THRESHOLD_GOOD = 0.7;
    private static final double QUALITY_THRESHOLD_FAIR = 0.5;
    private static final double QUALITY_THRESHOLD_POOR = 0.3;
    
    // Current state
    private final AtomicReference<NetworkQuality> currentQuality = new AtomicReference<>(NetworkQuality.UNKNOWN);
    private final AtomicLong lastBitrateAdjustment = new AtomicLong(0);
    private static final long ADJUSTMENT_COOLDOWN_MS = 5000; // 5 seconds between adjustments
    
    // Bitrate ladder (in bps)
    private static final int[] BITRATE_LADDER = {
        200_000,    // 200 kbps - Very low
        500_000,    // 500 kbps - Low
        1_000_000,  // 1 Mbps - Fair
        2_000_000,  // 2 Mbps - Good
        4_000_000,  // 4 Mbps - Very good
        8_000_000,  // 8 Mbps - Excellent
        12_000_000  // 12 Mbps - Ultra
    };
    
    public enum NetworkQuality {
        EXCELLENT("Excellent", 1.0),
        GOOD("Good", 0.8),
        FAIR("Fair", 0.6),
        POOR("Poor", 0.4),
        VERY_POOR("Very Poor", 0.2),
        UNKNOWN("Unknown", 0.5);
        
        public final String label;
        public final double score;
        
        NetworkQuality(String label, double score) {
            this.label = label;
            this.score = score;
        }
    }
    
    private static class NetworkSample {
        final long timestamp;
        final long bandwidth; // in kbps
        final int latency;    // in ms
        final double packetLoss; // percentage
        final String networkType;
        
        NetworkSample(long bandwidth, int latency, double packetLoss, String networkType) {
            this.timestamp = System.currentTimeMillis();
            this.bandwidth = bandwidth;
            this.latency = latency;
            this.packetLoss = packetLoss;
            this.networkType = networkType;
        }
        
        double getQualityScore() {
            // Calculate quality score based on multiple factors
            double bandwidthScore = Math.min(bandwidth / 10000.0, 1.0); // Normalize to 10 Mbps
            double latencyScore = Math.max(1.0 - (latency / 200.0), 0.0); // 200ms = 0 score
            double lossScore = Math.max(1.0 - (packetLoss / 5.0), 0.0); // 5% loss = 0 score
            
            // Weighted average
            return bandwidthScore * 0.5 + latencyScore * 0.3 + lossScore * 0.2;
        }
    }
    
    private AdaptiveBitrateManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initializeNetworkMonitoring();
    }
    
    public static synchronized AdaptiveBitrateManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdaptiveBitrateManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize network monitoring
     */
    private void initializeNetworkMonitoring() {
        // Register network callback
        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();
            
        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                onNetworkCapabilitiesChanged(networkCapabilities);
            }
            
            @Override
            public void onLost(Network network) {
                super.onLost(network);
                onNetworkLost();
            }
        });
        
        // Start periodic quality monitoring
        startQualityMonitoring();
        
        Log.i(TAG, "Adaptive bitrate manager initialized");
    }
    
    /**
     * Handle network capabilities change
     */
    private void onNetworkCapabilitiesChanged(NetworkCapabilities capabilities) {
        try {
            // Extract network metrics
            int linkDownBandwidth = capabilities.getLinkDownstreamBandwidthKbps();
            int linkUpBandwidth = capabilities.getLinkUpstreamBandwidthKbps();
            
            // Estimate latency (simplified - in real implementation, use actual ping)
            int estimatedLatency = estimateLatency(capabilities);
            
            // Create sample
            NetworkSample sample = new NetworkSample(
                linkDownBandwidth,
                estimatedLatency,
                0.0, // Packet loss would need actual measurement
                getNetworkTypeName(capabilities)
            );
            
            // Add to samples
            synchronized (networkSamples) {
                networkSamples.offer(sample);
                while (networkSamples.size() > SAMPLE_WINDOW_SIZE) {
                    networkSamples.poll();
                }
            }
            
            // Update quality assessment
            updateNetworkQuality();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing network capabilities", e);
        }
    }
    
    /**
     * Handle network loss
     */
    private void onNetworkLost() {
        currentQuality.set(NetworkQuality.UNKNOWN);
        
        // Switch to lowest quality immediately
        int lowestBitrate = BITRATE_LADDER[0];
        applyBitrateChange(lowestBitrate, "Network lost - switching to lowest quality");
    }
    
    /**
     * Start periodic quality monitoring
     */
    private void startQualityMonitoring() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                measureNetworkQuality();
                mainHandler.postDelayed(this, SAMPLE_INTERVAL_MS);
            }
        }, SAMPLE_INTERVAL_MS);
    }
    
    /**
     * Measure current network quality
     */
    private void measureNetworkQuality() {
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    onNetworkCapabilitiesChanged(capabilities);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error measuring network quality", e);
        }
    }
    
    /**
     * Update network quality assessment
     */
    private void updateNetworkQuality() {
        synchronized (networkSamples) {
            if (networkSamples.isEmpty()) return;
            
            // Calculate average quality score
            double totalScore = 0;
            int count = 0;
            
            for (NetworkSample sample : networkSamples) {
                // Only consider recent samples (last 30 seconds)
                if (System.currentTimeMillis() - sample.timestamp < 30000) {
                    totalScore += sample.getQualityScore();
                    count++;
                }
            }
            
            if (count == 0) return;
            
            double averageScore = totalScore / count;
            
            // Determine quality level
            NetworkQuality newQuality;
            if (averageScore >= QUALITY_THRESHOLD_EXCELLENT) {
                newQuality = NetworkQuality.EXCELLENT;
            } else if (averageScore >= QUALITY_THRESHOLD_GOOD) {
                newQuality = NetworkQuality.GOOD;
            } else if (averageScore >= QUALITY_THRESHOLD_FAIR) {
                newQuality = NetworkQuality.FAIR;
            } else if (averageScore >= QUALITY_THRESHOLD_POOR) {
                newQuality = NetworkQuality.POOR;
            } else {
                newQuality = NetworkQuality.VERY_POOR;
            }
            
            // Check if quality changed significantly
            NetworkQuality oldQuality = currentQuality.get();
            if (oldQuality != newQuality) {
                currentQuality.set(newQuality);
                onQualityChanged(oldQuality, newQuality);
            }
            
            // Adaptive bitrate adjustment
            adaptBitrateToQuality(newQuality, averageScore);
        }
    }
    
    /**
     * Handle quality change
     */
    private void onQualityChanged(NetworkQuality oldQuality, NetworkQuality newQuality) {
        Log.i(TAG, "Network quality changed: " + oldQuality.label + " -> " + newQuality.label);
        
        // Notify user
        UserFeedbackManager.getInstance(context).showInfo(
            "Network quality: " + newQuality.label
        );
        
        // Log event for AI learning
        if (CrashLogger.getInstance() != null) {
            CrashLogger.getInstance().logInfo(TAG, 
                "Network quality changed to " + newQuality.label);
        }
    }
    
    /**
     * Adapt bitrate based on network quality
     */
    private void adaptBitrateToQuality(NetworkQuality quality, double qualityScore) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastBitrateAdjustment.get() < ADJUSTMENT_COOLDOWN_MS) {
            return;
        }
        
        // Get current bitrate
        int currentBitrate = AppPreference.getInt(AppPreference.KEY.VIDEO_BITRATE, BITRATE_LADDER[3]);
        
        // Calculate target bitrate based on quality
        int targetBitrate = calculateTargetBitrate(quality, qualityScore, currentBitrate);
        
        // Apply if different
        if (targetBitrate != currentBitrate) {
            applyBitrateChange(targetBitrate, "Network adaptation");
            lastBitrateAdjustment.set(now);
        }
    }
    
    /**
     * Calculate target bitrate
     */
    private int calculateTargetBitrate(NetworkQuality quality, double qualityScore, int currentBitrate) {
        // Find current position in ladder
        int currentIndex = 0;
        for (int i = 0; i < BITRATE_LADDER.length; i++) {
            if (BITRATE_LADDER[i] >= currentBitrate) {
                currentIndex = i;
                break;
            }
        }
        
        // Calculate target index based on quality
        int targetIndex;
        switch (quality) {
            case EXCELLENT:
                targetIndex = BITRATE_LADDER.length - 1; // Highest
                break;
            case GOOD:
                targetIndex = BITRATE_LADDER.length - 2; // Second highest
                break;
            case FAIR:
                targetIndex = BITRATE_LADDER.length / 2; // Middle
                break;
            case POOR:
                targetIndex = 2; // Low
                break;
            case VERY_POOR:
                targetIndex = 0; // Lowest
                break;
            default:
                targetIndex = currentIndex; // No change
        }
        
        // Smooth transition - don't jump more than 2 steps at once
        if (targetIndex > currentIndex + 2) {
            targetIndex = currentIndex + 2;
        } else if (targetIndex < currentIndex - 2) {
            targetIndex = currentIndex - 2;
        }
        
        // Ensure within bounds
        targetIndex = Math.max(0, Math.min(targetIndex, BITRATE_LADDER.length - 1));
        
        return BITRATE_LADDER[targetIndex];
    }
    
    /**
     * Apply bitrate change
     */
    private void applyBitrateChange(int newBitrate, String reason) {
        Log.i(TAG, "Applying bitrate change to " + newBitrate + " bps. Reason: " + reason);
        
        // Apply through dynamic settings manager
        DynamicSettingsManager.getInstance(context).notifyListeners(
            AppPreference.KEY.VIDEO_BITRATE, newBitrate
        );
        
        // Show user feedback
        UserFeedbackManager.getInstance(context).showInfo(
            String.format("Video quality adjusted to %.1f Mbps", newBitrate / 1_000_000.0)
        );
    }
    
    /**
     * Estimate latency based on network type
     */
    private int estimateLatency(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return 20; // WiFi typically low latency
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // Estimate based on cellular technology
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                return 30; // 5G/LTE
            } else {
                return 100; // 3G/4G
            }
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return 10; // Ethernet lowest latency
        }
        return 150; // Unknown, assume high
    }
    
    /**
     * Get network type name
     */
    private String getNetworkTypeName(NetworkCapabilities capabilities) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WiFi";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "Cellular";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "Ethernet";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
            return "Bluetooth";
        }
        return "Unknown";
    }
    
    /**
     * Get current network quality
     */
    public NetworkQuality getCurrentQuality() {
        return currentQuality.get();
    }
    
    /**
     * Get network statistics
     */
    public String getNetworkStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Network Statistics ===\n");
        stats.append("Current Quality: ").append(currentQuality.get().label).append("\n");
        
        synchronized (networkSamples) {
            if (!networkSamples.isEmpty()) {
                NetworkSample latest = null;
                for (NetworkSample sample : networkSamples) {
                    latest = sample; // Get last sample
                }
                
                if (latest != null) {
                    stats.append("Bandwidth: ").append(latest.bandwidth).append(" kbps\n");
                    stats.append("Latency: ").append(latest.latency).append(" ms\n");
                    stats.append("Network Type: ").append(latest.networkType).append("\n");
                }
            }
        }
        
        return stats.toString();
    }
    
    /**
     * Force quality reassessment
     */
    public void forceQualityCheck() {
        measureNetworkQuality();
        updateNetworkQuality();
    }
}