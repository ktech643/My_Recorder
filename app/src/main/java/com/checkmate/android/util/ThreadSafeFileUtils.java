package com.checkmate.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe file operations utility to prevent ANR issues
 * All file operations are executed on background threads
 */
public class ThreadSafeFileUtils {
    private static final String TAG = "ThreadSafeFileUtils";
    private static final long FILE_OPERATION_TIMEOUT = 5000; // 5 seconds timeout
    private static final int BUFFER_SIZE = 8192; // 8KB buffer
    
    private static volatile ThreadSafeFileUtils instance;
    private final ExecutorService fileExecutor;
    private final CrashLogger crashLogger;
    private final ANRHandler anrHandler;
    private final Handler mainHandler;
    
    /**
     * File operation callback
     */
    public interface FileOperationCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    
    /**
     * Private constructor
     */
    private ThreadSafeFileUtils() {
        this.fileExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "FileOperation-" + System.currentTimeMillis());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        this.crashLogger = CrashLogger.getInstance();
        this.anrHandler = ANRHandler.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get singleton instance
     */
    public static ThreadSafeFileUtils getInstance() {
        if (instance == null) {
            synchronized (ThreadSafeFileUtils.class) {
                if (instance == null) {
                    instance = new ThreadSafeFileUtils();
                }
            }
        }
        return instance;
    }
    
    /**
     * Write string to file asynchronously
     */
    public void writeStringToFile(@NonNull File file, @NonNull String content, 
                                 @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("writeString", () -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
                writer.flush();
                return true;
            }
        }, callback);
    }
    
    /**
     * Append string to file asynchronously
     */
    public void appendStringToFile(@NonNull File file, @NonNull String content,
                                  @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("appendString", () -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(content);
                writer.flush();
                return true;
            }
        }, callback);
    }
    
    /**
     * Read string from file asynchronously
     */
    public void readStringFromFile(@NonNull File file, 
                                  @NonNull FileOperationCallback<String> callback) {
        executeFileOperation("readString", () -> {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }, callback);
    }
    
    /**
     * Copy file asynchronously
     */
    public void copyFile(@NonNull File source, @NonNull File destination,
                        @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("copyFile", () -> {
            try (FileInputStream fis = new FileInputStream(source);
                 FileOutputStream fos = new FileOutputStream(destination);
                 FileChannel sourceChannel = fis.getChannel();
                 FileChannel destChannel = fos.getChannel()) {
                
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                return true;
            }
        }, callback);
    }
    
    /**
     * Move file asynchronously
     */
    public void moveFile(@NonNull File source, @NonNull File destination,
                        @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("moveFile", () -> {
            // Try rename first (faster if on same filesystem)
            if (source.renameTo(destination)) {
                return true;
            }
            
            // Fall back to copy and delete
            try (FileInputStream fis = new FileInputStream(source);
                 FileOutputStream fos = new FileOutputStream(destination);
                 FileChannel sourceChannel = fis.getChannel();
                 FileChannel destChannel = fos.getChannel()) {
                
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                source.delete();
                return true;
            }
        }, callback);
    }
    
    /**
     * Delete file asynchronously
     */
    public void deleteFile(@NonNull File file,
                          @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("deleteFile", () -> {
            if (file.isDirectory()) {
                return deleteDirectory(file);
            } else {
                return file.delete();
            }
        }, callback);
    }
    
    /**
     * Delete directory recursively
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        }
        return false;
    }
    
    /**
     * Create directory with parents asynchronously
     */
    public void createDirectory(@NonNull File directory,
                               @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("createDirectory", directory::mkdirs, callback);
    }
    
    /**
     * Get file size asynchronously
     */
    public void getFileSize(@NonNull File file,
                           @NonNull FileOperationCallback<Long> callback) {
        executeFileOperation("getFileSize", file::length, callback);
    }
    
    /**
     * Check if file exists asynchronously
     */
    public void exists(@NonNull File file,
                      @NonNull FileOperationCallback<Boolean> callback) {
        executeFileOperation("exists", file::exists, callback);
    }
    
    /**
     * List files in directory asynchronously
     */
    public void listFiles(@NonNull File directory,
                         @NonNull FileOperationCallback<File[]> callback) {
        executeFileOperation("listFiles", () -> {
            File[] files = directory.listFiles();
            return files != null ? files : new File[0];
        }, callback);
    }
    
    /**
     * Write bytes to file asynchronously
     */
    public void writeBytesToFile(@NonNull File file, @NonNull byte[] data,
                                @Nullable FileOperationCallback<Boolean> callback) {
        executeFileOperation("writeBytes", () -> {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
                fos.flush();
                return true;
            }
        }, callback);
    }
    
    /**
     * Read bytes from file asynchronously
     */
    public void readBytesFromFile(@NonNull File file,
                                 @NonNull FileOperationCallback<byte[]> callback) {
        executeFileOperation("readBytes", () -> {
            byte[] buffer = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead = fis.read(buffer);
                if (bytesRead != buffer.length) {
                    throw new IOException("Could not read entire file");
                }
            }
            return buffer;
        }, callback);
    }
    
    /**
     * Copy stream asynchronously
     */
    public void copyStream(@NonNull InputStream input, @NonNull OutputStream output,
                          @Nullable FileOperationCallback<Long> callback) {
        executeFileOperation("copyStream", () -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            long totalBytes = 0;
            int bytesRead;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            output.flush();
            return totalBytes;
        }, callback);
    }
    
    /**
     * Execute file operation with timeout protection
     */
    private <T> void executeFileOperation(@NonNull String operationName,
                                         @NonNull FileOperation<T> operation,
                                         @Nullable FileOperationCallback<T> callback) {
        Future<T> future = fileExecutor.submit(() -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                crashLogger.e(TAG, "File operation failed: " + operationName, e);
                throw new RuntimeException(e);
            }
        });
        
        // Monitor operation completion
        fileExecutor.execute(() -> {
            try {
                T result = future.get(FILE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
                
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                crashLogger.e(TAG, "File operation error: " + operationName, e);
                
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
                
                // Cancel if still running
                future.cancel(true);
            }
        });
    }
    
    /**
     * Execute file operation synchronously with timeout (use sparingly)
     */
    @Nullable
    public <T> T executeFileOperationSync(@NonNull String operationName,
                                         @NonNull FileOperation<T> operation) {
        Future<T> future = fileExecutor.submit(() -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(FILE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            crashLogger.e(TAG, "Sync file operation failed: " + operationName, e);
            future.cancel(true);
            return null;
        }
    }
    
    /**
     * Clear all pending operations
     */
    public void clearPendingOperations() {
        fileExecutor.shutdownNow();
    }
    
    /**
     * Shutdown file utils
     */
    public void shutdown() {
        fileExecutor.shutdown();
        try {
            if (!fileExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fileExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fileExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * File operation interface
     */
    private interface FileOperation<T> {
        T execute() throws Exception;
    }
}