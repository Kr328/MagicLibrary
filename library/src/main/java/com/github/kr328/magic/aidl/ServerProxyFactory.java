package com.github.kr328.magic.aidl;

import android.os.Binder;
import android.os.IInterface;

public interface ServerProxyFactory<O extends IInterface, R extends IInterface> {
    Binder create(O original, R replaced);
}
