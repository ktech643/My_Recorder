package com.checkmate.android.service.SharedEGL;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.checkmate.android.service.BaseBackgroundService;

/**
 * Test class for SharedEglManager singleton implementation
 * This class provides utility methods to verify the singleton pattern works correctly
 */
public class SharedEglManagerTest {
    private static final String TAG = "SharedEglManagerTest";

    /**
     * Test the singleton pattern
     * @return true if singleton pattern works correctly
     */
    public static boolean testSingletonPattern() {
        Log.d(TAG, "Testing singleton pattern...");
        
        // Get two instances
        SharedEglManager instance1 = SharedEglManager.getInstance();
        SharedEglManager instance2 = SharedEglManager.getInstance();
        
        // Verify they are the same instance
        boolean isSingleton = (instance1 == instance2);
        Log.d(TAG, "Singleton test result: " + isSingleton);
        
        return isSingleton;
    }

    /**
     * Test service registration
     * @param context Application context
     * @return true if service registration works correctly
     */
    public static boolean testServiceRegistration(Context context) {
        Log.d(TAG, "Testing service registration...");
        
        SharedEglManager manager = SharedEglManager.getInstance();
        
        // Test with a mock service
        MockBackgroundService mockService = new MockBackgroundService();
        
        // Register different service types
        boolean result1 = manager.registerService(ServiceType.BgCamera, mockService);
        boolean result2 = manager.registerService(ServiceType.BgAudio, mockService);
        
        // Verify registration
        boolean registrationWorks = result1 && result2;
        Log.d(TAG, "Service registration test result: " + registrationWorks);
        
        // Clean up
        manager.unregisterService(ServiceType.BgCamera);
        manager.unregisterService(ServiceType.BgAudio);
        
        return registrationWorks;
    }

    /**
     * Test service switching
     * @return true if service switching works correctly
     */
    public static boolean testServiceSwitching() {
        Log.d(TAG, "Testing service switching...");
        
        SharedEglManager manager = SharedEglManager.getInstance();
        MockBackgroundService mockService = new MockBackgroundService();
        
        // Register first service
        manager.registerService(ServiceType.BgCamera, mockService);
        ServiceType active1 = manager.getCurrentActiveService();
        
        // Register second service (should become active)
        manager.registerService(ServiceType.BgAudio, mockService);
        ServiceType active2 = manager.getCurrentActiveService();
        
        // Verify switching
        boolean switchingWorks = (active1 == ServiceType.BgCamera) && (active2 == ServiceType.BgAudio);
        Log.d(TAG, "Service switching test result: " + switchingWorks);
        
        // Clean up
        manager.unregisterService(ServiceType.BgCamera);
        manager.unregisterService(ServiceType.BgAudio);
        
        return switchingWorks;
    }

    /**
     * Test initialization state management
     * @param context Application context
     * @return true if initialization state management works correctly
     */
    public static boolean testInitializationState(Context context) {
        Log.d(TAG, "Testing initialization state management...");
        
        SharedEglManager manager = SharedEglManager.getInstance();
        
        // Test initial state
        boolean initialState = !manager.isInitialized() && !manager.isShuttingDown();
        
        // Initialize
        manager.initialize(context, ServiceType.BgCamera);
        
        // Wait for initialization
        boolean initialized = manager.waitForInitialization(5000);
        
        // Test initialized state
        boolean initializedState = manager.isInitialized() && !manager.isShuttingDown();
        
        // Shutdown
        manager.shutdown();
        
        // Test shutdown state
        boolean shutdownState = !manager.isInitialized() && !manager.isShuttingDown();
        
        boolean stateManagementWorks = initialState && initialized && initializedState && shutdownState;
        Log.d(TAG, "Initialization state management test result: " + stateManagementWorks);
        
        return stateManagementWorks;
    }

    /**
     * Run all tests
     * @param context Application context
     * @return true if all tests pass
     */
    public static boolean runAllTests(Context context) {
        Log.d(TAG, "Running all SharedEglManager tests...");
        
        boolean singletonTest = testSingletonPattern();
        boolean registrationTest = testServiceRegistration(context);
        boolean switchingTest = testServiceSwitching();
        boolean stateTest = testInitializationState(context);
        
        boolean allTestsPass = singletonTest && registrationTest && switchingTest && stateTest;
        
        Log.d(TAG, "All tests result: " + allTestsPass);
        Log.d(TAG, "  - Singleton pattern: " + singletonTest);
        Log.d(TAG, "  - Service registration: " + registrationTest);
        Log.d(TAG, "  - Service switching: " + switchingTest);
        Log.d(TAG, "  - State management: " + stateTest);
        
        return allTestsPass;
    }

    /**
     * Mock background service for testing
     */
    private static class MockBackgroundService extends BaseBackgroundService {
        @Override
        protected ServiceType getServiceType() {
            return ServiceType.BgCamera;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
} 