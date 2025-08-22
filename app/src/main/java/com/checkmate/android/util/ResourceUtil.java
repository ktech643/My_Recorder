package com.checkmate.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;

public class ResourceUtil {

    public static String RES_DIRECTORY = MyApp.getContext().getExternalCacheDir() + "/jorgeapps/recordApp/";

    private static String getImageFilePath(String fileName) {
        String tempDirPath = RES_DIRECTORY;
        String tempFileName = fileName;

        File tempDir = new File(tempDirPath);
        if (!tempDir.exists())
            tempDir.mkdirs();
        File tempFile = new File(tempDirPath + tempFileName);
        if (!tempFile.exists())
            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        return tempDirPath + tempFileName;
    }

    @Nullable
    public static File newMp4File() {
        String path = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, getRecordPath());
        File f = new File(path);
        if (f.isDirectory()) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss'.mp4'", Locale.US).format(new Date());
            return new File(f, timeStamp);
        }
        return null;
    }

    @Nullable
    public static File newMp4Folder() {
        String path = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, getRecordPath());
        File f = new File(path);
        if (f.isDirectory()) {
            return f;
        }
        return null;
    }

    public static String getRecordPath() {
        return AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
    }

    public static String getRecordPath(String folderName) {
        File mediaStorageDir = new File(getExternalStorageDirectory(), folderName);

        if (!mediaStorageDir.exists()) {
            if (!(mediaStorageDir.mkdirs() || mediaStorageDir.isDirectory())) {
                return "";
            }
        }
        return mediaStorageDir.getPath();
    }

    public static String getSdCardPath(Context context) {

        File[] externalFilesDirs = context.getExternalFilesDirs(null);
        for (File file : externalFilesDirs) {
//            if (Environment.isExternalStorageRemovable(file)) {
//                return file.getAbsolutePath();
//            }
            if (file != null) {
                Log.e("TAG", "getSdCardPath: "+file.getAbsolutePath() );
                return file.getAbsolutePath();  // Return any external storage path
            }
        }
        return null;  // SD card not found
    }

    public static Bitmap filetoBitmap(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        return bitmap;
    }

    public static void setFileName(String file_path, String new_name) {
        String currentFileName = file_path.substring(file_path.lastIndexOf("/"));
        currentFileName = currentFileName.substring(1);
//
        File directory = new File(AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, ResourceUtil.getRecordPath()));
        File from = new File(directory, currentFileName);
        File to = new File(directory, new_name.trim());
        from.renameTo(to);
    }

    public static String getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return formatSize(availableBlocks * blockSize);
    }

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return formatSize(totalBlocks * blockSize);
    }

    public static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
            if (size >= 1024) {
                suffix = "GB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static long videoDuration(File file) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(MyApp.getContext(), Uri.parse(file.getAbsolutePath()));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            retriever.release();
            return timeInMillisec;
        }catch (Exception e ){
            return -0L;
        }
    }

    public static String date(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/YYYY");
        String time = df.format(date);
        return time;
    }

    public static String time(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("h:mm a");
        String time = df.format(date);
        return time;
    }

    public static String milisecondsToHHMMSS(long milliseconds) {
        String hh = "", mm = "", ss = "";
        int seconds = (int) (milliseconds / 1000) % 60;
        if (seconds < 10) {
            ss = "0" + seconds;
        } else {
            ss = String.valueOf(seconds);
        }
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        if (minutes < 10) {
            mm = "0" + minutes;
        } else {
            mm = String.valueOf(minutes);
        }
        int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
        if (hours < 10) {
            hh = "0" + hours;
        } else {
            hh = String.valueOf(hours);
        }
        return String.format("%s:%s:%s", hh, mm, ss);
    }

    public static boolean isExpired() {
        if (TextUtils.isEmpty(AppConstant.expire_date)) {
            return false;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        try {
            Date date = df.parse(AppConstant.expire_date);
            if (new Date().before(date)) {
                return false;
            } else {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Nullable
    public static File newImageFile(Context context, Bitmap.CompressFormat format) {
        File f = getDefaultDirectory();
        if (f != null) {
            return new File(f, createImageFilename(format));
        }
        return null;
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
            dir = new File(getExternalStorageDirectory(), "checkmate!_files/data");
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

    public static final String folder = "checkmate!_files/data";

    private static final Map<Bitmap.CompressFormat, String> EXT_MAP = createExtMap();

    private static Map<Bitmap.CompressFormat, String> createExtMap() {
        Map<Bitmap.CompressFormat, String> result = new HashMap<>();
        result.put(Bitmap.CompressFormat.JPEG, ".jpg");
        result.put(Bitmap.CompressFormat.PNG, ".png");
        result.put(Bitmap.CompressFormat.WEBP, ".webp");
        return Collections.unmodifiableMap(result);
    }

    public static String createImageFilename(Bitmap.CompressFormat format) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return timeStamp.concat(EXT_MAP.get(format));
    }
}
