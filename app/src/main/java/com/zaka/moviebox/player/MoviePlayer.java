package com.zaka.moviebox.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;

import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Map;

/**
 * 播放器门面：支持重试与 AUTO 模式下 Exo→System 容错切换。
 */
public class MoviePlayer {
    private final Context appCtx;
    private IKernel kernel;
    private KernelListener externalListener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int kernelType = IKernel.KERNEL_AUTO;
    private int activeKernelType = IKernel.KERNEL_EXO;
    private float speed = 1f;
    private int scaleMode = 0;
    private Object attachedExoView;
    private TextureView textureView;

    private String currentUrl;
    private Map<String, String> currentHeaders;
    private long pendingSeekMs = -1;

    private int maxRetry = 1;
    private int retryLeft = 0;
    private boolean failoverTried = false;
    private boolean autoFailover = true;

    public MoviePlayer(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public void setKernelType(int type) {
        if (this.kernelType == type && kernel != null) return;
        this.kernelType = type;
        this.failoverTried = false;
        rebuildKernel(resolveInitialKernel(type));
    }

    public void setAutoFailover(boolean enable) { this.autoFailover = enable; }

    public void setMaxRetry(int n) { this.maxRetry = Math.max(0, n); }

    public int getKernelType() { return kernelType; }

    public int getActiveKernelType() { return activeKernelType; }

    public String currentKernelName() {
        return kernel == null ? "" : kernel.name();
    }

    private int resolveInitialKernel(int type) {
        if (type == IKernel.KERNEL_AUTO) return IKernel.KERNEL_EXO;
        return type;
    }

    private void rebuildKernel(int concreteType) {
        Object exoView = attachedExoView;
        TextureView tv = textureView;
        releaseKernelOnly();
        activeKernelType = concreteType;
        kernel = KernelFactory.create(appCtx, concreteType);
        kernel.setSpeed(speed);
        kernel.setScaleMode(scaleMode);
        kernel.setListener(internalListener);
        if (concreteType == IKernel.KERNEL_EXO && exoView != null) {
            kernel.attachPlayerView(exoView);
            setViewVisibility(exoView, View.VISIBLE);
            if (tv != null) tv.setVisibility(View.GONE);
        } else if (concreteType == IKernel.KERNEL_SYSTEM && tv != null) {
            if (kernel instanceof SystemKernel) {
                ((SystemKernel) kernel).attachTextureView(tv);
            }
            tv.setVisibility(View.VISIBLE);
            if (exoView instanceof View) ((View) exoView).setVisibility(View.GONE);
        } else if (exoView != null) {
            kernel.attachPlayerView(exoView);
        }
        attachedExoView = exoView;
        textureView = tv;
    }

    private void setViewVisibility(Object view, int vis) {
        if (view instanceof View) ((View) view).setVisibility(vis);
    }

    private void ensureKernel() {
        if (kernel == null) rebuildKernel(resolveInitialKernel(kernelType));
    }

    public void attachPlayerView(Object playerView) {
        this.attachedExoView = playerView;
        ensureKernel();
        if (activeKernelType == IKernel.KERNEL_EXO) {
            kernel.attachPlayerView(playerView);
            setViewVisibility(playerView, View.VISIBLE);
            if (textureView != null) textureView.setVisibility(View.GONE);
        }
    }

    public void attachTextureView(TextureView tv) {
        this.textureView = tv;
        if (kernel instanceof SystemKernel) {
            ((SystemKernel) kernel).attachTextureView(tv);
            tv.setVisibility(View.VISIBLE);
            if (attachedExoView instanceof View) ((View) attachedExoView).setVisibility(View.GONE);
        } else if (tv != null) {
            tv.setVisibility(View.GONE);
        }
    }

    public void setListener(KernelListener listener) {
        this.externalListener = listener;
    }

    public void setDataSource(String url) {
        setDataSource(url, null);
    }

    public void setDataSource(String url, Map<String, String> headers) {
        currentUrl = url;
        currentHeaders = headers == null ? new HashMap<>() : new HashMap<>(headers);
        retryLeft = maxRetry;
        failoverTried = false;
        // AUTO 每次新片源从 Exo 起
        if (kernelType == IKernel.KERNEL_AUTO && activeKernelType != IKernel.KERNEL_EXO) {
            rebuildKernel(IKernel.KERNEL_EXO);
        } else {
            ensureKernel();
        }
        kernel.setDataSource(url, currentHeaders);
    }

    public void prepare() {
        ensureKernel();
        kernel.prepare();
    }

    public void play() {
        ensureKernel();
        kernel.play();
    }

    /** 打开并播放，可带起始进度 */
    public void open(String url, Map<String, String> headers, long seekMs) {
        pendingSeekMs = seekMs;
        setDataSource(url, headers);
        prepare();
        play();
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
        attachedExoView = null;
        textureView = null;
        externalListener = null;
        currentUrl = null;
    }

    private void releaseKernelOnly() {
        if (kernel != null) {
            kernel.setListener(null);
            kernel.release();
            kernel = null;
        }
    }

    private void tryFailoverOrRetry(String errorMsg) {
        // 同内核重试
        if (retryLeft > 0 && currentUrl != null) {
            retryLeft--;
            final String url = currentUrl;
            final Map<String, String> headers = currentHeaders;
            main.postDelayed(() -> {
                if (kernel == null) return;
                kernel.setDataSource(url, headers);
                kernel.prepare();
                kernel.play();
            }, 400);
            return;
        }
        // AUTO：Exo 失败切 System 一次
        if (autoFailover && kernelType == IKernel.KERNEL_AUTO
                && !failoverTried && activeKernelType == IKernel.KERNEL_EXO
                && currentUrl != null) {
            failoverTried = true;
            retryLeft = maxRetry;
            final String url = currentUrl;
            final Map<String, String> headers = currentHeaders;
            final long seek = pendingSeekMs > 0 ? pendingSeekMs : 0;
            main.post(() -> {
                rebuildKernel(IKernel.KERNEL_SYSTEM);
                kernel.setDataSource(url, headers);
                kernel.prepare();
                if (seek > 5000) kernel.seekTo(seek);
                kernel.play();
                if (externalListener != null) {
                    // 可选提示由 UI 处理；此处不打断错误流
                }
            });
            return;
        }
        if (externalListener != null) {
            externalListener.onError(errorMsg);
        }
    }

    private final KernelListener internalListener = new KernelListener() {
        @Override public void onLoading() {
            if (externalListener != null) main.post(externalListener::onLoading);
        }

        @Override public void onPrepared() {
            if (pendingSeekMs > 5000 && kernel != null) {
                kernel.seekTo(pendingSeekMs);
                pendingSeekMs = -1;
            }
            if (externalListener != null) main.post(externalListener::onPrepared);
        }

        @Override public void onRenderingStart() {
            if (externalListener != null) main.post(externalListener::onRenderingStart);
        }

        @Override public void onCompletion() {
            if (externalListener != null) main.post(externalListener::onCompletion);
        }

        @Override public void onError(String message) {
            tryFailoverOrRetry(message == null ? "播放失败" : message);
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
