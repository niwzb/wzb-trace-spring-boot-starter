package com.wzb.trace.utils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtil {

    public static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final TimeZone TIMEZONE_UTC = new SimpleTimeZone(0, "UTC");
    public static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("Asia/Shanghai");

    public static String formatIso8601Date(Date date) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        df.setTimeZone(TIMEZONE_UTC);
        return df.format(date);
    }

    public static String subtract(String value, int multiple, TimeUnit unit) {
        if (StringUtil.isNotBlank(value)) {
            try {
                return subtract(Long.parseLong(value), multiple, unit);
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(Calendar.getInstance().getTimeInMillis());
    }

    public static String subtract(long value, int multiple, TimeUnit unit) {
        Calendar calendar = Calendar.getInstance();
        long subtract = -multiple * unit.toSeconds(value);
        calendar.add(Calendar.SECOND, (int) subtract);
        return String.valueOf(calendar.getTimeInMillis());
    }

    public static long subtract(long value, double multiple, TimeUnit unit) {
        Calendar calendar = Calendar.getInstance();
        long subtract = (long) (-multiple * unit.toSeconds(value));
        calendar.add(Calendar.SECOND, (int) subtract);
        return calendar.getTimeInMillis();
    }

    public static String dateFormat(Date date, String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        return df.format(date);
    }

    public static String dateFormat(Date date, String pattern, TimeZone timeZone) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(timeZone);
        return df.format(date);
    }

    public static Date formatLocal(String dateStr, String sourcePattern) {
        SimpleDateFormat sourceDateFormat = new SimpleDateFormat(sourcePattern);
        sourceDateFormat.setTimeZone(TIMEZONE_GMT);
        try {
            return sourceDateFormat.parse(dateStr);
        } catch (ParseException ignored) {
        }
        return null;
    }

    public static String formatLocal(String dateStr, String sourcePattern, String targetPattern) {
        SimpleDateFormat sourceDateFormat = new SimpleDateFormat(sourcePattern);
        sourceDateFormat.setTimeZone(TIMEZONE_GMT);
        SimpleDateFormat targetDateFormat = new SimpleDateFormat(targetPattern);
        try {
            Date date = sourceDateFormat.parse(dateStr);
            return targetDateFormat.format(date);
        } catch (ParseException ignored) {
        }
        return dateStr;
    }

    public static String dateFormat(long date, String pattern, TimeUnit unit) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        long millis = unit.toMillis(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return df.format(calendar.getTime());
    }

    public static String dateFormat(long date, String pattern, TimeUnit unit, TimeZone timeZone) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(timeZone);
        long millis = unit.toMillis(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return df.format(calendar.getTime());
    }

    public static Date subtract(Date date, long subtract, TimeUnit unit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, (int) unit.toSeconds(subtract));
        return calendar.getTime();
    }

    public static String getWeekFullYear(String timestamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        DecimalFormat decimalFormat = new DecimalFormat("00");
        dateFormat.setTimeZone(TIMEZONE_UTC);
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(timestamp));
            int year = calendar.get(Calendar.YEAR);
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            return year + decimalFormat.format(week);
        } catch (ParseException ignored) {
        }
        return timestamp;
    }

    public static String subtractWeekFullYear(int weeks) {
        DecimalFormat decimalFormat = new DecimalFormat("00");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks);
        int year = calendar.get(Calendar.YEAR);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        return year + decimalFormat.format(week);
    }

    public static String formatUnit(long date, TimeUnit timeUnit) {
        long seconds = timeUnit.toSeconds(date);
        if (seconds < 1) {
            return timeUnit == TimeUnit.MILLISECONDS ? date + " ms" : "0 ms";
        }
        long minutes = timeUnit.toMinutes(date);
        if (minutes < 1) {
            return seconds + " s";
        }
        long hours = timeUnit.toHours(date);
        if (hours < 1) {
            seconds = seconds - 60 * minutes;
            return minutes + " min" + (seconds > 0 ? " " + seconds + " s" : "");
        }
        long days = timeUnit.toDays(date);
        if (days < 1) {
            minutes = minutes - 60 * hours;
            return hours + " hour" + (minutes > 0 ? " " + minutes + " min" : "");
        }
        long weeks = days / 7;
        if (weeks < 1) {
            hours = hours - 24 * days;
            return days + " day" + (hours > 0 ? " " + hours + " hour" : "");
        }

        days = days - 7 * weeks;
        return weeks + " week" + (days > 0 ? " " + days + " day" : "");
    }
}
