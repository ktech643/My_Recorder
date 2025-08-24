package com.checkmate.android;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe implementation of AppPreference with type safety and ANR prevention.
 * Uses concurrent data structures and non-blocking operations to prevent ANR.
 */
public class ThreadSafeAppPreference {
    private static final String TAG = "ThreadSafeAppPreference";
    
    // Thread-safe singleton instance
    private static volatile ThreadSafeAppPreference instance;
    
    // SharedPreferences instance
    private SharedPreferences sharedPreferences;
    
    // Read-write lock for thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Cache for frequently accessed values
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    
    // Executor for background operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // Handler for main thread operations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Fallback values for crash recovery
    private final ConcurrentHashMap<String, Object> fallbackValues = new ConcurrentHashMap<>();
    
    // Constructor
    private ThreadSafeAppPreference() {
        initializeFallbackValues();
    }
    
    /**
     * Initialize fallback values for crash recovery
     */
    private void initializeFallbackValues() {
        // Add critical fallback values here
        fallbackValues.put(AppPreference.KEY.IS_APP_BACKGROUND, false);
        fallbackValues.put(AppPreference.KEY.IS_RESTART_APP, false);
        fallbackValues.put(AppPreference.KEY.APP_FORCE_QUIT, false);
        fallbackValues.put(AppPreference.KEY.STREAMING_MODE, "default");
        fallbackValues.put(AppPreference.KEY.RECORD_AUDIO, true);
    }
    
    /**
     * Get singleton instance with double-checked locking
     */
    public static ThreadSafeAppPreference getInstance() {
        if (instance == null) {
            synchronized (ThreadSafeAppPreference.class) {
                if (instance == null) {
                    instance = new ThreadSafeAppPreference();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize with SharedPreferences
     * @param preferences SharedPreferences instance
     */
    public static void initialize(@NonNull SharedPreferences preferences) {
        ThreadSafeAppPreference inst = getInstance();
        inst.sharedPreferences = preferences;
        inst.preloadCache();
    }
    
    /**
     * Preload frequently accessed values into cache
     */
    private void preloadCache() {
        executor.execute(() -> {
            try {
                // Preload critical values
                lock.readLock().lock();
                try {
                    if (sharedPreferences != null) {
                        cache.put(AppPreference.KEY.IS_APP_BACKGROUND, 
                                sharedPreferences.getBoolean(AppPreference.KEY.IS_APP_BACKGROUND, false));
                        cache.put(AppPreference.KEY.DEVICE_ID, 
                                sharedPreferences.getString(AppPreference.KEY.DEVICE_ID, ""));
                    }
                } finally {
                    lock.readLock().unlock();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error preloading cache", e);
            }
        });
    }
    
    /**
     * Check if key exists with thread safety
     */
    public boolean contains(@NonNull String key) {
        lock.readLock().lock();
        try {
            if (sharedPreferences == null) return false;
            return sharedPreferences.contains(key);
        } catch (Exception e) {
            Log.e(TAG, "Error checking contains for key: " + key, e);
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get boolean value with thread safety and type checking
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        // Check cache first
        Object cached = cache.get(key);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        
        lock.readLock().lock();
        try {
            if (sharedPreferences == null) {
                return getFallbackValue(key, defaultValue);
            }
            
            boolean value = sharedPreferences.getBoolean(key, defaultValue);
            cache.put(key, value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error getting boolean for key: " + key, e);
            return getFallbackValue(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Set boolean value with thread safety
     */
    public void setBoolean(@NonNull String key, boolean value) {
        cache.put(key, value);
        
        // Use apply() for non-blocking write
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putBoolean(key, value).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting boolean for key: " + key, e);
                // Keep value in cache even if write fails
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Get integer value with thread safety and type checking
     */
    public int getInt(@NonNull String key, int defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }
        
        lock.readLock().lock();
        try {
            if (sharedPreferences == null) {
                return getFallbackValue(key, defaultValue);
            }
            
            int value = sharedPreferences.getInt(key, defaultValue);
            cache.put(key, value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error getting int for key: " + key, e);
            return getFallbackValue(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Set integer value with thread safety
     */
    public void setInt(@NonNull String key, int value) {
        cache.put(key, value);
        
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putInt(key, value).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting int for key: " + key, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Get long value with thread safety
     */
    public long getLong(@NonNull String key, long defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof Long) {
            return (Long) cached;
        }
        
        lock.readLock().lock();
        try {
            if (sharedPreferences == null) {
                return getFallbackValue(key, defaultValue);
            }
            
            long value = sharedPreferences.getLong(key, defaultValue);
            cache.put(key, value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error getting long for key: " + key, e);
            return getFallbackValue(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Set long value with thread safety
     */
    public void setLong(@NonNull String key, long value) {
        cache.put(key, value);
        
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putLong(key, value).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting long for key: " + key, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Get string value with thread safety
     */
    @Nullable
    public String getString(@NonNull String key, @Nullable String defaultValue) {
        Object cached = cache.get(key);
        if (cached instanceof String) {
            return (String) cached;
        }
        
        lock.readLock().lock();
        try {
            if (sharedPreferences == null) {
                return getFallbackValue(key, defaultValue);
            }
            
            String value = sharedPreferences.getString(key, defaultValue);
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Error getting string for key: " + key, e);
            return getFallbackValue(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Set string value with thread safety
     */
    public void setString(@NonNull String key, @Nullable String value) {
        if (value != null) {
            cache.put(key, value);
        } else {
            cache.remove(key);
        }
        
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (value != null) {
                        editor.putString(key, value);
                    } else {
                        editor.remove(key);
                    }
                    editor.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting string for key: " + key, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Remove key with thread safety
     */
    public void removeKey(@NonNull String key) {
        cache.remove(key);
        
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().remove(key).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing key: " + key, e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Batch update for atomic operations
     */
    public void batchUpdate(@NonNull BatchUpdateCallback callback) {
        executor.execute(() -> {
            lock.writeLock().lock();
            try {
                if (sharedPreferences != null) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    BatchEditor batchEditor = new BatchEditor(editor, cache);
                    callback.onBatchUpdate(batchEditor);
                    batchEditor.apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in batch update", e);
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Get fallback value for crash recovery
     */
    @SuppressWarnings("unchecked")
    private <T> T getFallbackValue(String key, T defaultValue) {
        Object fallback = fallbackValues.get(key);
        if (fallback != null && defaultValue != null && 
            fallback.getClass().equals(defaultValue.getClass())) {
            return (T) fallback;
        }
        return defaultValue;
    }
    
    /**
     * Clear cache (use with caution)
     */
    public void clearCache() {
        cache.clear();
        preloadCache();
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Interface for batch updates
     */
    public interface BatchUpdateCallback {
        void onBatchUpdate(BatchEditor editor);
    }
    
    /**
     * Batch editor for atomic operations
     */
    public static class BatchEditor {
        private final SharedPreferences.Editor editor;
        private final ConcurrentHashMap<String, Object> cache;
        
        BatchEditor(SharedPreferences.Editor editor, ConcurrentHashMap<String, Object> cache) {
            this.editor = editor;
            this.cache = cache;
        }
        
        public BatchEditor putBoolean(String key, boolean value) {
            editor.putBoolean(key, value);
            cache.put(key, value);
            return this;
        }
        
        public BatchEditor putInt(String key, int value) {
            editor.putInt(key, value);
            cache.put(key, value);
            return this;
        }
        
        public BatchEditor putLong(String key, long value) {
            editor.putLong(key, value);
            cache.put(key, value);
            return this;
        }
        
        public BatchEditor putString(String key, String value) {
            editor.putString(key, value);
            cache.put(key, value);
            return this;
        }
        
        public BatchEditor remove(String key) {
            editor.remove(key);
            cache.remove(key);
            return this;
        }
        
        void apply() {
            editor.apply();
        }
    }
    
    // Convenience methods for rotation settings
    public void saveRotationSettings(int rotation, boolean isFlipped, boolean isMirrored) {
        batchUpdate(editor -> {
            editor.putInt(AppPreference.KEY.IS_ROTATED, rotation)
                  .putBoolean(AppPreference.KEY.IS_FLIPPED, isFlipped)
                  .putBoolean(AppPreference.KEY.IS_MIRRORED, isMirrored);
        });
    }
    
    public int getRotation() {
        return getInt(AppPreference.KEY.IS_ROTATED, 0);
    }
    
    public boolean isFlipped() {
        return getBoolean(AppPreference.KEY.IS_FLIPPED, false);
    }
    
    public boolean isMirrored() {
        return getBoolean(AppPreference.KEY.IS_MIRRORED, false);
    }
    
    public void resetRotationSettings() {
        saveRotationSettings(0, false, false);
    }
}