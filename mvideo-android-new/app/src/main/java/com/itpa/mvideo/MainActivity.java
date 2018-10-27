package com.itpa.mvideo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.itpa.mvideo.misc.Config;
import com.itpa.mvideo.misc.DbHelper;
import com.itpa.mvideo.misc.RecordingMonitor;
import com.itpa.mvideo.misc.Pinpad;
import com.itpa.mvideo.misc.SntpTime;
import com.itpa.mvideo.misc.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;

public class MainActivity extends BaseActivity {

    View vgManage;
    View vgPinpad;
    View vgBusList;
    LinearLayout vgVideo;
    View layoutTop;
    RadioGroup rgBusList;
    TextView tvBody;
    TextView tvStatus;
    Pinpad pinpad;

    private boolean isInitialized = false;

    private TextToSpeech textToSpeech;
    private CameraView cameraView = null;

    private Location mCurrentLocation = null;
    private Date mCurrentDate = null;

    private long mLastTouchTime = 0;
    private boolean mIsLocationInitialized = false;
    private long mLastRecordStopTime = 0;
    private long mLastRecordStartTime = 0;

    private AtomicBoolean mIsSendingLocationToMvideo = new AtomicBoolean(false);
    private AtomicBoolean mIsSendingLocationToAPI = new AtomicBoolean(false);
    private AtomicBoolean mIsSendingLocationToETA = new AtomicBoolean(false);
    private AtomicBoolean mIsSendingAppStatus = new AtomicBoolean(false);

    private long mLastTestNetworkConnectivity = 0;
    private long mLastSendAppStatus = 0;
    private long mLastSendToMvideo = 0;
    private long mLastSendToAPI = 0;
    private long mLastSendToETA = 0;
    private long mLastUpdateETA = 0;

    private int eta = -1;
    private String nextStop = "";
    private boolean isAtStop = false;
    private boolean sayArriving = false;
    private boolean sayLeaving = false;
    private String arriveLeaveStop = "";
    private int leaveETA = -1;

    private String tvBodyFormat = "";

    protected void adjustAudio() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        }
    }

    SimpleDateFormat uiDateFormat;

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adjustAudio();

        uiDateFormat = new SimpleDateFormat("HH:mm");
        uiDateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

        vgManage = findViewById(R.id.vgManage);
        vgPinpad = findViewById(R.id.vgPinpad);
        vgBusList = findViewById(R.id.vgBusList);
        vgVideo = findViewById(R.id.vgVideo);
        layoutTop = findViewById(R.id.layoutManage);
        rgBusList = findViewById(R.id.rgBusList);
        tvBody = findViewById(R.id.tvBody);
        tvStatus = findViewById(R.id.tvStatus);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setSpeechRate(Config.SPEECH_RATE);
                } else {
                    Util.sendLogToServer(getApplicationContext(), "MainActivity: TextToSpeech init failed");
                    textToSpeech = null;
                }
            }
        });

        loadDatabase();
        initPinPad();
        initBusList();
        Util.sendLogToServer(this, "MainActivity: onCreate is called");
    }

    private void setUIStyle() {
        int style = Config.busInfoList[Config.busIndex].uiStyle;
        TextView tvTitle = findViewById(R.id.tvTitle);
        String titleText0 = "Welcome to the FIU CATS Shuttle";
        String titleText1 = "Welcome to the Sweetwater Trolley<br><font color=\"#C89800\">Bienvenidos a el Trolley de la Ciudad de Sweetwater</font>";
        String bodyFormat0 = "Next Stop: <font color=\"#FF0000\">%s</font><br>ETA: <font color=\"#FF0000\">%s</font><br>"
            + "Time: <font color=\"#FF0000\">%s</font><br>WiFi: <font color=\"#FF0000\">%s</font>";
        String bodyFormat1 = "Next Stop / <font color=\"#C89800\">Proxima Parada</font>:<br> &nbsp; <font color=\"#FF0000\"><small>%s</small></font><br>"
            + "ETA / <font color=\"#C89800\">Estimada llegada</font>: <font color=\"#FF0000\">%s</font><br>"
            + "Time / <font color=\"#C89800\">Hora</font>: <font color=\"#FF0000\">%s</font><br>"
            + "WiFi / <font color=\"#C89800\">Red inalámbrica</font>: <font color=\"#FF0000\">%s</font>";
        tvTitle.setText(Html.fromHtml(style == 0 ? titleText0 : titleText1));
        tvBodyFormat = style == 0 ? bodyFormat0 : bodyFormat1;
    }

    private void initAll() {
        if (isInitialized)
            return;
        if (!checkPermissions())
            return;
        initSntp();
        initLocation();
        isInitialized = true;
        Log.d(Config.TAG, "MainActivity.initAll finished");
    }

    private void updateLastTouchTimeToNow() {
        mLastTouchTime = SntpTime.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLastTouchTimeToNow();
        showManageUI(true);
        initAll();
        mUpdateRunnable.run();
        Util.sendLogToServer(this, "MainActivity: onResume is called");
    }

    protected void onPause() {
        super.onPause();
        Util.sendLogToServer(this, "MainActivity: onPause is called");
        stopRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.sendLogToServer(this, "MainActivity: onDestroy is called");
        try { SmartLocation.with(this).location().stop(); }
        catch (Exception ex) { Util.sendLogToServer(this, "MainActivity: SmartLocation failed to stop", ex); }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        updateLastTouchTimeToNow();
        showManageUI(true);
        return super.dispatchTouchEvent(ev);
    }

    private void showManageUI(boolean isShow) {
        layoutTop.setVisibility(isShow ? View.VISIBLE : View.GONE);
        vgVideo.setVisibility(isShow ? View.VISIBLE : View.GONE);
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = isShow ? Config.SCREEN_BRIGHTNESS_ACTIVE : Config.SCREEN_BRIGHTNESS_STANDBY;
        getWindow().setAttributes(layoutParams);
    }

    public void onClickManage(View view) {
        vgManage.setVisibility(View.VISIBLE);
        vgPinpad.setVisibility(View.VISIBLE);
        vgBusList.setVisibility(View.GONE);
        pinpad.clear();
        initBusList();
    }

    public void onClickExit(View view) {
        vgManage.setVisibility(View.GONE);
    }

    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            // add cameraView to UI
            if (cameraView != null && cameraView.isRecording && !cameraView.isViewAddedToViewGroup) {
                cameraView.isViewAddedToViewGroup = true;
                vgVideo.addView(cameraView);
                Log.d(Config.TAG, "MainActivity: cameraView is added to viewGroup");
            }

            // send app status
            long timeNow = SntpTime.currentTimeMillis();
            if (timeNow - mLastSendAppStatus > Config.SEND_APP_STATUS_MS &&
                    mIsSendingAppStatus.compareAndSet(false, true)) {
                mLastSendAppStatus = timeNow;
                sendAppStatus();
            }

            // test network
            if (timeNow - mLastTestNetworkConnectivity > Config.TEST_NETWORK_CONNECTIVITY_MS) {
                mLastTestNetworkConnectivity = timeNow;
                testNetworkConnectivity();
            }

            checkVideoStreaming();

            // UI related
            updateUI();
            if (SntpTime.currentTimeMillis() - mLastTouchTime > Config.MAIN_MANAGE_AUTO_HIDE_MS) {
                showManageUI(false);
            }
            mHandler.postDelayed(mUpdateRunnable, Config.MAIN_UPDATE_MS);
        }
    };

    private void checkVideoStreaming() {
        if (!isInitialized)
            return;

        long timeNow = SntpTime.currentTimeMillis();
        // when camera is not running, check network, start camera if network is available
        if (cameraView == null && timeNow - mLastRecordStopTime >= Config.RECORDING_STOP_DELAY) {
            if (RecordingMonitor.isNetworkAvailable()) {
                Util.sendLogToServer(this, "Start recording because network is available");
                startRecording();
            }
        }

        if (cameraView != null && timeNow - mLastRecordStartTime >= Config.RECORDING_START_DELAY) {
            if (!RecordingMonitor.isRecordingSuccessful()) {
                Util.sendLogToServer(this, "Stop recording because no frame is recorded");
                stopRecording();
            }
        }
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    private void updateUI() {
        String timeString = uiDateFormat.format(SntpTime.now());
        Location location = mCurrentLocation;
        tvBody.setText(Html.fromHtml(String.format(tvBodyFormat, nextStop, (isAtStop ? "" : Util.secondsToString(eta)), timeString, Config.busInfoList[Config.busIndex].wifiName)));
        tvStatus.setText(String.format("%s/%s, %s%.4f/%.4f, acc=%.2f, speed=%.2f, bear=%.0f, eta=%d, atStop=%d\nframe ok/fail/skip: %d/%d/%d; mvideo/api/eta succeed/fail: %d/%d, %d/%d, %d/%d",
                Config.busInfoList[Config.busIndex].name, Config.busInfoList[Config.busIndex].id, (Config.isTestMode ? "test, " : ""),
                (location == null ? 0.0f : location.getLatitude()), (location == null ? 0.0f : location.getLongitude()),
                (location == null ? 0.0f : location.getAccuracy()), (location == null ? 0.0f : location.getSpeed()),
                (location == null ? 0.0f : location.getBearing()), eta, (isAtStop ? 1 : 0),
                Config.frameSentSuccess, Config.frameSentFailed, Config.frameSkipped,
                Config.locationSentMvideoSuccess, Config.locationSentMvideoFailed,
                Config.locationSentAPISuccess, Config.locationSentAPIFailed,
                Config.locationSentETASuccess, Config.locationSentETAFailed));
        if (sayArriving && !arriveLeaveStop.equals("")) {
            speak(String.format(Config.VOICE_ARRIVAL, arriveLeaveStop));
            sayArriving = false;
            arriveLeaveStop = "";
        } else if (sayLeaving && !arriveLeaveStop.equals("")) {
            int nextStopMinutes = Util.secondsToMinutes(leaveETA);
            if (nextStopMinutes == 1)
                speak(String.format(Config.VOICE_DEPARTURE_MINUTE, arriveLeaveStop));
            else
                speak(String.format(Config.VOICE_DEPARTURE_MINUTES, arriveLeaveStop, nextStopMinutes));
            sayLeaving = false;
            arriveLeaveStop = "";
        }
    }

    private void speak(String message) {
        if (textToSpeech != null)
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void initLocation() {
        if (mIsLocationInitialized)
            return;
        mIsLocationInitialized = true;
        SmartLocation.with(this).location().config(LocationParams.NAVIGATION).start(new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                mCurrentLocation = location;
                // mCurrentDate = SntpTime.now();
                // to improve sync of video position with vehicle location we need to use
                // the time when the gps location was actually acquired; hopefully the
                // gps location getTime() uses time from the gps source, and not
                // the device's own time.
                // See the discussion below and others like it:
                // https://stackoverflow.com/questions/10266175/how-to-get-accurate-time-stamps-from-android-gps-location?rq=1
                // 20181009: changed back to tablet time since location.getTime() is bugged
                //     see https://stackoverflow.com/questions/32377005/android-java-location-gettime-returns-utc-1-instead-of-utc-if-current-timezon
                mCurrentDate = SntpTime.now();
                long timeNow = mCurrentDate.getTime();
                if (Config.isTestMode) {
                    location.setLatitude(Config.testLatlon[Config.testCounter]);
                    location.setLongitude(Config.testLatlon[Config.testCounter + 1]);
                    location.setAccuracy(10);
                    if (timeNow - Config.testLastChangeLatlon > Config.testChangeLatLonInterval) {
                        Config.testLastChangeLatlon = timeNow;
                        Config.testCounter += 2;
                        if (Config.testCounter >= Config.testLatlon.length)
                            Config.testCounter = Config.testLatlon.length - 2;
                    }
                }
                if (cameraView != null && cameraView.isRecording && timeNow - mLastSendToMvideo > Config.SEND_LOCATION_TO_MVIDEO_MS &&
                        mIsSendingLocationToMvideo.compareAndSet(false, true)) {
                    mLastSendToMvideo = timeNow;
                    sendLocationToMvideo(mCurrentLocation, mCurrentDate, cameraView.uuid);
                }
                if (timeNow - mLastSendToAPI > Config.SEND_LOCATION_TO_API_MS &&
                        mIsSendingLocationToAPI.compareAndSet(false, true)) {
                    mLastSendToAPI = timeNow;
                    sendLocationToAPI(mCurrentLocation, mCurrentDate);
                }
                if (timeNow - mLastSendToETA > Config.SEND_LOCATION_TO_ETA_MS &&
                        mIsSendingLocationToETA.compareAndSet(false, true)) {
                    mLastSendToETA = timeNow;
                    sendLocationToETA(mCurrentLocation, mCurrentDate);
                }
            }
        });
    }

    private void testNetworkConnectivity() {
        Ion.with(getApplicationContext()).load(Config.urlAddLog).asString().setCallback(new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (e == null) {
                    RecordingMonitor.setNetworkSuccess();
                }
            }
        });
    }

    private void sendAppStatus() {
        try { Util.sendLogToServer(this, Util.getAppStatus(this)); }
        catch (Exception ex) {
            String message = ex.getMessage();
            if(message == null) { message = "null message"; }
            Log.e(Config.TAG,"MainActivity: failed to log to server: " + message);
        }
        finally { mIsSendingAppStatus.set(false); }
    }

    private void sendLocationToMvideo(Location location, Date currentDate, String uuid) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", Util.getISO8601StringForDate(currentDate));
        jsonObject.addProperty("latitude", String.valueOf(location.getLatitude()));
        jsonObject.addProperty("longitude", String.valueOf(location.getLongitude()));
        apiRequest(Config.urlUpdateLocationMvideo(uuid), jsonObject, Config.recorderToken, new FutureCallback<JsonObject> () {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
            if (e != null)
                Config.locationSentMvideoFailed++;
            else
                Config.locationSentMvideoSuccess++;
            mIsSendingLocationToMvideo.set(false);
            }
        });
    }

    private void sendLocationToAPI(Location location, Date currentDate) {
        if(Config.usingLocalNetwork) {
            mIsSendingLocationToAPI.set(false);
            return;
        }
        apiRequest(Config.urlUpdateLocationAPI(currentDate, Config.busInfoList[Config.busIndex].id, location.getLatitude(),
                location.getLongitude(), location.getSpeed(), location.getBearing()),
                null, new FutureCallback<JsonObject> () {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                if (e != null)
                    Config.locationSentAPIFailed++;
                else
                    Config.locationSentAPISuccess++;
                mIsSendingLocationToAPI.set(false);
            }
        });
    }

    private void sendLocationToETA(Location location, Date currentDate) {
        if(Config.usingLocalNetwork) {
            mIsSendingLocationToETA.set(false);
            return;
        }
        apiRequest(Config.urlUpdateLocationETA(currentDate, Config.busInfoList[Config.busIndex].id,
                location.getLatitude(), location.getLongitude(), location.getSpeed(), location.getBearing(), location.getAccuracy()),
        null, new FutureCallback<JsonObject> () {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                long currentMillis = SntpTime.currentTimeMillis();
                if (e != null || result.get("error").getAsInt() != 0) {
                    Config.locationSentETAFailed++;
                    if (currentMillis - mLastUpdateETA > Config.ETA_OUTDATE_MS) {
                        eta = -1;
                        nextStop = "";
                        isAtStop = false;
                    }
                } else {
                    mLastUpdateETA = currentMillis;
                    Config.locationSentETASuccess++;
                    eta = result.get("eta").getAsInt();
                    nextStop = result.get("nextStop").getAsString();
                    boolean atStop = result.get("isAtStop").getAsBoolean();
                    String prevStop = result.get("prevStop").getAsString();
                    if (atStop && !isAtStop) {
                        sayArriving = true;
                        arriveLeaveStop = prevStop;
                    }
                    if (!atStop && isAtStop) {
                        sayLeaving = true;
                        arriveLeaveStop = nextStop;
                        leaveETA = eta;
                    }
                    isAtStop = atStop;
                }
                mIsSendingLocationToETA.set(false);
            }
        });
    }

    private static class InitSntpTask extends AsyncTask<MainActivity, Void, Void> {
        protected Void doInBackground(MainActivity... params) {
            MainActivity mainActivity = params[0];
            try {
                if(!SntpTime.isInitialized()) {
                    SntpTime.build().withNtpHost("time.google.com").withConnectionTimeout(2_000).initialize();
                    if(SntpTime.isInitialized()) { mainActivity.updateLastTouchTimeToNow(); }
                    else { throw (new Exception("failed to initialize sntp")); }
                }
            } catch (Exception ex) {
                Util.sendLogToServer(mainActivity.getApplicationContext(), ex.getMessage(), ex);
            }
            return null;
        }
    }

    private void initSntp(){
        new InitSntpTask().execute(this);
    }

    private void startRecording() {
        // video container needs to be visible to trigger onPreviewFrame events.
        updateLastTouchTimeToNow();
        showManageUI(true);
        if (cameraView == null) {
            try {
                mLastRecordStartTime = SntpTime.currentTimeMillis();
                cameraView = new CameraView(this);
            } catch (Exception ex) {
                Util.sendLogToServer(getApplicationContext(), "MainActivity: cannot create cameraView", ex);
            }
        }
    }

    private void stopRecording() {
        if (cameraView != null) {
            try {
                cameraView.stop();
            } catch (Exception ex) {
                Util.sendLogToServer(this, "MainActivity: cameraView failed to stop", ex);
            }
            cameraView = null;
            mLastRecordStopTime = SntpTime.currentTimeMillis();
            vgVideo.removeAllViews();
        }
    }

    private void loadDatabase() {
        SQLiteDatabase db = new DbHelper(getBaseContext()).getReadableDatabase();
        String busIndexStr = DbHelper.getFromGeneral(db, DbHelper.keyBusIndex);
        db.close();
        if (busIndexStr != null) {
            try {
                int index = Integer.parseInt(busIndexStr);
                if (index < 0)
                    index = 0;
                index %= Config.busInfoList.length;
                Config.busIndex = index;
            } catch (Exception ex) {
                Config.busIndex = 0;
            }
        }
        setUIStyle();
    }

    private void initPinPad() {
        ArrayList<Button> pinButtons = new ArrayList<>();
        pinButtons.add((Button) findViewById(R.id.pinbtn0));
        pinButtons.add((Button) findViewById(R.id.pinbtn1));
        pinButtons.add((Button) findViewById(R.id.pinbtn2));
        pinButtons.add((Button) findViewById(R.id.pinbtn3));
        pinButtons.add((Button) findViewById(R.id.pinbtn4));
        pinButtons.add((Button) findViewById(R.id.pinbtn5));
        pinButtons.add((Button) findViewById(R.id.pinbtn6));
        pinButtons.add((Button) findViewById(R.id.pinbtn7));
        pinButtons.add((Button) findViewById(R.id.pinbtn8));
        pinButtons.add((Button) findViewById(R.id.pinbtn9));
        pinButtons.add((Button) findViewById(R.id.pinbtnback));

        ArrayList<TextView> pinBoxes = new ArrayList<>();
        pinBoxes.add((TextView) findViewById(R.id.pinbox0));
        pinBoxes.add((TextView) findViewById(R.id.pinbox1));
        pinBoxes.add((TextView) findViewById(R.id.pinbox2));
        pinBoxes.add((TextView) findViewById(R.id.pinbox3));

        TextView pinTitle = findViewById(R.id.pinpadTitle);
        pinTitle.setText(R.string.main_enter_password);
        pinpad = new Pinpad(pinButtons, pinBoxes, pinTitle, (Button) findViewById(R.id.pinbtnback), new Pinpad.PinpadCallback() {
            @Override
            public void onAllPinCompleted(String code) {
                if (code.equals(Config.PASS_CODE)) {
                    vgPinpad.setVisibility(View.GONE);
                    vgBusList.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void initBusList() {
        rgBusList.removeAllViews();
        for (int i = 0; i < Config.busInfoList.length; i++) {
            RadioButton btn = new RadioButton(this);
            btn.setId(10000 + i);
            btn.setText(Config.busInfoList[i].name + ", " + Config.busInfoList[i].id);
            if (i == Config.busIndex)
                btn.setChecked(true);
            btn.setTextSize(getResources().getDimension(R.dimen.textSizeSS));
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int id = (view.getId()) - 10000;
                    if (id >=0 && id < Config.busInfoList.length) {
                        try {
                            SQLiteDatabase db = new DbHelper(getBaseContext()).getWritableDatabase();
                            DbHelper.insertOrUpdateGeneral(db, DbHelper.keyBusIndex, String.valueOf(id));
                            db.close();
                        } catch (Exception ex) {
                            Util.sendLogToServer(getApplicationContext(), "database update failed", ex);
                        }
                        loadDatabase();
                    }
                }
            });
            rgBusList.addView(btn);
        }
    }

    void apiRequest(final String url, final JsonObject jsonObject, String token, FutureCallback<JsonObject> futureCallback) {
        Util.apiRequest(this, url, jsonObject, token, futureCallback);
    }
    void apiRequest(final String url, final JsonObject jsonObject, FutureCallback<JsonObject> futureCallback) {
        Util.apiRequest(this, url, jsonObject, futureCallback);
    }
}
