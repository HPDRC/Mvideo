package com.itpa.mvideo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.itpa.mvideo.misc.Config;
import com.itpa.mvideo.misc.RecordingMonitor;
import com.itpa.mvideo.misc.SntpTime;
import com.itpa.mvideo.misc.Util;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.UUID.randomUUID;

@SuppressWarnings("deprecation")
@SuppressLint("ViewConstructor")
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public String uuid;
    public boolean isRecording = false;
    public boolean isViewAddedToViewGroup = false;
    public AtomicBoolean isCameraBeingOpened = new AtomicBoolean(false);
    public AtomicBoolean isCameraStoppedDuringOpen = new AtomicBoolean(false);

    // camera related
    private Camera mCamera = null;
    private boolean isPreviewOn = false;
    private HandlerThread mCameraThread = null;
    private Handler mCameraHandler = null;
    private AtomicBoolean isVideoDataUploading = new AtomicBoolean(false);
    private static int imageWidth = 320;
    private static int imageHeight = 240;

    // recorder related
    private Frame yuvImage = null;
    private FFmpegFrameRecorder frameRecorder = null;
    private long recorderStartTime = 0;


    public CameraView(Context context) {
        super(context);
        uuid = randomUUID().toString();
        //uuid="70eb2163-7f36-45de-90b2-51879463fcea";
        Util.sendLogToServer(getContext(), "CameraView: starting");
        openCameraThread();
    }

    // stop recorder, stop camera
    public void stop() {
        if (isCameraBeingOpened.get()) {
            isCameraStoppedDuringOpen.set(true);
            Log.d(Config.TAG, "CameraView: camera is stopped during open");
        } else {
            isRecording = false;
            if (mCamera != null) {
                stopPreview();
                try {
                    if (mCameraThread != null) {
                        mCameraThread.quit();
                    }
                    mCamera.release();
                } catch (Exception ex) {
                    Util.sendLogToServer(getContext(), "CameraView: failed to release camera", ex);
                } finally {
                    mCamera = null;
                }
            }
        }
        /*
         * Liangdong: when WIFI is available but there is no internet connection, frameRecorder will freeze at stop().
         * My best guess is stop() will send data to server, indicating the streaming has done. It will wait for server's response probably for 30 seconds or so
        try {
            if (frameRecorder != null) {
                frameRecorder.stop();
            }
        } catch (Exception ex) {
            Log.e(Config.TAG, "CameraView: failed to stop recorder", ex);
        } */
        frameRecorder = null;
        yuvImage = null;
        Util.sendLogToServer(getContext(), "CameraView: stopped");
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isRecording && frameRecorder != null) {
            if (isVideoDataUploading.compareAndSet(false, true)) {
                try {
                    long newTs = 1000 * (SntpTime.currentTimeMillis() - recorderStartTime);
                    long curTs = frameRecorder.getTimestamp();
                    if (newTs > curTs) { frameRecorder.setTimestamp(newTs); }
                    ((ByteBuffer)yuvImage.image[0].position(0)).put(data);
                    frameRecorder.record(yuvImage);
                    Config.frameSentSuccess++;
                    RecordingMonitor.setRecorderSuccess();
                } catch (Exception e) {
                    Config.frameSentFailed++;
                    if (Config.frameSentFailed % 100 == 0)
                        Log.d(Config.TAG, e.getMessage());
                }
                finally{
                    isVideoDataUploading.set(false);
                }
            } else {
                Config.frameSkipped++;
            }
        }
    }

    private void closeCamera() {
        if(mCamera != null) {
            stopPreview();
            try {
                mCamera.release();
            }
            catch (Exception ex) { Util.sendLogToServer(getContext(), "CameraView: failed to release camera", ex); }
            finally {
                mCamera = null;
            }
        }
    }

    // open camera, send initial uuid to mvideo server, start recording
    private void openCameraThread() {
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread("cameraThread");
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
        }
        isCameraBeingOpened.set(true);
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                // open camera
                try {
                    mCamera = Camera.open(0);
                } catch (RuntimeException e) {
                    Util.sendLogToServer(getContext(), "CameraView: failed to open camera");
                    isCameraBeingOpened.set(false);
                    return;
                }

                try {
                    SurfaceHolder mHolder = getHolder();
                    mHolder.addCallback(CameraView.this);
                    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                    setPreviewCallBack();
                } catch (Exception ex) {
                    Util.sendLogToServer(getContext(), "CameraView: failed to create mHolder");
                    isCameraBeingOpened.set(false);
                    closeCamera();
                    return;
                }

                isCameraBeingOpened.set(false);

                // check if stop is called during open
                if (isCameraStoppedDuringOpen.get()) {
                    Log.d(Config.TAG, "CameraView: camera is closed after being opened");
                    closeCamera();
                    return;
                }

                try {
                    yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
                    frameRecorder = new FFmpegFrameRecorder(Config.urlVideoStream(uuid, Config.busInfoList[Config.busIndex].name), imageWidth, imageHeight, 1);
                    frameRecorder.setInterleaved(false);
                    frameRecorder.setVideoOption("preset", "superfast");
                    frameRecorder.setVideoOption("tune", "zerolatency");
                    frameRecorder.setVideoBitrate(Config.BIT_RATE);
                    frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    frameRecorder.setFormat("flv");
                    frameRecorder.setFrameRate(Config.FRAME_RATE);
                    frameRecorder.setGopSize(Config.GOP_SIZE);
                    recorderStartTime = SntpTime.currentTimeMillis();
                    frameRecorder.start();
                    startPreview();
                    isRecording = true;
                    Util.sendLogToServer(getContext(), "CameraView: recording started");
                } catch (Exception ex) {
                    Util.sendLogToServer(getContext(), "CameraView: start recorder failed", ex);
                }
            }
        });
    }

    private void setPreviewCallBack() { mCamera.setPreviewCallback(this); }

    private void startPreview() {
        if (!isPreviewOn && mCamera != null) {
            isPreviewOn = true;
            mCamera.startPreview();
            Log.d(Config.TAG, "CameraView: preview started");
        }
    }

    private void stopPreview() {
        if (isPreviewOn && mCamera != null) {
            isPreviewOn = false;
            mCamera.stopPreview();
            Log.d(Config.TAG, "CameraView: preview stopped");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            stopPreview();
            mCamera.setPreviewDisplay(holder);
            Log.d(Config.TAG, "CameraView: surface is created");
        } catch (IOException ex) {
            Util.sendLogToServer(getContext(), "CameraView: surface failed to create", ex);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(Config.TAG, "CameraView: surface changed");
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
                break;
            }
        }
        camParams.setPreviewSize(imageWidth, imageHeight);
        camParams.setPreviewFrameRate(Config.FRAME_RATE);
        mCamera.setParameters(camParams);

        // Set the holder (which might have changed) again
        try {
            mCamera.setPreviewDisplay(holder);
            setPreviewCallBack();
            startPreview();
        } catch (Exception e) {
            Log.d(Config.TAG, "Could not set preview display in surfaceChanged");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        Log.d(Config.TAG, "CameraView: surface is destroyed");
    }
}