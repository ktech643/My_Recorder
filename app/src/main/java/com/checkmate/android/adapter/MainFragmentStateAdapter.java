package com.checkmate.android.adapter;

import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.checkmate.android.AppConstant;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.PlaybackFragment;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.fragment.StreamingFragment;

public class MainFragmentStateAdapter extends FragmentStateAdapter {
    private static final String TAG = "MainFragmentStateAdapter";
    
    // Keep references to created fragments to avoid recreation
    private final SparseArray<Fragment> fragments = new SparseArray<>();
    
    public MainFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = fragments.get(position);
        if (fragment != null) {
            Log.d(TAG, "Reusing existing fragment at position: " + position);
            return fragment;
        }
        
        Log.d(TAG, "Creating new fragment at position: " + position);
        switch (position) {
            case AppConstant.SW_FRAGMENT_LIVE:
                fragment = LiveFragment.newInstance();
                break;
            case AppConstant.SW_FRAGMENT_PLAYBACK:
                fragment = PlaybackFragment.newInstance();
                break;
            case AppConstant.SW_FRAGMENT_STREAMING:
                fragment = StreamingFragment.newInstance();
                break;
            case AppConstant.SW_FRAGMENT_SETTINGS:
                fragment = SettingsFragment.newInstance();
                break;

            default:
                fragment = LiveFragment.newInstance();
                break;
        }
        
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4; // Live, Playback, Streaming, Settings (Hide is handled separately)
    }
    
    /**
     * Get fragment by position without creating it
     */
    public Fragment getFragmentByPosition(int position) {
        return fragments.get(position);
    }
    
    /**
     * Check if fragment is created at position
     */
    public boolean isFragmentCreated(int position) {
        return fragments.get(position) != null;
    }
    
    /**
     * Get specific fragment instances (these will only return if fragment exists)
     */
    public LiveFragment getLiveFragment() {
        Fragment fragment = fragments.get(AppConstant.SW_FRAGMENT_LIVE);
        return fragment instanceof LiveFragment ? (LiveFragment) fragment : null;
    }
    
    public PlaybackFragment getPlaybackFragment() {
        Fragment fragment = fragments.get(AppConstant.SW_FRAGMENT_PLAYBACK);
        return fragment instanceof PlaybackFragment ? (PlaybackFragment) fragment : null;
    }
    
    public StreamingFragment getStreamingFragment() {
        Fragment fragment = fragments.get(AppConstant.SW_FRAGMENT_STREAMING);
        return fragment instanceof StreamingFragment ? (StreamingFragment) fragment : null;
    }
    
    public SettingsFragment getSettingsFragment() {
        Fragment fragment = fragments.get(AppConstant.SW_FRAGMENT_SETTINGS);
        return fragment instanceof SettingsFragment ? (SettingsFragment) fragment : null;
    }
    
    /**
     * REMOVE THIS METHOD - it causes fragment recreation
     */
    // public void recreateFragment(int position) {
    //     fragments.remove(position);
    //     notifyItemChanged(position);
    // }
} 