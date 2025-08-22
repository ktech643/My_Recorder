package com.checkmate.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;

import com.checkmate.android.R;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

public class PlayService extends Service {

    private MediaSessionCompat mediaSession;
    int volume_up = 0, volume_down = 0;
    boolean is_handle_key = true;
    public static NotificationManager nm;
    private String NOTIFICATION_CHANNEL_ID = "vcsrecorder.channel.player";
    private final String ACTION_TOGGLE_RECORD_PAUSE = "io.vxg.encodersdk.record_pause_toggle";
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "PlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0) //you simulate a player which plays something.
                .build());

        mediaSession.setPlaybackToRemote(myVolumeProvider);
        mediaSession.setActive(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(11, new Notification());
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void startMyOwnForeground() {
        createNotificationChannel();

        startForeground(10, createNotification("Pause recording"));
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String actionText) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Intent recordPauseIntent = new Intent(ACTION_TOGGLE_RECORD_PAUSE);
        recordPauseIntent.setAction(ACTION_TOGGLE_RECORD_PAUSE);
        recordPauseIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent recordPausePendingIntent =
                PendingIntent.getBroadcast(this, 0, recordPauseIntent, PendingIntent.FLAG_IMMUTABLE);


        if (notificationBuilder == null)
            notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setSmallIcon(R.drawable.ic_fiber_manual_record_trans);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notif);
        notificationBuilder.setContent(remoteViews);

        return notificationBuilder
                .build();
    }

    //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134
    VolumeProviderCompat myVolumeProvider =
            new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, /*max volume*/100, /*initial volume level*/50) {
                @Override
                public void onAdjustVolume(int direction) {
                    if (!is_handle_key) {
                        return;
                    }

                    if (direction == -1) { // volume down
                        Log.e("Play service: ", "volume down key");
                        if (volume_up > 0) {
                            volume_up = 0;
                        }
                        volume_down++;
                        if (volume_down > 15) { // long press of down
                            volume_down = 0;
                            is_handle_key = false;
                            new Handler().postDelayed(() -> is_handle_key = true, 500);
                            MessageUtil.showToast(MainActivity.instance, "Stop record by volume key");
                            MainActivity.instance.stopRecord();
                            CommonUtil.twice_vibrate();
                        }
                    } else if (direction == 1) { // volume up
                        Log.e("Play service: ", "volume up key");
                        if (volume_down > 0) {
                            volume_down = 0;
                        }
                        volume_up++;
                        if (volume_up > 15) { // long press of up
                            volume_up = 0;
                            is_handle_key = false;
                            new Handler().postDelayed(() -> is_handle_key = true, 500);
                            MainActivity.instance.startRecord();
                            CommonUtil.vibrate();
                        }
                        return;
                    } else if (direction == 0) { // volume release
                        Log.e("Play service: ", "volume key release");
                    }
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }
}