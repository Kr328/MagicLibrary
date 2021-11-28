package com.github.kr328.magic.proxy;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.BiFunction;

@SuppressLint("DiscouragedPrivateApi")
public final class ServiceManagerProxy {
    private final BiFunction<String, Binder, Binder> addServiceFilter;
    private final BiFunction<String, IBinder, IBinder> getServiceFilter;
    private boolean installed = false;

    private ServiceManagerProxy(
            BiFunction<String, Binder, Binder> addServiceFilter,
            BiFunction<String, IBinder, IBinder> getServiceFilter
    ) {
        this.addServiceFilter = addServiceFilter;
        this.getServiceFilter = getServiceFilter;
    }

    private static ClassLoader getClassLoader() {
        final ClassLoader self = ServiceManagerProxy.class.getClassLoader();
        if (self != null) {
            return self;
        }

        return ClassLoader.getSystemClassLoader();
    }

    public synchronized void install() throws ReflectiveOperationException {
        if (installed) {
            return;
        }

        final Class<?> cServiceManager = getClassLoader().loadClass("android.os.ServiceManager");
        final Class<?> cIServiceManager = getClassLoader().loadClass("android.os.IServiceManager");
        final Method mGetIServiceManager = cServiceManager.getDeclaredMethod("getIServiceManager");
        final Field fSServiceManager = cServiceManager.getDeclaredField("sServiceManager");

        mGetIServiceManager.setAccessible(true);
        fSServiceManager.setAccessible(true);

        final Object original = mGetIServiceManager.invoke(null);
        final Object proxy = Proxy.newProxyInstance(
                ServiceManagerProxy.class.getClassLoader(),
                new Class[]{cIServiceManager},
                (_proxy, method, args) -> {
                    if (method.getName().startsWith("getService") && args.length >= 1) {
                        if (args[0] instanceof String) {
                            try {
                                final String name = (String) args[0];
                                final IBinder service = (IBinder) method.invoke(original, args);

                                return getServiceFilter.apply(name, service);
                            } catch (Exception ignored) {
                                // Ignore filter exceptions
                            }

                            return method.invoke(original, args);
                        }
                    } else if (method.getName().startsWith("addService") && args.length >= 2) {
                        if (args[0] instanceof String && args[1] instanceof Binder) {
                            final String name = (String) args[0];
                            final Binder service = (Binder) args[1];

                            try {
                                final Object[] newArgs = Arrays.copyOf(args, args.length);

                                newArgs[1] = addServiceFilter.apply(name, service);

                                return method.invoke(original, newArgs);
                            } catch (Exception ignored) {
                                // Ignore filter exceptions
                            }
                        }
                    }

                    return method.invoke(original, args);
                }
        );

        fSServiceManager.set(null, proxy);

        installed = true;
    }

    public synchronized void uninstall() throws ReflectiveOperationException {
        if (!installed) {
            return;
        }

        getClassLoader().loadClass("android.os.ServiceManager")
                .getDeclaredField("sServiceManager")
                .set(null, null);

        installed = false;
    }

    public static final class Builder {
        private BiFunction<String, Binder, Binder> addServiceFilter;
        private BiFunction<String, IBinder, IBinder> getServiceFilter;

        public Builder setAddServiceFilter(BiFunction<String, Binder, Binder> func) {
            this.addServiceFilter = func;
            return this;
        }

        public Builder setGetServiceFilter(BiFunction<String, IBinder, IBinder> func) {
            this.getServiceFilter = func;
            return this;
        }

        public ServiceManagerProxy build() {
            BiFunction<String, Binder, Binder> add = addServiceFilter;
            BiFunction<String, IBinder, IBinder> get = getServiceFilter;

            if (add == null) {
                add = (name, service) -> service;
            }

            if (get == null) {
                get = (name, service) -> service;
            }

            return new ServiceManagerProxy(add, get);
        }
    }
}
