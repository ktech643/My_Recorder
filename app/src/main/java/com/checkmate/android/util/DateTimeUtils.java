package com.checkmate.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {
    /*
     * Time Format for saving
     */
    public static String DATE_STRING_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public static String DATE_WEEKDAY_FORMAT = "EEEE, MMMM dd, yyyy";
    public static String DATE_TIME_FORMAT = "HH:mm:ss";
    public static String DATE_MONTH_STRING_FORMAT = "MMMM dd, yyyy";
    public static String DATE_MONTH_FORMAT = "MMMM, yyyy";
    public static String DATE_BIRTH_STRING_FORMAT = "dd MMMM yyyy";
    public static String DATE_STRING_FORMAT1 = "yyyy-MM-dd";
//    public static String DATE_FULL_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static String DATE_FULL_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss";
    public static String DATE_STRING_NEXT_HOUR = "MMMM dd hh:mm aa";
    public static String DATE_SPACE_FORMATTER = "yyyy MM dd HH mm ss";

    public static String DATE_ICAL_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static String DATE_ICAL_YEAR_DAY = "yyyy MMMM dd";
    public static String DATE_ICAL_YEAR_LOCAL = "yyyy MMMM dd hh:mm aa";

    public static String DATE_PDF_FORMAT = "MM/dd/yyyy";

    /*
     * Date -> yyyyMMdd
     * Date -> MMdd
     * Date -> yyyy/MM/dd
     * Date -> dd/MM/yyyy
     */
    public static String dateToString(Date date, String strformat) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(strformat);
            return format.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static Date dateToUTC(Date date) {
        return new Date(date.getTime() - Calendar.getInstance().getTimeZone().getOffset(date.getTime()));
    }

    /*
     * yyyyMMdd -> Date
     * MMdd -> Date
     * yyyy/MM/dd -> Date
     * dd/MM/yyyy -> Date
     */
    public static Date stringToDate(String strDate, String strformat) {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat(strformat);
        try {
            date = format.parse(strDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return date;
    }

    // next week date
    public static Date getWeekDate(int week) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        Date date = new Date(calendar.getTime().getTime() + ((week - day) * 24 * 3600 * 1000));
        return date;
    }

    public static Date getPreviewMonthDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        return calendar.getTime();
    }

    // get start date
    public static Date getNextHourDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, hour + 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    // get start date
    public static Date addTime(Date date, int years, int months, int days, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        calendar.set(Calendar.YEAR, year + years);
        calendar.set(Calendar.MONTH, month + months);
        calendar.set(Calendar.DAY_OF_MONTH, day + days);
        calendar.set(Calendar.HOUR_OF_DAY, hour + hours);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date getBeginningofDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getEndofDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public static boolean isBeforeDate(Date first, Date second) {
        return getBeginningofDay(second).after(getBeginningofDay(first));
    }
}
