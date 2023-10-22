package com.github.kr328.magic.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ServerProxy {
    private static final String TAG = "ServerProxy";

    @NonNull
    public static <O extends IInterface, R extends IInterface> ServerProxyFactory<O, R> mustCreateFactory(
            @NonNull final Class<O> original,
            @NonNull final Class<R> replace,
            final boolean strict
    ) {
        try {
            return createFactory(original, replace, strict);
        } catch (final ReflectiveOperationException e) {
            Log.e(TAG, "Create factory of " + replace + " failed", e);

            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static <O extends IInterface, R extends IInterface> ServerProxyFactory<O, R> createFactory(
            @NonNull final Class<O> original,
            @NonNull final Class<R> replace,
            final boolean strict
    ) throws ReflectiveOperationException {
        ClassLoader classLoader = original.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        Log.d(TAG, "Create factory of " + replace);

        final Class<?> stub = classLoader.loadClass(original.getName() + "$Stub");

        final Set<Integer> transactCodes = new HashSet<>();

        for (final Method method : replace.getMethods()) {
            final TransactProxy proxy = method.getAnnotation(TransactProxy.class);
            if (proxy == null) {
                continue;
            }

            Log.d(TAG, "Method " + method + " mark as @TransactProxy");

            if (proxy.value() != null && proxy.value().length > 0) {
                transactCodes.addAll(Arrays.stream(proxy.value()).boxed().collect(Collectors.toList()));
            }

            try {
                original.getMethod(method.getName(), method.getParameterTypes());
            } catch (final ReflectiveOperationException e) {
                Log.w(TAG, "Transact code of " + method + " not found.", e);

                if (strict) {
                    throw e;
                }

                continue;
            }

            final Field code = stub.getDeclaredField("TRANSACTION_" + method.getName());
            code.setAccessible(true);
            final int codeValue = code.getInt(original);

            Log.d(TAG, "Transact code of " + method.getName() + " is " + codeValue);
            transactCodes.add(codeValue);
        }

        return (o, r) -> new Binder() {
            final IBinder delegate = r.asBinder();
            final IBinder fallback = o.asBinder();

            @Override
            protected boolean onTransact(final int code, @NonNull final Parcel data, @Nullable final Parcel reply, final int flags) throws RemoteException {
                if (transactCodes.contains(code)) {
                    return delegate.transact(code, data, reply, flags);
                }

                return fallback.transact(code, data, reply, flags);
            }

            @Override
            public void linkToDeath(@NonNull final DeathRecipient recipient, final int flags) {
                try {
                    delegate.linkToDeath(recipient, flags);
                } catch (final RemoteException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean unlinkToDeath(@NonNull final DeathRecipient recipient, final int flags) {
                return delegate.unlinkToDeath(recipient, flags);
            }

            @Override
            public void attachInterface(@Nullable final IInterface owner, @Nullable final String descriptor) {
                // ignore
            }

            @Nullable
            @Override
            public IInterface queryLocalInterface(@NonNull final String descriptor) {
                return null;
            }

            @Nullable
            @Override
            public String getInterfaceDescriptor() {
                try {
                    return fallback.getInterfaceDescriptor();
                } catch (final RemoteException e) {
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
