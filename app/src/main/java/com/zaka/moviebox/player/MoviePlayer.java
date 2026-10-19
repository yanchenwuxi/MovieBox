package com.zaka.moviebox.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/**
 * 播放器门面：UI 只与 MoviePlayer 交互，不直接碰具体内核。
 */
public class MoviePlayer {
    private final Context appCtx;
    private IKernel kernel;
    private KernelListener externalListener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int kernelType = IKernel.KERNEL_AUTO;
    private float speed = 1f;
    private int scaleMode = 0;
    private Object attachedView;

    public MoviePlayer(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void setKernelType(int type) {
        if (this.kernelType == type && kernel != null) return;
        this.kernelType = type;
        rebuildKernel();
    }

    public int getKernelType() { return kernelType; }

    public String currentKernelName() {
        return kernel == null ? "" : kernel.name();
    }

    private void rebuildKernel() {
        Object view = attachedView;
        releaseKernelOnly();
        kernel = KernelFactory.create(appCtx, kernelType);
        kernel.setSpeed(speed);
        kernel.setScaleMode(scaleMode);
        kernel.setListener(internalListener);
        if (view != null) {
            kernel.attachPlayerView(view);
            attachedView = view;
        }
    }

    private void ensureKernel() {
        if (kernel == null) rebuildKernel();
    }

    public void attachPlayerView(Object playerView) {
        this.attachedView = playerView;
        ensureKernel();
        kernel.attachPlayerView(playerView);
    }

    public void setListener(KernelListener listener) {
        this.externalListener = listener;
    }

    public void setDataSource(String url) {
        setDataSource(url, null);
    }

    public void setDataSource(String url, Map<String, String> headers) {
        ensureKernel();
        Map<String, String> h = headers == null ? new HashMap<>() : new HashMap<>(headers);
        kernel.setDataSource(url, h);
    }

    public void prepare() {
        ensureKernel();
        kernel.prepare();
    }

    public void play() {
        ensureKernel();
        kernel.play();
    }

    public void pause() {
        if (kernel != null) kernel.pause();
    }

    public void stop() {
        if (kernel != null) kernel.stop();
    }

    public void seekTo(long ms) {
        if (kernel != null) kernel.seekTo(ms);
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        if (kernel != null) kernel.setSpeed(speed);
    }

    public float getSpeed() { return speed; }

    public void setScaleMode(int mode) {
        this.scaleMode = mode;
        if (kernel != null) kernel.setScaleMode(mode);
    }

    public boolean isPlaying() {
        return kernel != null && kernel.isPlaying();
    }

    public long getCurrentPosition() {
        return kernel == null ? 0 : kernel.getCurrentPosition();
    }

    public long getDuration() {
        return kernel == null ? 0 : kernel.getDuration();
    }

    public int getBufferedPercentage() {
        return kernel == null ? 0 : kernel.getBufferedPercentage();
    }

    public void release() {
        releaseKernelOnly();
        attachedView = null;
        externalListener = null;
    }

    private void releaseKernelOnly() {
        if (kernel != null) {
            kernel.setListener(null);
            kernel.release();
            kernel = null;
        }
    }

    private final KernelListener internalListener = new KernelListener() {
        @Override public void onLoading() {
            if (externalListener != null) main.post(externalListener::onLoading);
        }

        @Override public void onPrepared() {
            if (externalListener != null) main.post(externalListener::onPrepared);
        }

        @Override public void onRenderingStart() {
            if (externalListener != null) main.post(externalListener::onRenderingStart);
        }

        @Override public void onCompletion() {
            if (externalListener != null) main.post(externalListener::onCompletion);
        }

        @Override public void onError(String message) {
            if (externalListener != null) main.post(() -> externalListener.onError(message));
        }

        @Override public void onIsPlayingChanged(boolean isPlaying) {
            if (externalListener != null) main.post(() -> externalListener.onIsPlayingChanged(isPlaying));
        }

        @Override public void onProgress(long position, long duration) {
            if (externalListener != null) main.post(() -> externalListener.onProgress(position, duration));
        }

        @Override public void onVideoSizeChanged(int width, int height) {
            if (externalListener != null) main.post(() -> externalListener.onVideoSizeChanged(width, height));
        }
    };
}
