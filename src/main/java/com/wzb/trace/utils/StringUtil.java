package com.wzb.trace.utils;


public class StringUtil {

    public static boolean isNotBlank(String string) {
        return null != string && string.trim().length() > 0;
    }

    public static boolean isBlank(String string) {
        return null == string || string.trim().length() == 0;
    }

    public static String firstSubString(String string, String delimiter) {
        int index = string.indexOf(delimiter);
        return index < 0 ? string : string.substring(0, index);
    }

    public static String getOfNotBlank(String string, String defaultValue) {
        return isNotBlank(string) ? defaultValue : string;
    }

    public static int parseLength(Object object) {
        if (null == object) {
            return 0;
        }
        return String.valueOf(object).length();
    }
}
