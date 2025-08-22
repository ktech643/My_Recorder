package com.checkmate.android.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;
import com.wmspanel.libstream.Streamer;
import com.wmspanel.libstream.StreamerGL;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.os.Environment.getExternalStorageDirectory;

public final class StorageUtils {
    private static final String TAG = "StorageUtils";

    public static final String folder = "checkmate!_files/data";

    private static final Map<Bitmap.CompressFormat, String> EXT_MAP = createExtMap();

    private static Map<Bitmap.CompressFormat, String> createExtMap() {
        Map<Bitmap.CompressFormat, String> result = new HashMap<>();
        result.put(Bitmap.CompressFormat.JPEG, ".jpg");
        result.put(Bitmap.CompressFormat.PNG, ".png");
        result.put(Bitmap.CompressFormat.WEBP, ".webp");
        return Collections.unmodifiableMap(result);
    }

    private static final Map<Bitmap.CompressFormat, String> MIME_MAP = createMimeMap();

    private static Map<Bitmap.CompressFormat, String> createMimeMap() {
        Map<Bitmap.CompressFormat, String> result = new HashMap<>();
        result.put(Bitmap.CompressFormat.JPEG, "image/jpg");
        result.put(Bitmap.CompressFormat.PNG, "image/png");
        result.put(Bitmap.CompressFormat.WEBP, "image/webp");
        return Collections.unmodifiableMap(result);
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getDefaultDirectory() {
        File dir = null;
        if (isExternalStorageWritable()) {
            String path = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, ResourceUtil.getRecordPath());
            if (path.equals("")) {
                dir = new File(getExternalStorageDirectory(), "checkmate!_files/data");
            } else {
                dir = new File(path);
            }
//            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

    public static File getDirectory(Context context) {
//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        String path = sp.getString(context.getString(R.string.pref_custom_dir_key), null);
//
//        if (path != null) {
//            File dir = new File(path);
//            if (dir.isDirectory()) {
//                File test = new File(dir, ".larix");
//                boolean isWritable = isFileWritable(test);
//                //Log.d(TAG, test.getAbsolutePath() + ", isWriteable: " + Boolean.toString(isWritable));
//                if (isWritable) {
//                    return dir;
//                }
//            }
//        }
        return getDefaultDirectory();
    }

    public static File getTempDirectory(Context context) {
        return context.getCacheDir();
    }

    public static String createRecordFilename(Streamer.MODE mode) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return timeStamp.concat(mode == Streamer.MODE.AUDIO_ONLY ? ".m4a" : ".mp4");
    }

    public static String createImageFilename(Bitmap.CompressFormat format) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return timeStamp.concat(EXT_MAP.get(format));
    }

    @Nullable
    public static File newMp4File(Context context, Streamer.MODE mode) {

        File f = getDirectory(context);

        if (f != null) {
            return new File(f, createRecordFilename(mode));
        }
        return null;
    }

    @Nullable
    public static File newMp4Temp(Context context, Streamer.MODE mode) {

        File f = getTempDirectory(context);

        if (f != null) {
            return new File(f, createRecordFilename(mode));
        }
        return null;
    }

    public static File newMp4File(Context context, Streamer.MODE mode, String path) {

        File f = new File(path.trim());

        if (f != null) {
            return new File(f, createRecordFilename(mode));
        }
        return null;
    }

    @Nullable
    public static File newImageFile(Context context, Bitmap.CompressFormat format) {
        File f = getDirectory(context);
        if (f != null) {
            return new File(f, createImageFilename(format));
        }
        return null;
    }

    public static File newFile(Context context, Bitmap.CompressFormat format, String path) {
        File f = new File(path);
        if (f != null) {
            return new File(f, createImageFilename(format));
        }
        return null;
    }

    public static boolean isFileWritable(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fc = raf.getChannel();
            FileLock fl = fc.lock();
            fl.release();
            fc.close();
            raf.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean startRecord(Context ctx, Streamer streamer) {
        return startRecord(ctx, streamer, Streamer.MODE.AUDIO_VIDEO);
    }

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean startRecord(Context ctx, Streamer streamer, Streamer.MODE mode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // https://developer.android.com/reference/android/media/MediaMuxer.html
            // Added in API level 18
            return false;
        }
        if (!SettingsUtils.record(ctx)) {
            return false;
        }

        boolean result;

//        String safUri = null;
        String path = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, ResourceUtil.getRecordPath());
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
//            safUri = sp.getString(ctx.getString(R.string.saf_uri_key), null);
//        }

        if (path != null) {
            // try custom location
            Log.e(TAG, "startRecord: 1");
            result = startRecordSAF(ctx, streamer, path, mode);
            if (!result) {
                Log.e(TAG, "startRecord: 2" );
                // fallback to default DCIM/LarixBroadcaster
                result = startRecordDCIM(ctx, streamer, mode);
            }
        } else {
            result = startRecordDCIM(ctx, streamer, mode);
            Log.e(TAG, "startRecord: else");
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean startTempRecord(Context ctx, Streamer streamer, Streamer.MODE mode) {
        if (!SettingsUtils.record(ctx)) {
            return false;
        }

        boolean result;
        result = startRecordTemp(ctx, streamer, mode);
        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static boolean startRecordTemp(Context ctx, Streamer streamer, Streamer.MODE mode) {
        boolean result = false;
        File f = newMp4Temp(ctx, mode);
        if (f != null && streamer != null) {
            streamer.startRecord(f);
            result = true;
        }

        return result;
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean startRecordSAF(Context ctx, Streamer streamer,
                                          String safUri, Streamer.MODE mode) {
        boolean result = false;
        Log.e(TAG, "startRecordSAF: 260" );

        try {

//            File f = newMp4File(ctx, mode, safUri);
            File f = newMp4File1(ctx,mode);
            if (f != null && streamer != null) {
                streamer.startRecord(f);
                result = true;
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return result;
    }
    static File newMp4File1(Context context, Streamer.MODE mode) {
//        File internalDir = new File(context.getFilesDir(), "temp_videos");
        File internalDir = new File(ResourceUtil.getRecordPath(), "temp_videos");
        if (!internalDir.exists()) {
            internalDir.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss'.mp4'", Locale.US).format(new Date());
        File file=new File(internalDir, createRecordFilename(mode));
        AppPreference.setStr(AppPreference.KEY.temp_PATH, file.getAbsolutePath());

        return file;
    }
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static boolean startRecordDCIM(Context ctx, Streamer streamer, Streamer.MODE mode) {
        boolean result = false;
        Log.e(TAG, "startRecordDCIM: result:" );
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        File f = newMp4File(ctx, mode);
        if (f.exists() && streamer != null) {
            streamer.startRecord(f);
            result = true;
            Log.e(TAG, "startRecordDCIM: result = true;");
        }

//        } else {
//            try {
//                final ContentResolver resolver = ctx.getContentResolver();
//
//                final Uri mime;
//                final String parent;
//                if (mode == Streamer.MODE.AUDIO_ONLY) {
//                    mime = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                    parent = Environment.DIRECTORY_PODCASTS;
//                } else {
//                    mime = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                    parent = Environment.DIRECTORY_DCIM;
//                }
//
//                final ContentValues contentValues = new ContentValues();
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, createRecordFilename(mode));
//                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, parent.concat("/").concat(folder));
//                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
//
//                final Uri recordUri = resolver.insert(mime, contentValues);
//                if (recordUri != null) {
//                    final ParcelFileDescriptor parcel = resolver.openFileDescriptor(recordUri, "rw");
//                    if (parcel != null && parcel.getFileDescriptor() != null) {
//                        streamer.startRecord(parcel, recordUri, Streamer.SAVE_METHOD.MEDIA_STORE);
//                        result = true;
//                    }
//                }
//            } catch (SecurityException | IOException e) {
//                Log.e(TAG, Log.getStackTraceString(e));
//            }
//        }
        return result;
    }

    private static void refreshGallery(Context ctx, File file,
                                       MediaScannerConnection.OnScanCompletedListener callback) {
        // refresh gallery
        if (file != null && file.exists()) {
            MediaScannerConnection.scanFile(
                    ctx,
                    new String[]{file.getAbsolutePath()},
                    null,
                    callback);
        }
    }

    public static boolean takeSnapshot(Context ctx, StreamerGL streamer) {
        boolean result;

        final Bitmap.CompressFormat format = SettingsUtils.snapshotFormat(ctx);
        final int quality = SettingsUtils.snapshotQuality(ctx);

        String safUri = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
//            safUri = sp.getString(ctx.getString(R.string.saf_uri_key), null);
//            safUri = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, "");
//        }
        safUri = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, ResourceUtil.getRecordPath());
        if (safUri != null && !safUri.equals("")) {
            // try custom location
            result = takeSnapshotSAF(ctx, streamer, safUri, format, quality);
            if (!result) {
                // fallback to default DCIM/LarixBroadcaster
                result = takeSnapshotDCIM(ctx, streamer, format, quality);
            }
        } else {
            result = takeSnapshotDCIM(ctx, streamer, format, quality);
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean takeSnapshotSAF(Context ctx, StreamerGL streamer, String safUri,
                                           Bitmap.CompressFormat format, int quality) {

        boolean result = false;

        try {
//            final ContentResolver resolver = ctx.getContentResolver();
//
//            final Uri treeUri = Uri.parse(safUri);
//            final Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
//            if (docUri != null) {
//                final Uri imageUri = DocumentsContract.createDocument(resolver, docUri,
//                        MIME_MAP.get(format), createImageFilename(format));
//                if (imageUri != null) {
//                    final OutputStream os = resolver.openOutputStream(imageUri);
//                    if (os != null) {
//                        streamer.takeSnapshot(os, imageUri, Streamer.SAVE_METHOD.SAF, format, quality);
//                    }
//                    result = true;
//                }
//            }
            final File file = StorageUtils.newFile(ctx, format, safUri);
            if (file != null) {
                streamer.takeSnapshot(file, format, quality, true);
                result = true;
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException | SecurityException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return result;
    }

    private static boolean takeSnapshotDCIM(Context ctx, StreamerGL streamer,
                                            Bitmap.CompressFormat format, int quality) {
        boolean result = false;
        try {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            final File file = StorageUtils.newImageFile(ctx, format);
            if (file != null) {
                streamer.takeSnapshot(file, format, quality, true);
                result = true;
            }
//            } else {
//                final ContentResolver resolver = ctx.getContentResolver();
//
//                final ContentValues contentValues = new ContentValues();
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, createImageFilename(format));
//                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
//                        Environment.DIRECTORY_DCIM.concat("/").concat(folder));
//                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
//
//                final Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//                if (imageUri != null) {
//                    final OutputStream os = resolver.openOutputStream(imageUri);
//                    if (os != null) {
//                        streamer.takeSnapshot(os, imageUri, Streamer.SAVE_METHOD.MEDIA_STORE, format, quality);
//                    }
//                    result = true;
//                }
//            }
        } catch (SecurityException | IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.Q)
    public static String finishInsert(Context ctx, Uri uri) {
        String displayName = null;
        if (uri != null) {
            final ContentResolver resolver = ctx.getContentResolver();

            final ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, contentValues, null, null);

            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    displayName = cursor.getString(nameIndex);
                }
            } catch (IllegalArgumentException | SecurityException e) {
            }
        }
        return displayName;
    }

    public static String onSaveFinished(Context ctx,
                                        Uri uri, Streamer.SAVE_METHOD method,
                                        MediaScannerConnection.OnScanCompletedListener callback) {
        if (uri == null || method == null) {
            return null;
        }

        String displayName = null;
        switch (method) {
            case FILE:
                if (uri.getPath() != null) {
                    String path = uri.getPath();
                    File file = new File(path);
                    StorageUtils.refreshGallery(ctx, file, callback);

                    boolean need_encryption = AppPreference.getBool(AppPreference.KEY.FILE_ENCRYPTION, false);
                    String key = AppPreference.getStr(AppPreference.KEY.ENCRYPTION_KEY, "");
                    if (need_encryption && !TextUtils.isEmpty(key)) {
                        MainActivity.instance.runOnUiThread(() -> {
                            MainActivity.instance.is_dialog = true;
                            MainActivity.instance.dlg_progress.show();
                        });
                        // need to encrypt file if the option is enabled
                        new Thread(() -> {
                            try {
                                String files_dir = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, ResourceUtil.getRecordPath());
                                String file_name = CommonUtil.getFileName(path);
                                String file_extension = CommonUtil.getFileExtension(path);
                                if (TextUtils.equals(file_extension, ".jpg")) {
                                    file_name = file_name.replace(file_extension, ".t3j");
                                } else {
                                    file_name = file_name.replace(file_extension, ".t3v");
                                }
                                String out_path = new File(files_dir, file_name).getPath();
                                boolean result = EncryptDecryptUtils.encode(EncryptDecryptUtils.getInstance(MyApp.getContext()).getCustomKey(key), file, out_path);
                                if (result) {
                                    Log.d("encrypt file", "success");
                                }
                                file.delete();
                                MainActivity.instance.runOnUiThread(() -> {
                                    MainActivity.instance.is_dialog = false;
                                    MainActivity.instance.dlg_progress.dismiss();
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                MainActivity.instance.runOnUiThread(() -> {
                                    MainActivity.instance.dlg_progress.show();
                                    MessageUtil.showToast(MainActivity.instance, "Failed to encrypt");
                                    MainActivity.instance.runOnUiThread(() -> MainActivity.instance.dlg_progress.dismiss());
                                });
                            }
                        }).start();
                    }
                }
                break;
            case SAF:
                File file = null;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    file = getFileFromDocumentUriSAF(ctx, uri);
                    if (file != null) {
                        StorageUtils.refreshGallery(ctx, file, callback);
                    }
                }
                if (file == null) {
                    displayName = uri.toString();
                }
                break;
            case MEDIA_STORE:
                displayName = StorageUtils.finishInsert(ctx, uri);
                break;
            default:
                break;
        }
        return displayName;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static File getFileFromDocumentUriSAF(Context ctx, Uri uri) {
        File file = null;
        String authority = uri.getAuthority();
        if ("com.android.externalstorage.documents".equals(authority)) {
            final String id = DocumentsContract.getDocumentId(uri);
            String[] split = id.split(":");
            if (split.length >= 1) {
                String type = split[0];
                String path = split.length >= 2 ? split[1] : "";
                File[] storagePoints = new File("/storage").listFiles();
                if ("primary".equalsIgnoreCase(type)) {
                    final File externalStorage = Environment.getExternalStorageDirectory();
                    file = new File(externalStorage, path);
                }
                for (int i = 0; storagePoints != null && i < storagePoints.length && file == null; i++) {
                    File externalFile = new File(storagePoints[i], path);
                    if (externalFile.exists()) {
                        file = externalFile;
                    }
                }
                if (file == null) {
                    file = new File(path);
                }
            }
        } else if ("com.android.providers.downloads.documents".equals(authority)) {
            final String id = DocumentsContract.getDocumentId(uri);
            if (id.startsWith("raw:")) {
                String filename = id.replaceFirst("raw:", "");
                file = new File(filename);
            } else {
                try {
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                    String filename = getDataColumn(ctx, contentUri, null, null);
                    if (filename != null) {
                        file = new File(filename);
                    }
                } catch (NumberFormatException e) {
                }
            }
        } else if ("com.android.providers.media.documents".equals(authority)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];
            Uri contentUri = null;
            switch (type) {
                case "image":
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "video":
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "audio":
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    break;
            }
            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{split[1]};
            String filename = getDataColumn(ctx, contentUri, selection, selectionArgs);
            if (filename != null) {
                file = new File(filename);
            }
        }
        return file;
    }

    private static String getDataColumn(Context ctx, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {column};
        String result = null;
        try (Cursor cursor = ctx.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                result = cursor.getString(columnIndex);
            }
        } catch (IllegalArgumentException | SecurityException e) {
        }
        return result;
    }

}
