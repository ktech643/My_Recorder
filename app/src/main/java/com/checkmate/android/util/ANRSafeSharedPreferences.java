package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ANRSafeSharedPreferences - ANR-protected SharedPreferences wrapper
 * 
 * This class wraps SharedPreferences with ANR protection mechanisms,
 * ensuring that preference operations never block the main thread or
 * cause application timeouts.
 */
public class ANRSafeSharedPreferences implements SharedPreferences {
    
    private static final String TAG = "ANRSafeSharedPreferences";
    
    private final SharedPreferences mDelegate;
    private final ANRProtectionManager mProtectionManager;
    private final ConcurrentHashMap<String, Object> mFastCache = new ConcurrentHashMap<>();
    private final String mPreferenceName;
    
    public ANRSafeSharedPreferences(Context context, String name, ANRProtectionManager protectionManager) {
        this.mPreferenceName = name;
        this.mProtectionManager = protectionManager;
        
        // Get actual SharedPreferences with ANR protection
        this.mDelegate = protectionManager.executePreferenceOperation(
            "create_prefs_" + name,
            () -> context.getSharedPreferences(name, Context.MODE_PRIVATE),
            null
        );
        
        if (mDelegate == null) {
            throw new RuntimeException("Failed to create SharedPreferences: " + name);
        }
        
        Log.d(TAG, "🛡️ ANR-safe SharedPreferences created: " + name);
    }
    
    @Override
    public Map<String, ?> getAll() {
        return mProtectionManager.executePreferenceOperation(
            "getAll_" + mPreferenceName,
            () -> mDelegate.getAll(),
            new ConcurrentHashMap<>()
        );
    }
    
    @Override
    public String getString(String key, String defValue) {
        // Check fast cache first
        Object cached = mFastCache.get(key);
        if (cached instanceof String) {
            return (String) cached;
        }
        
        String result = mProtectionManager.executePreferenceOperation(
            "getString_" + key,
            () -> mDelegate.getString(key, defValue),
            defValue
        );
        
        // Cache the result for fast access
        if (result != null) {
            mFastCache.put(key, result);
        }
        
        return result;
    }
    
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mProtectionManager.executePreferenceOperation(
            "getStringSet_" + key,
            () -> mDelegate.getStringSet(key, defValues),
            defValues
        );
    }
    
    @Override
    public int getInt(String key, int defValue) {
        // Check fast cache first
        Object cached = mFastCache.get(key);
        if (cached instanceof Integer) {
            return (Integer) cached;
        }
        
        Integer result = mProtectionManager.executePreferenceOperation(
            "getInt_" + key,
            () -> mDelegate.getInt(key, defValue),
            defValue
        );
        
        // Cache the result for fast access
        mFastCache.put(key, result);
        
        return result;
    }
    
    @Override
    public long getLong(String key, long defValue) {
        // Check fast cache first
        Object cached = mFastCache.get(key);
        if (cached instanceof Long) {
            return (Long) cached;
        }
        
        Long result = mProtectionManager.executePreferenceOperation(
            "getLong_" + key,
            () -> mDelegate.getLong(key, defValue),
            defValue
        );
        
        // Cache the result for fast access
        mFastCache.put(key, result);
        
        return result;
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        // Check fast cache first
        Object cached = mFastCache.get(key);
        if (cached instanceof Float) {
            return (Float) cached;
        }
        
        Float result = mProtectionManager.executePreferenceOperation(
            "getFloat_" + key,
            () -> mDelegate.getFloat(key, defValue),
            defValue
        );
        
        // Cache the result for fast access
        mFastCache.put(key, result);
        
        return result;
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        // Check fast cache first
        Object cached = mFastCache.get(key);
        if (cached instanceof Boolean) {
            return (Boolean) cached;
        }
        
        Boolean result = mProtectionManager.executePreferenceOperation(
            "getBoolean_" + key,
            () -> mDelegate.getBoolean(key, defValue),
            defValue
        );
        
        // Cache the result for fast access
        mFastCache.put(key, result);
        
        return result;
    }
    
    @Override
    public boolean contains(String key) {
        return mProtectionManager.executePreferenceOperation(
            "contains_" + key,
            () -> mDelegate.contains(key),
            false
        );
    }
    
    @Override
    public Editor edit() {
        return new ANRSafeEditor(mDelegate.edit(), mProtectionManager, mFastCache);
    }
    
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mProtectionManager.executeOnMainThreadSafely(
            "registerListener_" + mPreferenceName,
            () -> mDelegate.registerOnSharedPreferenceChangeListener(listener)
        );
    }
    
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mProtectionManager.executeOnMainThreadSafely(
            "unregisterListener_" + mPreferenceName,
            () -> mDelegate.unregisterOnSharedPreferenceChangeListener(listener)
        );
    }
    
    /**
     * ANR-safe Editor implementation
     */
    private static class ANRSafeEditor implements Editor {
        
        private final Editor mDelegate;
        private final ANRProtectionManager mProtectionManager;
        private final ConcurrentHashMap<String, Object> mFastCache;
        
        public ANRSafeEditor(Editor delegate, ANRProtectionManager protectionManager, 
                           ConcurrentHashMap<String, Object> fastCache) {
            this.mDelegate = delegate;
            this.mProtectionManager = protectionManager;
            this.mFastCache = fastCache;
        }
        
        @Override
        public Editor putString(String key, String value) {
            try {
                mDelegate.putString(key, value);
                if (value != null) {
                    mFastCache.put(key, value);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error putting string: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor putStringSet(String key, Set<String> values) {
            try {
                mDelegate.putStringSet(key, values);
            } catch (Exception e) {
                Log.e(TAG, "Error putting string set: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor putInt(String key, int value) {
            try {
                mDelegate.putInt(key, value);
                mFastCache.put(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error putting int: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor putLong(String key, long value) {
            try {
                mDelegate.putLong(key, value);
                mFastCache.put(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error putting long: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor putFloat(String key, float value) {
            try {
                mDelegate.putFloat(key, value);
                mFastCache.put(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error putting float: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor putBoolean(String key, boolean value) {
            try {
                mDelegate.putBoolean(key, value);
                mFastCache.put(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Error putting boolean: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor remove(String key) {
            try {
                mDelegate.remove(key);
                mFastCache.remove(key);
            } catch (Exception e) {
                Log.e(TAG, "Error removing: " + key, e);
            }
            return this;
        }
        
        @Override
        public Editor clear() {
            try {
                mDelegate.clear();
                mFastCache.clear();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing preferences", e);
            }
            return this;
        }
        
        @Override
        public boolean commit() {
            return mProtectionManager.executePreferenceOperation(
                "commit_editor",
                () -> mDelegate.commit(),
                false
            );
        }
        
        @Override
        public void apply() {
            mProtectionManager.executeOnMainThreadSafely(
                "apply_editor",
                () -> mDelegate.apply()
            );
        }
    }
}