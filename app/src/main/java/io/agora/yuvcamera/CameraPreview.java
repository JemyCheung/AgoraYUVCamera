package io.agora.yuvcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by jemy on 2020/8/27
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    public interface CameraPreviewCallback {
        void onSurfaceReady(SurfaceHolder holder);
        void onSurfaceDestroy();
    }
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraPreviewCallback mCallback;

    public CameraPreview(Context context, Camera camera, CameraPreviewCallback callback) {
        super(context);
        mCallback = callback;
        //初始化Camera对象
        mCamera = camera;
        //得到SurfaceHolder对象
        mHolder = getHolder();
        //添加回调，得到Surface的三个声明周期方法
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(CustomCameraActivity.class.getSimpleName(), "camera surfaceCreated");

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("zwtest", "camera surfaceChanged");
        if (holder.getSurface() == null) {
            return;
        }
        if (mCallback != null) {
            mCallback.onSurfaceReady(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCallback != null) {
            mCallback.onSurfaceDestroy();
        }
    }
}
