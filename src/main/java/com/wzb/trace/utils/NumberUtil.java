package com.wzb.trace.utils;

public class NumberUtil {

    public static long parseLong(String longStr, long defaultValue) {
        if (StringUtil.isNotBlank(longStr)) {
            try {
                return Long.parseLong(longStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

}
