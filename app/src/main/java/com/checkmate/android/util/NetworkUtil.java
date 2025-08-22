package com.checkmate.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Network utility methods
 */
public class NetworkUtil {
    
    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        
        return false;
    }
    
    /**
     * Check if connected to WiFi
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && 
                   activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && 
                   activeNetwork.isConnected();
        }
        
        return false;
    }
}