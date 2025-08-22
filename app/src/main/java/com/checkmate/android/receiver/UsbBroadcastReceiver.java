package com.checkmate.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import com.checkmate.android.util.MessageUtil;
import com.kongzue.dialogx.dialogs.MessageDialog;

public class UsbBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            // Handle USB device attached
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            // Check if the device is a camera and handle according
            MessageDialog.show("usb device attached", "" + device.getDeviceName() + "","Ok");
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            // Handle USB device detached
        }
    }
}
