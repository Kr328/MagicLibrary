package com.github.kr328.magic.util;

import android.os.Binder;

import androidx.annotation.NonNull;

import com.github.kr328.magic.action.Action;
import com.github.kr328.magic.action.VoidAction;

public final class BinderUtils {
    public static <R, T extends Throwable> R withEvaluated(@NonNull final Action<R, T> action) throws T {
        final long token = Binder.clearCallingIdentity();

        try {
            return action.run();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static <T extends Throwable> void withEvaluated(@NonNull final VoidAction<T> action) throws T {
        withEvaluated(() -> {
            action.run();

            return null;
        });
    }
}
