package com.itpa.mvideo.misc;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.itpa.mvideo.BaseActivity;
import com.itpa.mvideo.BuildConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Util {

    private static SimpleDateFormat ISO8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    public static String getISO8601StringForCurrentDate() { return getISO8601StringForDate(SntpTime.now()); };

    public static String getISO8601StringForDate(Date date) {
        ISO8601DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return ISO8601DateFormat.format(date);
    };

    public static int secondsToMinutes(int seconds) { return (seconds < 60) ? 1 : ((seconds / 60) + 1); };

    public static String secondsToString(int seconds) {
        if (seconds < 0) return "";
        if (seconds < 60) return "1 minute";
        return ((seconds / 60) + 1) + " minutes";
    };

    public static void apiRequest(Context context, final String url, final JsonObject jsonObject, final String authToken, FutureCallback<JsonObject> futureCallback) {
        String authTokenStr = "";
        if(authToken != null) {
            authTokenStr = "Token " + authToken;
        }
        if (jsonObject == null) {
            if(authToken == null) {
                Ion.with(context).load(url).asJsonObject().setCallback(futureCallback);
            }
            else {
                Ion.with(context).load(url).setHeader("Authorization", authTokenStr).asJsonObject().setCallback(futureCallback);
            }
        }
        else {
            if(authToken == null) {
                Ion.with(context).load(url).setJsonObjectBody(jsonObject).asJsonObject().setCallback(futureCallback);
            }
            else {
                Ion.with(context).load(url).setHeader("Authorization", authTokenStr).setJsonObjectBody(jsonObject).asJsonObject().setCallback(futureCallback);
            }
        }
    }

    public static void apiRequest(Context context, final String url, final JsonObject jsonObject, FutureCallback<JsonObject> futureCallback) {
        apiRequest(context, url, jsonObject,null, futureCallback);
    }

    public static void sendLogToServer(Context context, String message) {
        Log.d(Config.TAG, "log sent to server: " + message);
        if(Config.usingLocalNetwork) { return; }
        Ion.with(context).load(Config.urlAddLog)
                .setBodyParameter("bus_id", Config.busInfoList[Config.busIndex].id)
                .setBodyParameter("message", message)
                .setBodyParameter("bus_time", String.valueOf(SntpTime.currentTimeMillis() / 1000L))
                .asString().setCallback(null);
    }

    public static void sendLogToServer(Context context, String message, Throwable ex) {
        sendLogToServer(context, message + " -- Exception: " + ex.getMessage() + " -- Stack trace: " + Log.getStackTraceString(ex));
    }

    public static int getBatteryPercentage(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        float batteryPct = level / (float) scale;
        return (int) (batteryPct * 100);
    }

    public static String getAppStatus(Context context) {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        return String.format("App Version Code=%d, Battery = %d%%, Used mem = %dMB, Max heap size = %dMB, Available heap size = %dMB", BuildConfig.VERSION_CODE, getBatteryPercentage(context), usedMemInMB, maxHeapSizeInMB, availHeapSizeInMB);
    }
}
