package com.itpa.mvideo.misc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Config {

    //#################################### test related ##########################################
    public static boolean isTestMode = false;
    public static int testCounter = 0;
    public static int testChangeLatLonInterval = 5000;
    public static long testLastChangeLatlon = 0;
    public static double[] testLatlon  = new double[]{
            25.764352, -80.373402,
            25.764252, -80.373302,
            25.764152, -80.373202,
            25.764102, -80.373152,
            25.764052, -80.373102,  // at stop 1032
            25.763952, -80.373002,
            25.763902, -80.372902,
            25.763852, -80.372802,
            25.763802, -80.372702,
            25.763737, -80.372658, // at stop 1033
            25.763700, -80.373158,
            25.763637, -80.373358,
            25.763600, -80.373758,
            25.763537, -80.374058,
            25.762511, -80.374408 // at stop 1034
    };

    //####################################### below are constants #######################################

    public static final String TAG = "tfmvideo";

    public static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    //public static final int BIT_RATE = 262144 * 2;
    public static final int BIT_RATE = 262144;
    public static final int GOP_SIZE = 10;
    public static final int FRAME_RATE = 30;
    public static final float SPEECH_RATE = 0.7f;

    public static final float SCREEN_BRIGHTNESS_STANDBY = 0.3f;
    public static final float SCREEN_BRIGHTNESS_ACTIVE = 1.0f;

    public static final String PASS_CODE = "2468";

    public static final BusInfo[] busInfoList = new BusInfo[] {
            new BusInfo("5012", "MPV-1", "MPV-1-Passenger", 0),
            new BusInfo("25002", "MPV-2", "MPV-2-Passenger", 0),
            new BusInfo("5011", "MPV-3", "MPV-3-Passenger", 0),
            new BusInfo("5667", "SW-1", "SW-1-Passenger", 1),
            new BusInfo("7140", "SW-2", "SW-2-Passenger", 1),
            new BusInfo("8828", "SW-3", "SW-3-Passenger", 1),
            new BusInfo("1103", "SW-4", "SW-4-Passenger", 1),
            new BusInfo("4056", "SW-5", "SW-5-Passenger", 1),
            new BusInfo("4061", "SW-6", "SW-6-Passenger", 1),
            new BusInfo("25001", "SW-7", "SW-7-Passenger", 1)
    };

    public static final int MAIN_UPDATE_MS = 1000;
    public static final int MAIN_MANAGE_AUTO_HIDE_MS = 15000;

    public static final int TEST_NETWORK_CONNECTIVITY_MS = 30000;
    public static final int SEND_APP_STATUS_MS = 180000;
    public static final int SEND_LOCATION_TO_MVIDEO_MS = 15000;
    public static final int SEND_LOCATION_TO_API_MS = 2000;
    public static final int SEND_LOCATION_TO_ETA_MS = 2000;
    public static final int ETA_OUTDATE_MS = 90000; // when ETA is not updated for this much time, consider it as out of date

    public static final String VOICE_ARRIVAL = "Arriving at %s";
    public static final String VOICE_DEPARTURE_MINUTES = "Next stop %s, ETA %d minutes";
    public static final String VOICE_DEPARTURE_MINUTE = "Next stop %s, ETA one minute";

    public static final int NETWORK_ERROR_TOLERANCE_MS = 90000;    // mark network as unavailable when test failed for more than this time
    public static final int RECORDER_ERROR_TOLLERANCE_MS = 90000;    // mark recording as unrecoverable when no frame has been recorded for this time
    public static final int RECORDING_STOP_DELAY = 60000;  // do not try to restart if recording has stopped recently
    public static final int RECORDING_START_DELAY = 60000;  // do not try to check if recording is working before this much time has elapsed

    //####################################### below are global variables #######################################

    // statistics
    public static int frameSentSuccess = 0;
    public static int frameSentFailed = 0;
    public static int frameSkipped = 0;

    public static int locationSentMvideoSuccess = 0;
    public static int locationSentMvideoFailed = 0;
    public static int locationSentAPISuccess = 0;
    public static int locationSentAPIFailed = 0;
    public static int locationSentETASuccess = 0;
    public static int locationSentETAFailed = 0;

    public static int busIndex = busInfoList.length - 1;

    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss.S");

    //####################################### below are api url related #######################################

    public static final boolean usingLocalNetwork = false;
    //public static final boolean usingLocalNetwork = false;

    public static final boolean usingUTMAServer = true; // set to false to use FIU backup server

    public static final String urlLocalNetworkServer = "192.168.0.82/";
    public static final String urlUTMAMvideoServer = "utma-video.cs.fiu.edu/";
    public static final String urlFIUMvideoServer = "131.94.133.214/";

    public static final String urlRemoteServer = usingUTMAServer ? urlUTMAMvideoServer : urlFIUMvideoServer;

    public static final String urlMvideoServer = usingLocalNetwork ? urlLocalNetworkServer : urlRemoteServer;

    public static final String urlMvideoLocationUpdate = "http://" + urlMvideoServer + "api/tracks/";

    public static final String urlMvideoStream = "rtmp://" + urlMvideoServer + "live/";

    public static final String recorderToken = "128349a35c6d151f3a3fb367752788cde7e17866";

    // send video stream to this url
    public static String urlVideoStream(String uuid, String busName) { return urlMvideoStream + uuid + "__" + busName + "__" + "?recorder_token=" + recorderToken; }

    // during video recording, send location to mvideo server so that archived videos has gps information
    // param: json with lat/lon fields
    public static String urlUpdateLocationMvideo (String uuid) { return urlMvideoLocationUpdate + uuid + "/update_location/"; }

    public static final String urlTransitApiFIU = "http://transit.cs.fiu.edu/api/";
    public static final String urlTransitApiLocalNetwork = "http://192.168.0.105/";

    public static final String urlTransitApi = usingLocalNetwork ? urlTransitApiLocalNetwork : urlTransitApiFIU;

    // when app is running, send location to api server so that bus location can be shown on ITPA IOC/app maps
    public static String urlUpdateLocationAPI(Date currentDate, String busId, double lat, double lon, float speed, float bearing) {
        String timeString = apiDateFormat.format(currentDate);
        return urlTransitApi + "v1/transit/settrack?busid=" + busId
                + "&lat=" + lat
                + "&lon=" + lon
                + "&date=" + timeString
                + "&altitude=0&heading=" + bearing
                + "&speed=" + speed
                + "&token=" + recorderToken;
    }

    // when app is running, send location to ETA server to get ETA data.
    public static String urlUpdateLocationETA(Date currentDate, String busId, double lat, double lon, float speed, float bearing, float accuracy) {
        return "http://utma.cs.fiu.edu/buseta/updatetrack.aspx?bus_id=" + busId
                + "&lat=" + lat
                + "&lon=" + lon
                + "&speed=" + speed
                + "&bearing=" + bearing
                + "&accuracy=" + accuracy
                + "&bus_time=" + (currentDate.getTime() / 1000L);
    }

    public static String urlAddLog = "http://utma.cs.fiu.edu/buseta/addlog.aspx";

    static {
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }
}