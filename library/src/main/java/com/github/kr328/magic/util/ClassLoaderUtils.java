package com.github.kr328.magic.util;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;

public final class ClassLoaderUtils {
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    public static ClassLoader replaceParentClassLoader(ClassLoader target, ClassLoader newParent) throws ReflectiveOperationException {
        final Field field = ClassLoader.class.getDeclaredField("parent");
        field.setAccessible(true);

        final ClassLoader oldParent = (ClassLoader) field.get(target);
        field.set(target, newParent);

        return oldParent;
    }
}
