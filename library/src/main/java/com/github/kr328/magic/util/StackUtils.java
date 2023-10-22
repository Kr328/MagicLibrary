package com.github.kr328.magic.util;

import androidx.annotation.NonNull;

public final class StackUtils {
    public static boolean isStacktraceContains(@NonNull final String name) {
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getMethodName().contains(name)) {
                return true;
            }
        }

        return false;
    }
}
