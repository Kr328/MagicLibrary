package com.github.kr328.magic.services;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.IBinder;
import android.os.IServiceManager;
import android.os.ServiceManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.BiFunction;

@SuppressLint("DiscouragedPrivateApi")
public class ServiceManagerProxy {
    private static final String TAG = "ServiceManagerProxy";

    private final BiFunction<String, Binder, Binder> addServiceFilter;
    private final BiFunction<String, IBinder, IBinder> getServiceFilter;
    private boolean installed = false;

    private ServiceManagerProxy(
            final BiFunction<String, Binder, Binder> addServiceFilter,
            final BiFunction<String, IBinder, IBinder> getServiceFilter
    ) {
        this.addServiceFilter = addServiceFilter;
        this.getServiceFilter = getServiceFilter;
    }

    public static void install(final Interceptor interceptor) throws ReflectiveOperationException {
        final Method mGetIServiceManager = ServiceManager.class.getDeclaredMethod("getIServiceManager");
        final Field fSServiceManager = ServiceManager.class.getDeclaredField("sServiceManager");

        mGetIServiceManager.setAccessible(true);
        fSServiceManager.setAccessible(true);

        final Object original = mGetIServiceManager.invoke(null);
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getService".equals(method.getName())) {
                if (args != null &&
                        args.length >= 1 &&
                        args[0] instanceof String &&
                        method.getReturnType().isAssignableFrom(IBinder.class)
                ) {
                    try {
                        return interceptor.getService(
                                (String) args[0],
                                (IBinder) method.invoke(original, args)
                        );
                    } catch (final Throwable throwable) {
                        Log.e(TAG, "Handler: getService", throwable);
                    }
                }
            } else if ("addService".equals(method.getName())) {
                if (args != null &&
                        args.length >= 2 &&
                        args[0] instanceof String &&
                        args[1] instanceof Binder
                ) {
                    try {
                        final Object[] newArgs = Arrays.copyOf(args, args.length);

                        newArgs[1] = interceptor.addService((String) newArgs[0], (Binder) newArgs[1]);

                        return method.invoke(original, newArgs);
                    } catch (final Throwable throwable) {
                        Log.e(TAG, "Handler: addService", throwable);
                    }
                }
            }

            return method.invoke(original, args);
        };
        final Object proxy = Proxy.newProxyInstance(
                ServiceManagerProxy.class.getClassLoader(),
                new Class[]{IServiceManager.class},
                handler
        );

        fSServiceManager.set(null, proxy);
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

        install(new Interceptor() {
            @Override
            public IBinder getService(final String name, final IBinder service) {
                if (getServiceFilter != null) {
                    return getServiceFilter.apply(name, service);
                }

                return service;
            }

            @Override
            public Binder addService(final String name, final Binder service) {
                if (addServiceFilter != null) {
                    return addServiceFilter.apply(name, service);
                }

                return service;
            }
        });

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

    public abstract static class Interceptor {
        public IBinder getService(final String name, final IBinder service) {
            return service;
        }

        public Binder addService(final String name, final Binder service) {
            return service;
        }
    }

    public static final class Builder {
        private BiFunction<String, Binder, Binder> addServiceFilter;
        private BiFunction<String, IBinder, IBinder> getServiceFilter;

        public Builder setAddServiceFilter(final BiFunction<String, Binder, Binder> func) {
            this.addServiceFilter = func;
            return this;
        }

        public Builder setGetServiceFilter(final BiFunction<String, IBinder, IBinder> func) {
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
