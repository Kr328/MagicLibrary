package com.github.kr328.magic.proxy;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Deprecated
public final class AIDLProxy {
    public static <T extends IInterface> Binder newServer(
            Class<T> interfaceClass,
            T original,
            T proxy
    ) throws ReflectiveOperationException {
        final Set<Integer> transactCodes = collectTransactCodes(interfaceClass, proxy.getClass().getMethods());

        return new Binder() {
            final IBinder delegate = proxy.asBinder();
            final IBinder fallback = original.asBinder();

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

    private static Set<Integer> collectTransactCodes(Class<?> interfaceClass, Method[] methods) throws ReflectiveOperationException {
        ClassLoader loader = interfaceClass.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        final Set<Integer> result = new HashSet<>();
        final Class<?> cStub = loader.loadClass(interfaceClass.getName() + "$Stub");
        final Method mAsInterface = cStub.getMethod("asInterface", IBinder.class);
        final Binder dump = new Binder() {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                result.add(code);
                return true;
            }
        };
        final Object dumpInterface = mAsInterface.invoke(null, dump);
        if (dumpInterface == null) {
            return result;
        }

        final Class<?> cDump = dumpInterface.getClass();
        for (Method method : methods) {
            final TransactProxy proxy = method.getAnnotation(TransactProxy.class);
            if (proxy == null) {
                continue;
            }

            if (proxy.value().length != 0) {
                for (int code : proxy.value()) {
                    result.add(code);
                }
                continue;
            }

            try {
                cDump.getMethod(method.getName(), method.getParameterTypes())
                        .invoke(dumpInterface, generateCallArgs(method));
            } catch (NoSuchMethodException ignored) {
                // Ignore not existed methods
            }
        }

        return result;
    }

    private static Object[] generateCallArgs(Method method) {
        return Arrays.stream(method.getParameterTypes()).map(t -> {
            switch (t.getName()) {
                case "int":
                    return 0;
                case "short":
                    return (short) 0;
                case "long":
                    return (long) 0;
                case "byte":
                    return (byte) 0;
                case "char":
                    return '\0';
                case "boolean":
                    return false;
                case "float":
                    return 0.0f;
                case "double":
                    return 0.0;
                default:
                    return (Object) null;
            }
        }).toArray();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransactProxy {
        int[] value() default {};
    }
}
