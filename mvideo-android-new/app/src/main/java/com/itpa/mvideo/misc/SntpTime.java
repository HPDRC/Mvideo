package com.itpa.mvideo.misc;

import android.os.SystemClock;
import android.util.Log;

import java.util.Date;
import java.util.Locale;

public class SntpTime {
    private static final String TAG = SntpTime.class.getSimpleName();
    private static final SntpTime SINGLETON = new SntpTime();

    private float _rootDelayMax = 100;
    private float _rootDispersionMax = 100;
    private int _serverResponseDelayMax = 750;
    private int _udpSocketTimeoutInMillis = 30_000;
    private String _ntpHost = "1.us.pool.ntp.org";
    private SntpClient sntpClient = null;

    public static boolean isInitialized() { return SINGLETON.getIsInitialized(); }
    public static long currentTimeMillis() { return SINGLETON.getCurrentTimeMillis(); }
    public static Date now() { return SINGLETON.getNow(); }

    public static SntpTime build() { return SINGLETON; }

    private synchronized boolean getIsInitialized(){ return sntpClient != null; }

    private synchronized long getCurrentTimeMillis(){
        return sntpClient != null
            ? sntpClient.getCachedSntpTime() + (SystemClock.elapsedRealtime() - sntpClient.getCachedDeviceUptime())
            : System.currentTimeMillis();
    }

    private synchronized Date getNow() { return new Date(getCurrentTimeMillis()); }
    
    public synchronized void initialize() {
        if (sntpClient == null) {
            sntpClient = new SntpClient(_ntpHost, _rootDelayMax, _rootDispersionMax, _serverResponseDelayMax, _udpSocketTimeoutInMillis);
            if(!sntpClient.wasInitialized()) { sntpClient = null; }
        }
    }

    public synchronized SntpTime withConnectionTimeout(int timeoutInMillis) {
        _udpSocketTimeoutInMillis = timeoutInMillis; return SINGLETON;
    }

    public synchronized SntpTime withRootDelayMax(float rootDelayMax) {
        if (rootDelayMax > _rootDelayMax) {
            String log = String.format(Locale.getDefault(),
                "The recommended max rootDelay value is %f. You are setting it at %f",
                _rootDelayMax, rootDelayMax);
            Log.w(TAG, log);
        }
        _rootDelayMax = rootDelayMax;
        return SINGLETON;
    }

    public synchronized SntpTime withRootDispersionMax(float rootDispersionMax) {
        if (rootDispersionMax > _rootDispersionMax) {
            String log = String.format(Locale.getDefault(),
                "The recommended max rootDispersion value is %f. You are setting it at %f",
                _rootDispersionMax, rootDispersionMax);
            Log.w(TAG, log);
        }
        _rootDispersionMax = rootDispersionMax;
        return SINGLETON;
    }

    public synchronized SntpTime withServerResponseDelayMax(int serverResponseDelayInMillis) {
        _serverResponseDelayMax = serverResponseDelayInMillis; return SINGLETON;
    }

    public synchronized SntpTime withNtpHost(String ntpHost) { _ntpHost = ntpHost; return SINGLETON; }
}
