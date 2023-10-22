package com.github.kr328.magic.aidl;

import android.os.Binder;
import android.os.IInterface;

import androidx.annotation.NonNull;

public interface ServerProxyFactory<O extends IInterface, R extends IInterface> {
    @NonNull
    Binder create(@NonNull O original, @NonNull R replaced);
}
