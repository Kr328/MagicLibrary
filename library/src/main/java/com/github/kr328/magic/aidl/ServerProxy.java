package com.github.kr328.magic.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ServerProxy {
    public static <O extends IInterface, R extends IInterface> ServerProxyFactory<O, R> createFactory(
            Class<O> original,
            Class<R> replace,
            boolean strict
    ) throws ReflectiveOperationException {
        ClassLoader classLoader = original.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        final Class<?> stub = classLoader.loadClass(original.getName() + "$Stub");

        final Set<Integer> transactCodes = new HashSet<>();

        for (Method method : replace.getMethods()) {
            TransactProxy proxy = method.getAnnotation(TransactProxy.class);
            if (proxy == null) {
                continue;
            }

            if (proxy.value().length > 0) {
                transactCodes.addAll(Arrays.stream(proxy.value()).boxed().collect(Collectors.toList()));
            }

            try {
                original.getMethod(method.getName(), method.getParameterTypes());
            } catch (ReflectiveOperationException e) {
                if (strict) {
                    throw e;
                }

                continue;
            }

            final Field code = stub.getDeclaredField("TRANSACTION_" + method.getName());
            code.setAccessible(true);
            transactCodes.add(code.getInt(original));
        }

        return (o, r) -> new Binder() {
            final IBinder delegate = r.asBinder();
            final IBinder fallback = o.asBinder();

            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
                if (transactCodes.contains(code)) {
                    return delegate.transact(code, data, reply, flags);
                }

                return fallback.transact(code, data, reply, flags);
            }

            @Override
            public void linkToDeath(DeathRecipient recipient, int flags) {
                try {
                    delegate.linkToDeath(recipient, flags);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
                return delegate.unlinkToDeath(recipient, flags);
            }

            @Override
            public void attachInterface(IInterface owner, String descriptor) {
                // ignore
            }

            @Override
            public IInterface queryLocalInterface(String descriptor) {
                return null;
            }

            @Override
            public String getInterfaceDescriptor() {
                try {
                    return fallback.getInterfaceDescriptor();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean pingBinder() {
                return fallback.pingBinder();
            }

            @Override
            public boolean isBinderAlive() {
                return fallback.isBinderAlive();
            }
        };
    }
}
