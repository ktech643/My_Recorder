package com.checkmate.android.util;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.checkmate.android.R;
import com.checkmate.android.fragment.LiveFragment_Enhanced;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity_Refactored extends AppCompatActivity {
    
    private static final String TAG = "MainActivity_Refactored";
    
    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;
    
    // Fragment instances
    private LiveFragment_Enhanced liveFragment;
    // Note: Other fragments (PlaybackFragment, StreamingFragment, SettingsFragment) 
    // would need to be created similar to LiveFragment_Enhanced
    
    private Fragment currentFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeComponents();
        setupBottomNavigation();
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(getLiveFragment(), "LiveFragment");
        }
    }
    
    private void initializeComponents() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }
    
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragmentToLoad = null;
            String tag = "";
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_live) {
                fragmentToLoad = getLiveFragment();
                tag = "LiveFragment";
            } else if (itemId == R.id.nav_playback) {
                // fragmentToLoad = getPlaybackFragment();
                // tag = "PlaybackFragment";
                Log.d(TAG, "Playback fragment - to be implemented");
                return true;
            } else if (itemId == R.id.nav_streaming) {
                // fragmentToLoad = getStreamingFragment();
                // tag = "StreamingFragment";
                Log.d(TAG, "Streaming fragment - to be implemented");
                return true;
            } else if (itemId == R.id.nav_settings) {
                // fragmentToLoad = getSettingsFragment();
                // tag = "SettingsFragment";
                Log.d(TAG, "Settings fragment - to be implemented");
                return true;
            } else if (itemId == R.id.nav_hide) {
                handleHideAction();
                return true;
            }
            
            if (fragmentToLoad != null) {
                loadFragment(fragmentToLoad, tag);
                return true;
            }
            
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment, String tag) {
        if (currentFragment == fragment) {
            return; // Already showing this fragment
        }
        
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Hide current fragment if exists
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        
        // Show or add the target fragment
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment != null) {
            transaction.show(existingFragment);
            currentFragment = existingFragment;
        } else {
            transaction.add(R.id.fragment_container, fragment, tag);
            currentFragment = fragment;
        }
        
        transaction.commitAllowingStateLoss();
        
        Log.d(TAG, "Loaded fragment: " + tag);
    }
    
    // Fragment getters with lazy initialization
    private LiveFragment_Enhanced getLiveFragment() {
        if (liveFragment == null) {
            liveFragment = new LiveFragment_Enhanced();
        }
        return liveFragment;
    }
    
    // Note: These methods would need actual fragment implementations
    /*
    private PlaybackFragment getPlaybackFragment() {
        if (playbackFragment == null) {
            playbackFragment = new PlaybackFragment();
        }
        return playbackFragment;
    }
    
    private StreamingFragment getStreamingFragment() {
        if (streamingFragment == null) {
            streamingFragment = new StreamingFragment();
        }
        return streamingFragment;
    }
    
    private SettingsFragment getSettingsFragment() {
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
        }
        return settingsFragment;
    }
    */
    
    private void handleHideAction() {
        // Implement hide functionality
        Log.d(TAG, "Hide action triggered");
        // You can minimize the app or show a minimized view
        moveTaskToBack(true);
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current fragment state if needed
        if (currentFragment != null) {
            outState.putString("current_fragment", currentFragment.getTag());
        }
    }
    
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore fragment state if needed
        String currentFragmentTag = savedInstanceState.getString("current_fragment");
        if (currentFragmentTag != null) {
            currentFragment = fragmentManager.findFragmentByTag(currentFragmentTag);
        }
    }
    
    @Override
    public void onBackPressed() {
        // Handle back button press
        if (currentFragment instanceof LiveFragment_Enhanced) {
            // If we're on the main fragment, exit
            super.onBackPressed();
        } else {
            // Navigate back to live fragment
            bottomNavigation.setSelectedItemId(R.id.nav_live);
        }
    }
}
