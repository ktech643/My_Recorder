package com.checkmate.android.util.HttpServer;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class RootCommandExecutor {

    private final Context context;
    private final String url;
    private final boolean installMagisk;

    public RootCommandExecutor(Context context, String url, boolean installMagisk) {
        this.context = context;
        this.url = url;
        this.installMagisk = installMagisk;
    }

    public void execute() {
        if (installMagisk) {
            downloadFile("magisk.zip", this::installMagiskViaRoot);
        } else {
            downloadFile("update.apk", this::installApk);
        }
    }

    private void downloadFile(String fileName, OnDownloadCompleteListener listener) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading " + fileName);
        request.setDescription("Please wait...");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctxt, Intent intent) {
                Uri uri = manager.getUriForDownloadedFile(downloadId);
                listener.onDownloadComplete(uri);
                context.unregisterReceiver(this);
            }
        };

        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void installMagiskViaRoot(Uri uri) {
        String filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "magisk.zip").getAbsolutePath();
        String command = "su -c 'sh /data/local/tmp/magisk_install.sh \"" + filePath + "\"'";

        try {
            Process process = Runtime.getRuntime().exec(command);
            int result = process.waitFor();
            if (result == 0) {
                Toast.makeText(context, "Magisk installed successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Magisk install failed", Toast.LENGTH_LONG).show();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(context, "Root command error", Toast.LENGTH_LONG).show();
        }
    }

    private void installApk(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    private interface OnDownloadCompleteListener {
        void onDownloadComplete(Uri uri);
    }
}
