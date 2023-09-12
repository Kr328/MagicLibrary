package android.os;

public final class ServiceManager {
    private static IServiceManager sServiceManager;

    private static IServiceManager getIServiceManager() {
        throw new IllegalArgumentException("Stub!");
    }

    public static IBinder getService(final String name) {
        throw new IllegalArgumentException("Stub!");
    }
}
