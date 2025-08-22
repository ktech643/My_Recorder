package com.checkmate.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.checkmate.android.AppPreference;
import com.checkmate.android.MyApp;

import java.net.URL;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.content.Context.BATTERY_SERVICE;

public class CommonUtil {

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isLocationEnabled() {
        return AppPreference.getBool(AppPreference.KEY.GPS_ENABLED, false);

    }
    public static String secondsToHHMMSS(int seconds) {
        Date d = new Date(seconds * 1000L);
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = df.format(d);
        return time;
    }

    public static String timeToString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-M-dd HH:mm:ss"); // HH for 0-23
        String time = df.format(date);
        return time;
    }

    public static int batteryLevel(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static boolean isCharging(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                float px = 500 * (listView.getResources().getDisplayMetrics().density);
                item.measure(View.MeasureSpec.makeMeasureSpec((int) px, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);
            // Get padding
            int totalPadding = listView.getPaddingTop() + listView.getPaddingBottom();

            // Set list height.
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight + totalPadding;
            listView.setLayoutParams(params);
            listView.requestLayout();
            //setDynamicHeight(listView);
            return true;

        } else {
            return false;
        }
    }

    public static void vibrate() {
        Vibrator v = (Vibrator) MyApp.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    public static void twice_vibrate() {
        long[] pattern = {0, 400, 200, 400, 200};
        Vibrator v = (Vibrator) MyApp.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(VibrationEffect.createWaveform(pattern, -1));
    }

    public static String getVersionCode(Context context) {
        PackageInfo pIno = null;
        try {
            pIno = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        return pIno.versionName;
    }

    public static String getDeviceID(Context context) {
        if (context == null) {
            return AppPreference.getStr(AppPreference.KEY.DEVICE_ID, "");
        }
        String result = "";
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        int checksum = 0;
        if (androidId.length() < 8) {
            return "";
        }
        for (int i = 0; i < 8; i++) {
            String val = androidId.substring(i, i + 1);
            int num = Integer.valueOf(val, 16);
            checksum += num;
            result = result + val;
        }
        String hex_checksum = Integer.toHexString(checksum);
        hex_checksum = hex_checksum.substring(hex_checksum.length() - 1);
        result = result + hex_checksum;
        if (!result.isEmpty()) {
            AppPreference.setStr(AppPreference.KEY.DEVICE_ID, result);
        }else {
            result = AppPreference.getStr(AppPreference.KEY.DEVICE_ID, "");
        }
        return result;
    }

    /**
     * Returns the consumer friendly device name
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    public static String expire_date(String date_string) {
        if (date_string == null || date_string.isEmpty()) {
            return "N/A";  // Handle null or empty cases
        }

        String result = "";
        SimpleDateFormat[] possibleFormats = {
                new SimpleDateFormat("yyyy-MM-dd", Locale.US),  // Format with dashes
                new SimpleDateFormat("yyyy/MM/dd", Locale.US)   // Format with slashes
        };

        Date date = null;
        for (SimpleDateFormat format : possibleFormats) {
            try {
                date = format.parse(date_string);
                break;  // Exit loop if parsing succeeds
            } catch (ParseException ignored) {
            }
        }

        if (date != null) {
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            result = outputFormat.format(date);
        } else {
            result = "Invalid Date";  // Parsing failed for both formats
        }

        return result;
    }


    public static Point screenSize(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size;
    }

    public static Bitmap getFrameAsBitmap(ByteBuffer frame, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(frame);
        return bmp;
    }

    public static String getDomainIP(String url_string) {
        String result = "";
        url_string = url_string.replace("rtsp://", "http://");
        try {
            URL url = new URL(url_string);
            String host = url.getHost();
            result = host.replace("http://", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isValidEmail(String email) {
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    public static String isValidRTSP(String url_string, String username, String password) {
        String result = "";
        url_string = url_string.replaceAll("rtsp://", "http://").replaceAll("rtsps://", "https://");
        try {
            URL url = new URL(url_string);
            String host = url.getHost();
            if (TextUtils.isEmpty(host)) {
                return "";
            }
            int port = url.getPort();
            if (port < 0) {
                return "";
            }
            String authority = url.getAuthority();
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                url_string = url_string.replace(authority, String.format("%s:%s@%s", username, password, authority));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        result = url_string.replaceAll("http://", "rtsp://").replaceAll("https://", "rtsps://");
        return result;
    }

    public static String getRTSPURL(String url_string, String username, String password) {
        String result = "";
        url_string = url_string.replaceAll("rtsp://", "http://").replaceAll("rtsps://", "https://");
        try {
            URL url = new URL(url_string);
            String host = url.getHost();
            if (TextUtils.isEmpty(host)) {
                return "";
            }
            int port = url.getPort();
            if (port < 0) {
                return "";
            }

            String authority = url.getAuthority();
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                url_string = url_string.replace(authority, String.format("%s:%s@%s", username, password, authority));
            }
            url_string = url_string.replaceAll(String.valueOf(port), "8556");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        result = url_string.replaceAll("http://", "rtsp://").replaceAll("https://", "rtsp://");
        return result;
    }

    public static String getWifiSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        return info.getSSID().replaceAll("\"", "");
    }

    public static String formatSerial(String serial) {
        String replace = serial.substring(2, serial.length() - 4);
        StringBuilder substitute = new StringBuilder();
        for (int i = 0; i < serial.length() - 5; i++) {
            substitute.append("*");
        }
        serial = serial.replace(replace, substitute);
        return serial;
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String getFileExtension(String path) {
        return path.substring(path.lastIndexOf("."));
    }

}