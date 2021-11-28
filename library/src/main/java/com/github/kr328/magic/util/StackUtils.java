package com.github.kr328.magic.util;

public final class StackUtils {
    public static boolean isStacktraceContains(String name) {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getMethodName().contains(name)) {
                return true;
            }
        }

        return false;
    }
}
