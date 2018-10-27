package com.terrafly.mvideo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.common.api.GoogleApiClient;

import okhttp3.MediaType;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//Sensor Stuf
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.ExifInterface;

import static android.content.ContentValues.TAG;
import static java.util.UUID.randomUUID;

public class MainActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 0;
    final Handler h = new Handler();
    final int delay = 10000; //milliseconds
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final static String CLASS_LABEL = "MainActivity";
    private final static String LOG_TAG = CLASS_LABEL;


    //for updating the location
    boolean updateLocation = false;

    private String ffmpeg_link_prefix = "rtmp://131.94.133.214:1935/live/";
    private String streamName = "";
    private String ffmpeg_link = "";
    private String  apiPrefix= "http://131.94.133.214/api/";
    private String api_loc_link = "";
    private String api_trac_link = "http://131.94.133.214/api/tracks/";

    long startTime = 0;
    boolean recording = false;

    private FFmpegFrameRecorder recorder;
    private FFmpegFrameFilter filter;

    private boolean isPreviewOn = false;

    private int imageWidth = 320;
    private int imageHeight = 240;
    private int frameRate = 30;


    /* video data getting thread */
    private Camera cameraDevice;
    private CameraView cameraView;

    private Frame yuvImage = null;

    /* layout setting */
    private final int bg_screen_bx = 162;
    private final int bg_screen_by = 128;
    private final int bg_screen_width = 950;
    private final int bg_screen_height = 900;
    private final int bg_width = 1123;
    private final int bg_height = 1215;
    private final int live_width = 1280;
    private final int live_height = 720;
    private int screenWidth, screenHeight;
    private Button btnRecorderControl;
    private Button btnSettingsControl;
    private android.app.FragmentManager fragmentManager = getFragmentManager();
    private ConcurrentLinkedQueue<Location> LocationUpdateQueue;
    private boolean locationToggle;//used to determine if location updates should be on/off

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = 0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private ProgressDialog progress;

    OkHttpClient client = new OkHttpClient();
    private boolean addFilter = false;
    private String filterString;

    //Sensor stuff

    Sensor accelerometer;
    Sensor magnetometer;
    SensorManager mSensorManager;
    Sensor vectorSensor;
    DeviceOrientation deviceOrientation;

    //Device Orientation
    private final int ORIENTATION_PORTRAIT = ExifInterface.ORIENTATION_ROTATE_90; // 6
    private final int ORIENTATION_LANDSCAPE_REVERSE = ExifInterface.ORIENTATION_ROTATE_180; // 3
    private final int ORIENTATION_LANDSCAPE = ExifInterface.ORIENTATION_NORMAL; // 1
    private final int ORIENTATION_PORTRAIT_REVERSE = ExifInterface.ORIENTATION_ROTATE_270; // 8

    public static boolean hasPermissions(Context context, String... permissions) {
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
    }
    return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //for newer devices, adds a permission dialog box for the permissions needed
        int PERMISSION_ALL = 1;

        //These permissions are protected and therefore need user permission
        String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        LocationUpdateQueue = new ConcurrentLinkedQueue<>();
        locationToggle = true;

        setContentView(R.layout.main);
        initLayout();

        locationUpdater.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recording = false;

        if (cameraView != null) {
            cameraView.stopPreview();
        }

        if (cameraDevice != null) {
            cameraDevice.stopPreview();
            cameraDevice.release();
            cameraDevice = null;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(deviceOrientation.getEventListener());

        if(recording) {
            stopRecording();
            recording=false;
        }

        if (cameraView != null) {
            cameraView.stopPreview();
        }



    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);
        initLayout();

    }


    public interface CameraSupport {
        CameraSupport open (int cameraID);
    }



    private void initLayout() {

        /* get size of screen */
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        RelativeLayout.LayoutParams layoutParam = null;
        LayoutInflater myInflate = null;
        myInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout topLayout = new RelativeLayout(this);
        setContentView(topLayout);
        LinearLayout preViewLayout = (LinearLayout) myInflate.inflate(R.layout.main, null);
        layoutParam = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
        topLayout.addView(preViewLayout, layoutParam);

        /* add control button: start and stop */
        btnRecorderControl = (Button) findViewById(R.id.recorder_control);
        btnRecorderControl.setText("Start");



        /* add camera view */
        int display_width_d = (int) (1.0 * bg_screen_width * screenWidth / bg_width);
        int display_height_d = (int) (1.0 * bg_screen_height * screenHeight / bg_height);
        int prev_rw, prev_rh;
        if (1.0 * display_width_d / display_height_d > 1.0 * live_width / live_height) {
            prev_rh = display_height_d;
            prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
        } else {
            prev_rw = display_width_d;
            prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
        }
        layoutParam = new RelativeLayout.LayoutParams(prev_rw, prev_rh);
        layoutParam.topMargin = (int) (1.0 * bg_screen_by * screenHeight / bg_height);
        layoutParam.leftMargin = (int) (1.0 * bg_screen_bx * screenWidth / bg_width);


        try {
            cameraDevice = Camera.open();
        }catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(LOG_TAG, "cameara open");
        cameraView = new CameraView(this, cameraDevice);
        topLayout.addView(cameraView, layoutParam);
        Log.i(LOG_TAG, "cameara preview start: OK");
    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    @SuppressLint("NewApi")
    private void initRecorder() {

        Log.w(LOG_TAG, "init recorder");


        if (RECORD_LENGTH > 0) {
            imagesIndex = 0;
            images = new Frame[RECORD_LENGTH * frameRate];
            timestamps = new long[images.length];
            for (int i = 0; i < images.length; i++) {
                images[i] = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
                timestamps[i] = -1;
            }
        } else if (yuvImage == null) {
            yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
            Log.i(LOG_TAG, "create yuvImage");
        }


        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
        recorder.setFormat("flv");
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);
        Log.v("Build Model", "The build Model is " + Build.MODEL);


        int rotation = deviceOrientation.getOrientation();
        Log.d(TAG, "initRecorder: Adding rotation into equation"+rotation);

        if(Objects.equals(Build.MODEL, "Nexus 5X")) {

            Log.v("Build Model", "Made it inside of build check");
            switch (rotation) {
                case ORIENTATION_PORTRAIT:
                    addFilter=true;
                    filterString = "transpose=2";
                    Log.v("Screen", "orientation is portrait " + filterString + Build.MODEL);
                    break;
                case ORIENTATION_PORTRAIT_REVERSE:
                    addFilter = true;
                    filterString = "transpose=1";
                    Log.v("Screen", "orientation is reversed portrait " + filterString + Build.MODEL);
                    break;

                case ORIENTATION_LANDSCAPE_REVERSE:
                    filterString = "transpose=1, transpose 1";
                    Log.v("Screen", "orientation is reversed landscape " + filterString + Build.MODEL);
                    addFilter = false;
                    break;
                default:
                    addFilter = true;
                    filterString = "transpose=1, transpose=1";
                    Log.v("Screen", "orientation is landscape " + filterString + Build.MODEL);

                    break;
            }
        }
        else {
            addFilter=false;
            filterString = "transpose=1, transpose=1";
            Log.v("Screen", "Not adding a rotation filter");


        }
        // Filter information
        filter = new FFmpegFrameFilter(filterString, imageWidth, imageHeight);

        //default format on android
        filter.setPixelFormat(avutil.AV_PIX_FMT_NV21);



        Log.i(LOG_TAG, "recorder initialize success");

    }

    @SuppressLint("NewApi")
    public void startRecording() {
        initRecorder();

        Log.i(LOG_TAG, "Started recorder");

        try {
            Log.i("Locations", "Made it inside trying to record");
            recorder.start();
            Log.i("Locations", "Inside and starting recorder");
            startTime = System.currentTimeMillis();
            recording = true;
            Log.i(LOG_TAG, "Started recoreder");

            filter.start();


        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }


    public void stopRecording() {

        if (recorder != null && recording) {
            if (RECORD_LENGTH > 0) {
                Log.v(LOG_TAG, "Writing frames");
                try {
                    int firstIndex = imagesIndex % samples.length;
                    int lastIndex = (imagesIndex - 1) % images.length;
                    if (imagesIndex <= images.length) {
                        firstIndex = 0;
                        lastIndex = imagesIndex - 1;
                    }
                    if ((startTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L) < 0) {
                        startTime = 0;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += images.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        long t = timestamps[i % timestamps.length] - startTime;
                        if (t >= 0) {
                            if (t > recorder.getTimestamp()) {
                                recorder.setTimestamp(t);
                            }
                            recorder.record(images[i % images.length]);
                        }
                    }

                    firstIndex = samplesIndex % samples.length;
                    lastIndex = (samplesIndex - 1) % samples.length;
                    if (samplesIndex <= samples.length) {
                        firstIndex = 0;
                        lastIndex = samplesIndex - 1;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += samples.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        recorder.recordSamples(samples[i % samples.length]);
                    }
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
            }

            recording = false;
            Log.v(LOG_TAG, "Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }

        btnRecorderControl.setText("Stop");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording();
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }



    //---------------------------------------------
    // camera thread, gets and encodes video data
    //---------------------------------------------
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

        private SurfaceHolder mHolder;
        private Camera mCamera;


        public CameraView(Context context, Camera camera) {
            super(context);
            Log.w("camera", "camera view");
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewCallback(CameraView.this);

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                stopPreview();
                mCamera.setPreviewDisplay(holder);
            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPreview();

            Camera.Parameters camParams = mCamera.getParameters();
            List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
            // Sort the list in ascending order
            Collections.sort(sizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = 0; i < sizes.size(); i++) {
                if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                    imageWidth = sizes.get(i).width;
                    imageHeight = sizes.get(i).height;
                    Log.v(LOG_TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                    break;
                }
            }
            camParams.setPreviewSize(imageWidth, imageHeight);

            Log.v(LOG_TAG, "Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

            camParams.setPreviewFrameRate(frameRate);
            Log.v(LOG_TAG, "Preview Framerate: " + camParams.getPreviewFrameRate());

            mCamera.setParameters(camParams);

            // Set the holder (which might have changed) again
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(CameraView.this);
                startPreview();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not set preview display in surfaceChanged");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mHolder.addCallback(null);
                mCamera.setPreviewCallback(null);
            } catch (RuntimeException e) {
                // The camera has probably just been released, ignore.
            }
        }

        public void startPreview() {
            if (!isPreviewOn && mCamera != null) {
                isPreviewOn = true;
                mCamera.startPreview();
            }
        }

        public void stopPreview() {
            if (isPreviewOn && mCamera != null) {
                isPreviewOn = false;
                mCamera.stopPreview();
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (RECORD_LENGTH > 0) {
                int i = imagesIndex++ % images.length;
                yuvImage = images[i];
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
            }


            /* get video data */
            if (yuvImage != null && recording) {
                ((ByteBuffer) yuvImage.image[0].position(0)).put(data);

                if (RECORD_LENGTH <= 0) try {
                    Log.v(LOG_TAG, "Writing Frame");
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    if(addFilter) {
                        filter.push(yuvImage);
                        Frame frame2;
                        while ((frame2 = filter.pull()) != null) {
                            recorder.record(frame2);
                        }
                    } else {
                        recorder.record(yuvImage);
                    }
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                } catch (FrameFilter.Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SendLocationTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... arg0) {

            Log.i("locations", "Inside of background task");
            String url = arg0[0];
            String lat = arg0[1];
            String lng = arg0[2];

            String response = "";
            String gpsJson = null;
            try {
                gpsJson = gpsJson(lat, lng);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                response = post(url, gpsJson);
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.e(TAG, "doInBackground: Sending uptodates to\n url: " + url);
            Log.i(TAG, "doInBackground: Response is " + response);

            return null;
        }

    }

    private class SendInitialUuid extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... arg0) {

            Log.i("Locations", "Sending Initial track");
            String url = arg0[0];
            String json = arg0[1];

            String response = "";
            try {
                response = post(url, json);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "doInBackground: Sending initial update to\n url: " + url);
            Log.i(TAG, "doInBackground: Response is " + response);
            return null;
        }

    }

    public void recordAction(View v) throws JSONException, IOException {

        WindowManager.LayoutParams lparams = getWindow().getAttributes();

        String uuid = randomUUID().toString();

        streamName = uuid;
        ffmpeg_link = ffmpeg_link_prefix + streamName + "__demo2__";
        api_loc_link = apiPrefix + "tracks/" + uuid +"/update_location/";

        Log.i("STREAM NAME: ", ffmpeg_link);

        if (!recording) {
            lparams.screenBrightness=0.3f;
            getWindow().setAttributes(lparams);
            startRecording();
            toggleTracking();
            JSONObject jsonObject = new JSONObject();
            String streamer = "";
            jsonObject.put("uuid", streamName);
            streamer = jsonObject.toString();

            new SendInitialUuid().execute(api_trac_link,
                    streamer, null
            );
            Log.i("Locations", "Trying to toggle tracking");
            Log.w(LOG_TAG, "Start Button Pushed");
            btnRecorderControl.setText("Stop");
            btnRecorderControl.setTextColor(Color.RED);
        } else {
            lparams.screenBrightness=1;
            getWindow().setAttributes(lparams);stopRecording();
            toggleTracking();
            Log.i("Locations", "Trying to toggle tracking");
            Log.w(LOG_TAG, "Stop Button Pushed");
            btnRecorderControl.setTextColor(Color.BLACK);
            btnRecorderControl.setText("Start");
        }

    }

    public void fireMissiles(View view) {
        SettingsDialogFragment fmd = new SettingsDialogFragment();
        fmd.show(fragmentManager, "Fire Missiles");
    }


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {

            LocationUpdateQueue.add(location);
            Log.i("Locations", "Locations changed");

        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    };

    private final LocationListener currentLocationListener = new LocationListener() {


        @Override
        public void onLocationChanged(Location location) throws SecurityException {
            // TODO Auto-generated method stub
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            LocationUpdateQueue.add(location);
            mLocationManager.removeUpdates(this);

            Log.i("Locations", location.toString());


            toggleLocationUpdates(true);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

    };


    public void toggleTracking() throws SecurityException{
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //used to get current location once, handling of location is then transferred to a different
        //LocationListener


        if (recording) {
            updateLocation = true;
            progress = ProgressDialog.show(this, "loading", "Getting current location");

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, currentLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
                    0 , currentLocationListener);
            toggleLocationUpdates(true);
        }
        else
        {
            toggleLocationUpdates(false);
        }


    }

    private Runnable locationUpdater = new Runnable(){

        public void run() throws SecurityException{
            if(updateLocation)
            {
                //send most accurate location
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String sName = mPrefs.getString("stream_name", null);

                // location.getAccuracy()

                Location[] locs = LocationUpdateQueue.toArray(new Location[0]);
                LocationUpdateQueue.clear();

                Location location = null;
                if (locs.length == 0)//no new updates
                {

                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Location tempLocGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location tempLocNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.i("Locations", "Got inside of location");

                    if (tempLocGPS == null)
                        location = tempLocNetwork;
                    else if (tempLocNetwork == null)
                        location = tempLocGPS;
                    else if (tempLocGPS.getAccuracy() < tempLocNetwork.getAccuracy())
                        location = tempLocGPS;
                    else
                        location = tempLocNetwork;

                }
                else    //get updateLocation with best accuracy
                {
                    for(Location l : locs)
                    {
                        if (location == null)
                            location = l;
                        if (location.getAccuracy() > l.getAccuracy())
                            location = l;
                    }
                }

                if (location != null)
                {
                    Log.e("Locations", ffmpeg_link);
                    new SendLocationTask().execute(api_loc_link,
                            String.valueOf(location.getLatitude()),
                            String.valueOf(location.getLongitude())
                    );
                }
            }

            h.postDelayed(locationUpdater, delay);
        }
    };

    //turns on/off location updates
    public void toggleLocationUpdates(boolean toggle) throws SecurityException{


        LocationManager  mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        if (toggle)
        {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                    0 , mLocationListener);


            updateLocation = true;


            progress.dismiss();
        }
        else
        {
            mLocationManager.removeUpdates(mLocationListener);
            updateLocation = false;
        }
    }

    @SuppressLint("NewApi")
    String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    String gpsJson(String lat, String lng) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", getISO8601StringForCurrentDate());
        jsonObject.put("longitude", lng);
        jsonObject.put("latitude", lat);
        return jsonObject.toString();
    }

    /**
     * Return an ISO 8601 combined date and time string for current date/time
     *
     * @return String with format "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    public static String getISO8601StringForCurrentDate() {
        Date now = new Date();
        return getISO8601StringForDate(now);
    }

    /**
     * Return an ISO 8601 combined date and time string for specified date/time
     *
     * @param date
     *            Date
     * @return String with format "yyyy-MM-dd'T'HH:mm:ss'Z'"
     */
    private static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }
}


