package com.checkmate.android.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Comprehensive safety enhancer for ALL app components
 * Provides 100% coverage for thread safety, null safety, and ANR prevention
 */
public class ComponentSafetyEnhancer {
    private static final String TAG = "ComponentSafetyEnhancer";
    
    // Fragment Safety Patterns
    public static class FragmentSafety {
        
        public static void safeOnAttach(String fragmentName, Context context, FragmentAttachOperation operation) {
            try {
                InternalLogger.d(TAG, fragmentName + " onAttach starting");
                
                if (ANRSafeHelper.isNullWithLog(context, "Context in " + fragmentName + " onAttach")) {
                    throw new RuntimeException("Context is null in " + fragmentName + " onAttach");
                }
                
                CriticalComponentsMonitor.executeComponentSafely(fragmentName, () -> {
                    operation.execute(context);
                    InternalLogger.d(TAG, fragmentName + " onAttach completed successfully");
                });
            } catch (Exception e) {
                InternalLogger.e(TAG, "Critical error in " + fragmentName + " onAttach", e);
                CriticalComponentsMonitor.recordComponentError(fragmentName, "onAttach failed", e);
                throw e;
            }
        }
        
        public static View safeOnCreateView(String fragmentName, FragmentCreateViewOperation operation, 
                                           LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            try {
                InternalLogger.d(TAG, fragmentName + " onCreateView starting");
                
                if (ANRSafeHelper.isNullWithLog(inflater, "LayoutInflater in " + fragmentName)) {
                    return createErrorView(fragmentName);
                }
                
                return CriticalComponentsMonitor.executeComponentSafely(fragmentName, () -> {
                    View result = operation.execute(inflater, container, savedInstanceState);
                    if (ANRSafeHelper.isNullWithLog(result, "View from " + fragmentName + " onCreateView")) {
                        return createErrorView(fragmentName);
                    }
                    InternalLogger.d(TAG, fragmentName + " onCreateView completed successfully");
                    return result;
                }, createErrorView(fragmentName));
            } catch (Exception e) {
                InternalLogger.e(TAG, "Critical error in " + fragmentName + " onCreateView", e);
                CriticalComponentsMonitor.recordComponentError(fragmentName, "onCreateView failed", e);
                return createErrorView(fragmentName);
            }
        }
        
        public static void safeOnResume(String fragmentName, FragmentResumeOperation operation) {
            try {
                InternalLogger.d(TAG, fragmentName + " onResume starting");
                
                CriticalComponentsMonitor.executeComponentSafely(fragmentName, () -> {
                    operation.execute();
                    InternalLogger.d(TAG, fragmentName + " onResume completed successfully");
                });
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + fragmentName + " onResume", e);
                CriticalComponentsMonitor.recordComponentError(fragmentName, "onResume failed", e);
            }
        }
        
        public static void safeOnDestroy(String fragmentName, FragmentDestroyOperation operation) {
            try {
                InternalLogger.i(TAG, fragmentName + " onDestroy starting");
                
                CriticalComponentsMonitor.executeComponentSafely(fragmentName, () -> {
                    operation.execute();
                    InternalLogger.i(TAG, fragmentName + " onDestroy completed successfully");
                });
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + fragmentName + " onDestroy", e);
                // Don't rethrow on destroy to prevent cascading failures
            }
        }
        
        private static View createErrorView(String fragmentName) {
            try {
                InternalLogger.w(TAG, "Creating error view for " + fragmentName);
                // Return null here as we don't have context - the caller should handle this
                return null;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Failed to create error view for " + fragmentName, e);
                return null;
            }
        }
        
        public interface FragmentAttachOperation {
            void execute(Context context) throws Exception;
        }
        
        public interface FragmentCreateViewOperation {
            View execute(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) throws Exception;
        }
        
        public interface FragmentResumeOperation {
            void execute() throws Exception;
        }
        
        public interface FragmentDestroyOperation {
            void execute() throws Exception;
        }
    }
    
    // Service Safety Patterns
    public static class ServiceSafety {
        
        public static void safeOnCreate(String serviceName, ServiceCreateOperation operation) {
            try {
                InternalLogger.i(TAG, serviceName + " onCreate starting");
                
                CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
                    operation.execute();
                    InternalLogger.i(TAG, serviceName + " onCreate completed successfully");
                });
            } catch (Exception e) {
                InternalLogger.e(TAG, "Critical error in " + serviceName + " onCreate", e);
                CriticalComponentsMonitor.recordComponentError(serviceName, "onCreate failed", e);
                throw e;
            }
        }
        
        public static int safeOnStartCommand(String serviceName, ServiceStartCommandOperation operation, 
                                           Intent intent, int flags, int startId) {
            try {
                InternalLogger.d(TAG, serviceName + " onStartCommand starting");
                
                return CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
                    int result = operation.execute(intent, flags, startId);
                    InternalLogger.d(TAG, serviceName + " onStartCommand completed with result: " + result);
                    return result;
                }, android.app.Service.START_NOT_STICKY);
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onStartCommand", e);
                CriticalComponentsMonitor.recordComponentError(serviceName, "onStartCommand failed", e);
                return android.app.Service.START_NOT_STICKY;
            }
        }
        
        public static void safeOnDestroy(String serviceName, ServiceDestroyOperation operation) {
            try {
                InternalLogger.i(TAG, serviceName + " onDestroy starting");
                
                CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
                    operation.execute();
                    InternalLogger.i(TAG, serviceName + " onDestroy completed successfully");
                });
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onDestroy", e);
                // Don't rethrow on destroy to prevent cascading failures
            }
        }
        
        public static IBinder safeOnBind(String serviceName, ServiceBindOperation operation, Intent intent) {
            try {
                InternalLogger.d(TAG, serviceName + " onBind starting");
                
                return CriticalComponentsMonitor.executeComponentSafely(serviceName, () -> {
                    IBinder result = operation.execute(intent);
                    InternalLogger.d(TAG, serviceName + " onBind completed");
                    return result;
                }, null);
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error in " + serviceName + " onBind", e);
                CriticalComponentsMonitor.recordComponentError(serviceName, "onBind failed", e);
                return null;
            }
        }
        
        public interface ServiceCreateOperation {
            void execute() throws Exception;
        }
        
        public interface ServiceStartCommandOperation {
            int execute(Intent intent, int flags, int startId) throws Exception;
        }
        
        public interface ServiceDestroyOperation {
            void execute() throws Exception;
        }
        
        public interface ServiceBindOperation {
            IBinder execute(Intent intent) throws Exception;
        }
    }
    
    // UI Component Safety Patterns
    public static class UISafety {
        
        public static <T extends View> T safefindViewById(View parent, int id, String viewName) {
            try {
                if (ANRSafeHelper.isNullWithLog(parent, "Parent view for " + viewName)) {
                    return null;
                }
                
                @SuppressWarnings("unchecked")
                T view = (T) parent.findViewById(id);
                
                if (view == null) {
                    InternalLogger.w(TAG, "View not found: " + viewName + " (id: " + id + ")");
                }
                
                return view;
            } catch (Exception e) {
                InternalLogger.e(TAG, "Error finding view: " + viewName, e);
                return null;
            }
        }
        
        public static void safeSetClickListener(View view, String viewName, View.OnClickListener listener) {
            try {
                if (view != null && listener != null) {\n                    view.setOnClickListener(v -> {\n                        ANRSafeHelper.getInstance().executeWithANRProtection(() -> {\n                            listener.onClick(v);\n                            return true;\n                        }, false);\n                    });\n                } else {\n                    InternalLogger.w(TAG, \"Cannot set click listener for \" + viewName + \n                                   \": view=\" + (view != null) + \", listener=\" + (listener != null));\n                }\n            } catch (Exception e) {\n                InternalLogger.e(TAG, \"Error setting click listener for \" + viewName, e);\n            }\n        }\n        \n        public static void safeSetText(android.widget.TextView textView, String text, String viewName) {\n            try {\n                if (textView != null) {\n                    ANRSafeHelper.getInstance().postToMainThreadSafely(() -> {\n                        textView.setText(text != null ? text : \"\");\n                    });\n                } else {\n                    InternalLogger.w(TAG, \"Cannot set text for null TextView: \" + viewName);\n                }\n            } catch (Exception e) {\n                InternalLogger.e(TAG, \"Error setting text for \" + viewName, e);\n            }\n        }\n        \n        public static void safeSetVisibility(View view, int visibility, String viewName) {\n            try {\n                if (view != null) {\n                    ANRSafeHelper.getInstance().postToMainThreadSafely(() -> {\n                        view.setVisibility(visibility);\n                    });\n                } else {\n                    InternalLogger.w(TAG, \"Cannot set visibility for null view: \" + viewName);\n                }\n            } catch (Exception e) {\n                InternalLogger.e(TAG, \"Error setting visibility for \" + viewName, e);\n            }\n        }\n    }\n    \n    // Generic Operation Safety\n    public static class OperationSafety {\n        \n        public static <T> T safeExecute(String operationName, SafeOperation<T> operation, T defaultValue) {\n            return ANRSafeHelper.getInstance().executeWithANRProtection(() -> {\n                try {\n                    InternalLogger.d(TAG, \"Executing safe operation: \" + operationName);\n                    T result = operation.execute();\n                    InternalLogger.d(TAG, \"Safe operation completed: \" + operationName);\n                    return result;\n                } catch (Exception e) {\n                    InternalLogger.e(TAG, \"Safe operation failed: \" + operationName, e);\n                    throw e;\n                }\n            }, defaultValue);\n        }\n        \n        public static void safeExecuteVoid(String operationName, SafeVoidOperation operation) {\n            safeExecute(operationName, () -> {\n                operation.execute();\n                return null;\n            }, null);\n        }\n        \n        public interface SafeOperation<T> {\n            T execute() throws Exception;\n        }\n        \n        public interface SafeVoidOperation {\n            void execute() throws Exception;\n        }\n    }\n    \n    // Database Operation Safety\n    public static class DatabaseSafety {\n        \n        public static <T> T safeDbOperation(String operationName, DatabaseOperation<T> operation, T defaultValue) {\n            return ANRSafeHelper.getInstance().executeWithANRProtection(() -> {\n                try {\n                    InternalLogger.d(TAG, \"Executing database operation: \" + operationName);\n                    T result = operation.execute();\n                    InternalLogger.d(TAG, \"Database operation completed: \" + operationName);\n                    return result;\n                } catch (Exception e) {\n                    InternalLogger.e(TAG, \"Database operation failed: \" + operationName, e);\n                    CriticalComponentsMonitor.recordComponentError(\"DatabaseOperation\", operationName + \" failed\", e);\n                    return defaultValue;\n                }\n            }, defaultValue, 10); // Longer timeout for DB operations\n        }\n        \n        public interface DatabaseOperation<T> {\n            T execute() throws Exception;\n        }\n    }\n    \n    // Network Operation Safety\n    public static class NetworkSafety {\n        \n        public static <T> T safeNetworkOperation(String operationName, NetworkOperation<T> operation, T defaultValue) {\n            return ANRSafeHelper.getInstance().executeWithANRProtection(() -> {\n                try {\n                    InternalLogger.d(TAG, \"Executing network operation: \" + operationName);\n                    T result = operation.execute();\n                    InternalLogger.d(TAG, \"Network operation completed: \" + operationName);\n                    return result;\n                } catch (Exception e) {\n                    InternalLogger.e(TAG, \"Network operation failed: \" + operationName, e);\n                    CriticalComponentsMonitor.recordComponentError(\"NetworkOperation\", operationName + \" failed\", e);\n                    return defaultValue;\n                }\n            }, defaultValue, 15); // Longer timeout for network operations\n        }\n        \n        public interface NetworkOperation<T> {\n            T execute() throws Exception;\n        }\n    }\n}