package com.wzb.trace.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {}
        return null;
    }

    public static Object invokeMethod(Method method, Object instance, Object... args) {
        if (null != method) {
            try {
                return method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return null;
    }
}
