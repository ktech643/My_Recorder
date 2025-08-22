package com.checkmate.android.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.checkmate.android.service.FragmentVisibilityListener;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.util.MainActivity;

/**
 * Troubleshooting utility for common ViewPager2 issues
 */
public class ViewPager2Troubleshooting {
    
    private static final String TAG = "ViewPager2Troubleshooting";
    
    /**
     * Fix TextureView black screen issue
     */
    public static void fixTextureViewBlackScreen(LiveFragment liveFragment) {
        if (liveFragment != null && liveFragment.previewAfl != null) {
            // Force TextureView refresh
            liveFragment.previewAfl.setSurfaceTextureListener(null);
            
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (liveFragment.mActivityRef != null && liveFragment.mActivityRef.get() != null) {
                    MainActivity activity = liveFragment.mActivityRef.get();
                    liveFragment.previewAfl.setSurfaceTextureListener(activity.mSurfaceTextureListener);
                }
            }, 100);
        }
    }
    
    /**
     * Fix fragment not responding to visibility changes
     */
    public static void fixFragmentVisibility(MainActivity activity, int fragmentPosition) {
        Fragment fragment = activity.getFragmentByPosition(fragmentPosition);
        if (fragment instanceof FragmentVisibilityListener) {
            ((FragmentVisibilityListener) fragment).onFragmentVisible();
        }
    }
    
    /**
     * Emergency reset all fragments
     */
    public static void emergencyResetFragments(MainActivity activity) {
        activity.runOnUiThread(() -> {
            try {
                // Get current position
                int currentPosition = activity.getCurrentFragmentIndex();
                
                // Force recreate adapter
                activity.fragmentAdapter = new com.checkmate.android.adapter.MainFragmentStateAdapter(activity);
                activity.viewPager.setAdapter(activity.fragmentAdapter);
                
                // Restore position
                activity.viewPager.setCurrentItem(currentPosition, false);
                
                Log.d(TAG, "Emergency fragment reset completed");
            } catch (Exception e) {
                Log.e(TAG, "Error in emergency reset: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Check if ViewPager2 is properly initialized
     */
    public static boolean isViewPager2ProperlyInitialized(MainActivity activity) {
        return activity.viewPager != null && 
               activity.fragmentAdapter != null && 
               activity.viewPager.getAdapter() != null;
    }
    
    /**
     * Validate fragment positions
     */
    public static boolean validateFragmentPositions() {
        return com.checkmate.android.AppConstant.SW_FRAGMENT_LIVE == 0 &&
               com.checkmate.android.AppConstant.SW_FRAGMENT_PLAYBACK == 1 &&
               com.checkmate.android.AppConstant.SW_FRAGMENT_STREAMING == 2 &&
               com.checkmate.android.AppConstant.SW_FRAGMENT_SETTINGS == 3;
    }
} 