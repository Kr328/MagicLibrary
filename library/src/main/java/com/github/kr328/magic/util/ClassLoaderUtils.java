package com.github.kr328.magic.util;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

public final class ClassLoaderUtils {
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    @Nullable
    public static ClassLoader replaceParentClassLoader(@NonNull final ClassLoader target, @Nullable final ClassLoader newParent) throws ReflectiveOperationException {
        final Field field = ClassLoader.class.getDeclaredField("parent");
        field.setAccessible(true);

        final ClassLoader oldParent = (ClassLoader) field.get(target);
        field.set(target, newParent);

        return oldParent;
    }
}
