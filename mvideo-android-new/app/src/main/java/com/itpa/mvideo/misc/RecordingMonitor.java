package com.itpa.mvideo.misc;

public class RecordingMonitor {

    private static long lastRecorderSuccess = 0;
    private static long lastNetworkSuccess = 0;

    public static void setRecorderSuccess() {
        lastRecorderSuccess = SntpTime.currentTimeMillis();
    }

    public static boolean isRecordingSuccessful() {
        return SntpTime.currentTimeMillis() - lastRecorderSuccess < Config.RECORDER_ERROR_TOLLERANCE_MS;
    }

    public static void setNetworkSuccess() {
        lastNetworkSuccess = SntpTime.currentTimeMillis();
    }

    public static boolean isNetworkAvailable() {
        return SntpTime.currentTimeMillis() - lastNetworkSuccess < Config.NETWORK_ERROR_TOLERANCE_MS;
    }
}
