package com.checkmate.android.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.checkmate.android.AppPreference;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.checkmate.android.AppConstant.FILE_EXT;
import static com.checkmate.android.AppConstant.FILE_NAME;
import static com.checkmate.android.AppConstant.FILE_NAME_DE;
import static com.checkmate.android.AppConstant.TEMP_FILE_NAME;

/**
 * Created by Vipin on 5/6/18.
 */

public class FileUtils {

    public static void saveFile(byte[] encodedBytes, String path) {
        try {
            File file = new File(path);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(encodedBytes);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static byte[] readFile(String filePath) {
        byte[] contents;
        File file = new File(filePath);
        int size = (int) file.length();
        contents = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(
                    new FileInputStream(file));
            try {
                buf.read(contents);
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return contents;
    }

    @NonNull
    public static File createTempFile(Context context, byte[] decrypted) throws IOException {
        File tempFile = File.createTempFile(TEMP_FILE_NAME, FILE_EXT, context.getCacheDir());
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(decrypted);
        fos.close();
        return tempFile;
    }

    public static FileDescriptor getTempFileDescriptor(Context context, byte[] decrypted) throws IOException {
        File tempFile = createTempFile(context, decrypted);
        FileInputStream fis = new FileInputStream(tempFile);
        return fis.getFD();
    }

    public static final String getDirPath(Context context) {
        String path = AppPreference.getStr(AppPreference.KEY.VIDEO_PATH, ResourceUtil.getRecordPath());
        File directory = new File(path);
        return directory.getAbsolutePath();
    }

    public static final String getFilePath(Context context) {
        return getDirPath(context) + File.separator + FILE_NAME;
    }

    public static final String getDecryptFilePath(Context context) {
        return getDirPath(context) + File.separator + FILE_NAME_DE;
    }

    public static final void deleteDownloadedFile(Context context) {
        File file = new File(getFilePath(context));
        if (null != file && file.exists()) {
            if (file.delete()) Log.i("FileUtils", "File Deleted.");
        }
    }


}
