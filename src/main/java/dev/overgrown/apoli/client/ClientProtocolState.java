package dev.overgrown.apoli.client;


public final class ClientProtocolState {
    private static int serverVersion = -1;

    private ClientProtocolState() {}

    public static void setServerVersion(int version) {
        serverVersion = version;
    }

    public static int serverVersion() {
        return serverVersion;
    }

    public static void reset() {
        serverVersion = -1;
    }
}
