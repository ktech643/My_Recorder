package com.checkmate.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.ui.fragment.LiveFragment;
import com.checkmate.android.ui.fragment.SettingsFragment;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LocationManagerService extends Service {
    private static final String TAG = "LocationManager";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 3 * 1000;
    private static final float LOCATION_DISTANCE = 0.0f;
    public static double lat = 0.0;
    public static double lng = 0.0;
    public static double speed = 0.0;
    public static double bearing = 0.0;
    private static WeakReference<LiveFragment> liveFragment;

    Location previous_location = null;
    private Date datetimeLastCallForDriverLocationUpdate = null;
    Handler timerHandler;
    public static boolean isRunning = false;

    private Location mLastLocation;
    private Handler mTimerHandler;
    private Handler mHandler;

    private long lastApiUpdateTime = 0;

    public LocationManagerService() {
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        startLocationTracking();
        isRunning = true;
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLastLocation = new Location("");
            mLastLocation.setLatitude(0.0);
            mLastLocation.setLongitude(0.0);
            mHandler = new Handler(Looper.getMainLooper());
        }
    }

    private void startLocationTracking() {
        try {
            if (mLocationManager != null) {
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000,
                            0,
                            mLocationListeners[0]);
                    Log.d(TAG, "GPS location updates requested");
                }
                if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            1000,
                            0,
                            mLocationListeners[1]);
                    Log.d(TAG, "Network location updates requested");
                }

                Location lastKnownLocation = null;
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastKnownLocation == null && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastKnownLocation != null) {
                    mLastLocation = lastKnownLocation;
                    lat = lastKnownLocation.getLatitude();
                    lng = lastKnownLocation.getLongitude();
                    Log.d(TAG, "Got last known location: " + lat + ", " + lng);
                    updateUI();
                }

                startPeriodicLocationCheck();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates", e);
        }
    }

    private void startPeriodicLocationCheck() {
        if (mTimerHandler != null) {
            mTimerHandler.removeCallbacksAndMessages(null);
        }
        mTimerHandler = new Handler(Looper.getMainLooper());
        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mLocationManager != null) {
                        Location lastKnownLocation = null;
                        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                        if (lastKnownLocation == null && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        if (lastKnownLocation != null) {
                            mLastLocation = lastKnownLocation;
                            lat = lastKnownLocation.getLatitude();
                            lng = lastKnownLocation.getLongitude();
                            Log.d(TAG, "Periodic location check - Got location: " + lat + ", " + lng);
                            
                            long currentTime = System.currentTimeMillis();
                            int frequencyMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
                            
                            if (currentTime - lastApiUpdateTime >= (frequencyMinutes * 60 * 1000L)) {
                                lastApiUpdateTime = currentTime;
                                updateUI();
                            }
                        }
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Error in periodic location check", e);
                }
                mTimerHandler.postDelayed(this, AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1) * 60 * 1000L);
            }
        }, AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1) * 60 * 1000L);
    }

    private void updateUI() {
        if (mHandler != null) {
            mHandler.post(() -> {
                try {
                    LiveFragment fragment = LiveFragment.getInstance();
                    if (fragment != null && !fragment.isDetached()) {
                        fragment.updateLocation();
                        Log.d(TAG, "UI updated with location: " + lat + ", " + lng);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI", e);
                }
            });
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            try {
                if (location != null && (location.getLatitude() != 0.0 || location.getLongitude() != 0.0)) {
                    mLastLocation = location;
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                    Log.d(TAG, "Location changed: " + lat + ", " + lng);
                    
                    long currentTime = System.currentTimeMillis();
                    int frequencyMinutes = AppPreference.getInt(AppPreference.KEY.FREQUENCY_MIN, 1);
                    
                    if (currentTime - lastApiUpdateTime >= (frequencyMinutes * 60 * 1000L)) {
                        lastApiUpdateTime = currentTime;
                        updateUI();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onLocationChanged", e);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider disabled: " + provider);
            updateUI();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "Provider enabled: " + provider);
            updateUI();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "Provider status changed: " + provider + ", status: " + status);
            updateUI();
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        liveFragment = new WeakReference<>(LiveFragment.getInstance());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        stopLocationTracking();
        isRunning = false;
    }

    private void stopLocationTracking() {
        try {
            if (mTimerHandler != null) {
                mTimerHandler.removeCallbacksAndMessages(null);
                mTimerHandler = null;
            }
            if (mLocationManager != null) {
                mLocationManager.removeUpdates(mLocationListeners[1]);
                mLocationManager.removeUpdates(mLocationListeners[0]);
                Log.d(TAG, "Location updates removed");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Error stopping location tracking", e);
        }
    }
}
