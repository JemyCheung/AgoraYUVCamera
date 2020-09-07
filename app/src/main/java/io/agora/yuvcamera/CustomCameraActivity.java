package io.agora.yuvcamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.IOException;
import java.util.List;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.AgoraVideoFrame;
import io.agora.rtc.video.BeautyOptions;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.yuvcamera.config.Config;
import io.agora.yuvcamera.utils.FileUtil;

public class CustomCameraActivity extends Activity implements CameraPreview.CameraPreviewCallback, Camera.PreviewCallback {
    private Camera mCamera;
    private RtcEngine mRtcEngine;
    private static final int PERMISSION_REQ_ID = 22;
    private SurfaceHolder holder;
    private String TAG = "zwtest";
    private SurfaceView mPreview;
    private static final int DEFAULT_CAPTURE_WIDTH = 1280;
    private static final int DEFAULT_CAPTURE_HEIGHT = 720;
    private SurfaceHolder mHolder;
    private boolean mPreviewing = false;
    // App 运行时确认麦克风和摄像头设备的使用权限。
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
        }
        setContentView(R.layout.custom_camera_activity);
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), Config.APPID, mRtcEventHandler);
            mRtcEngine.setChannelProfile(io.agora.rtc.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.setLogFile(FileUtil.initializeLogFile(this));
            mRtcEngine.setClientRole(1);
            mRtcEngine.setBeautyEffectOptions(true,
                    new BeautyOptions(1, 0.7f, 1f, 0.1f));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "RtcEngine.create error");
        }

        setupLoacalPreview();
        initmRtcEngine();
    }

    private void setupLoacalPreview() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                openCamera();
                mPreview = new CameraPreview(CustomCameraActivity.this, mCamera, CustomCameraActivity.this);
                RelativeLayout camera_preview = (RelativeLayout) findViewById(R.id.camera_preview);
                camera_preview.addView(mPreview);
            }
        });
    }

    private void initmRtcEngine() {
        mRtcEngine.setParameters("{\"rtc.log_filter\":65535}");
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                new VideoEncoderConfiguration.VideoDimensions(DEFAULT_CAPTURE_WIDTH, DEFAULT_CAPTURE_HEIGHT),
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        ));

        mRtcEngine.setExternalVideoSource(true, true, true);
        mRtcEngine.joinChannel(Config.TOKEN, Config.CHANNEL, null, 0);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void finish() {
        mRtcEngine.leaveChannel();
        mCamera.stopPreview();
        super.finish();
    }


    private void openCamera() {
        if (mCamera != null || mPreviewing) {
            Log.i(TAG, "Camera preview has been started");
            return;
        }
        mCamera = Camera.open(0);
    }

    private void stopPreview() {
        if (mCamera != null && mPreviewing) {
            mCamera.stopPreview();
            mPreviewing = false;
        }
    }

    private void closeCamera() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    @Override
    public void onSurfaceReady(SurfaceHolder holder) {
        mHolder = holder;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setupCameraAndStart();
            }
        });
    }

    @Override
    public void onSurfaceDestroy() {

    }

    private void setupCameraAndStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCamera != null) {
                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setPreviewSize(DEFAULT_CAPTURE_WIDTH, DEFAULT_CAPTURE_HEIGHT);
                        List<String> list = parameters.getSupportedFocusModes();
                        if (list.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        mCamera.setParameters(parameters);

                        mCamera.setDisplayOrientation(90);
                        mCamera.setPreviewDisplay(mHolder);
                        addCameraCallBack();
                        mCamera.setPreviewCallback(CustomCameraActivity.this);
                        mCamera.startPreview();
                        mPreviewing = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    byte[][] mPreviewBuffer = null;

    private void addCameraCallBack() {
        mPreviewBuffer = new byte[][]{
                new byte[DEFAULT_CAPTURE_WIDTH * DEFAULT_CAPTURE_HEIGHT * 3 / 2],
                new byte[DEFAULT_CAPTURE_WIDTH * DEFAULT_CAPTURE_HEIGHT * 3 / 2],
                new byte[DEFAULT_CAPTURE_WIDTH * DEFAULT_CAPTURE_HEIGHT * 3 / 2]};
        mCamera.addCallbackBuffer(mPreviewBuffer[0]);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (mCamera == null || !mPreviewing)
            return;
        AgoraVideoFrame agoraVideoFrame = new AgoraVideoFrame();
        agoraVideoFrame.buf = bytes;
        agoraVideoFrame.stride=DEFAULT_CAPTURE_WIDTH;
        agoraVideoFrame.height=DEFAULT_CAPTURE_HEIGHT;
        agoraVideoFrame.rotation = 90;
        agoraVideoFrame.format = AgoraVideoFrame.FORMAT_NV21;//指采集，!指定sdk转码格式
        agoraVideoFrame.timeStamp = System.currentTimeMillis();
        boolean isPush = mRtcEngine.pushExternalVideoFrame(agoraVideoFrame);
//        Log.e(TAG,"ispush:"+isPush);
        if (mCamera != null)
            mCamera.addCallbackBuffer(bytes);
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            Log.e(TAG, "Join channel success, uid: " + (uid & 0xFFFFFFFFL));
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, final int width, int height, int elapsed) {
            Log.e(TAG, "First remote video decoded, width: " + width);
        }


        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            super.onFirstLocalVideoFrame(width, height, elapsed);
            Log.e(TAG, String.format("width:%d--height:%d", width, height));
        }

        @Override
        public void onLocalVideoStats(LocalVideoStats localVideoStats) {
            super.onLocalVideoStats(localVideoStats);
            Log.e(TAG, "--" + localVideoStats.sentBitrate + "---encoderOutputFrameRate:" + localVideoStats.encoderOutputFrameRate + "---rendererOutputFrameRate:" + localVideoStats.rendererOutputFrameRate + "---encodedFrameWidth:" + localVideoStats.encodedFrameWidth);
        }

        @Override
        public void onUserOffline(final int uid, int reason) {
            Log.e(TAG, "User offline, uid: " + (uid & 0xFFFFFFFFL));
        }
    };
}
