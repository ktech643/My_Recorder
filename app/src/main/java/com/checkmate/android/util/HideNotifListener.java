package com.checkmate.android.util;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class HideNotifListener extends NotificationListenerService {

    private static final int NOTIFICATION_ID = 6000;
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // watch for our service notification
        if (sbn.getPackageName().equals(getPackageName())
                && sbn.getId() == NOTIFICATION_ID) {
            cancelNotification(sbn.getKey());
        }
    }
}
