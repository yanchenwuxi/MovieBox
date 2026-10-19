package com.zaka.moviebox.player;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.util.Map;

/**
 * 系统 MediaPlayer 内核：无额外 so，作 Exo 失败时的兜底。
 */
public class SystemKernel implements IKernel {
    private Context appCtx;
    private MediaPlayer player;
    private TextureView textureView;
    private Surface surface;
    private KernelListener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private String pendingUrl;
    private Map<String, String> pendingHeaders;
    private float speed = 1f;
    private boolean prepared;
    private boolean startWhenReady;

    @Override
    public void init(Context context) {
        this.appCtx = context.getApplicationContext();
        if (player != null) return;
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(mp -> {
            prepared = true;
            try {
                // API 23+ playback params for speed
                android.media.PlaybackParams params = mp.getPlaybackParams();
                params.setSpeed(speed);
                mp.setPlaybackParams(params);
            } catch (Throwable ignored) {}
            post(() -> {
                if (listener != null) listener.onPrepared();
            });
            if (startWhenReady) {
                mp.start();
                post(() -> {
                    if (listener != null) {
                        listener.onRenderingStart();
                        listener.onIsPlayingChanged(true);
                    }
                });
            }
        });
        player.setOnCompletionListener(mp -> post(() -> {
            if (listener != null) listener.onCompletion();
        }));
        player.setOnErrorListener((mp, what, extra) -> {
            post(() -> {
                if (listener != null) listener.onError("系统播放器错误 what=" + what + " extra=" + extra);
            });
            return true;
        });
        player.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                post(() -> { if (listener != null) listener.onLoading(); });
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                    || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                post(() -> { if (listener != null) listener.onRenderingStart(); });
            }
            return false;
        });
        player.setOnVideoSizeChangedListener((mp, w, h) -> post(() -> {
            if (listener != null) listener.onVideoSizeChanged(w, h);
        }));
    }

    @Override
    public void attachPlayerView(Object playerView) {
        // 期望外部传入 TextureView；若传入 View 组则尝试 find
        if (playerView instanceof TextureView) {
            bindTexture((TextureView) playerView);
        } else if (playerView instanceof View) {
            View v = ((View) playerView).getRootView();
            // no-op: PlayerActivity 会单独传 textureView
        }
    }

    public void attachTextureView(TextureView tv) {
        bindTexture(tv);
    }

    private void bindTexture(TextureView tv) {
        this.textureView = tv;
        if (tv.isAvailable()) {
            setSurface(new Surface(tv.getSurfaceTexture()));
        }
        tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                setSurface(new Surface(st));
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                surface = null;
                try { if (player != null) player.setSurface(null); } catch (Throwable ignored) {}
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {}
        });
    }

    @Override
    public void setSurface(Surface surface) {
        this.surface = surface;
        if (player != null) {
            try { player.setSurface(surface); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void setDataSource(String url, Map<String, String> headers) {
        pendingUrl = url;
        pendingHeaders = headers;
        prepared = false;
        startWhenReady = false;
    }

    @Override
    public void prepare() {
        if (player == null || pendingUrl == null) return;
        post(() -> { if (listener != null) listener.onLoading(); });
        try {
            player.reset();
            prepared = false;
            if (surface != null) player.setSurface(surface);
            Uri uri = Uri.parse(pendingUrl);
            if (pendingHeaders != null && !pendingHeaders.isEmpty()) {
                player.setDataSource(appCtx, uri, pendingHeaders);
            } else {
                player.setDataSource(appCtx, uri);
            }
            player.prepareAsync();
        } catch (Throwable e) {
            post(() -> {
                if (listener != null) listener.onError(e.getMessage() == null ? "系统播放器打开失败" : e.getMessage());
            });
        }
    }

    @Override
    public void play() {
        if (player == null) return;
        startWhenReady = true;
        if (prepared) {
            try {
                player.start();
                post(() -> { if (listener != null) listener.onIsPlayingChanged(true); });
            } catch (Throwable e) {
                post(() -> { if (listener != null) listener.onError(e.getMessage()); });
            }
        }
    }

    @Override
    public void pause() {
        if (player == null) return;
        try {
            if (player.isPlaying()) {
                player.pause();
                post(() -> { if (listener != null) listener.onIsPlayingChanged(false); });
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void stop() {
        if (player == null) return;
        try { player.stop(); } catch (Throwable ignored) {}
        prepared = false;
        startWhenReady = false;
    }

    @Override
    public void seekTo(long ms) {
        if (player == null || !prepared) return;
        try { player.seekTo((int) ms); } catch (Throwable ignored) {}
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed <= 0 ? 1f : speed;
        if (player != null && prepared) {
            try {
                android.media.PlaybackParams params = player.getPlaybackParams();
                params.setSpeed(this.speed);
                player.setPlaybackParams(params);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void setVolume(float left, float right) {
        if (player != null) {
            try { player.setVolume(left, right); } catch (Throwable ignored) {}
        }
    }

    @Override
    public boolean isPlaying() {
        try { return player != null && player.isPlaying(); } catch (Throwable e) { return false; }
    }

    @Override
    public long getCurrentPosition() {
        try { return player == null ? 0 : player.getCurrentPosition(); } catch (Throwable e) { return 0; }
    }

    @Override
    public long getDuration() {
        try { return player == null || !prepared ? 0 : Math.max(0, player.getDuration()); } catch (Throwable e) { return 0; }
    }

    @Override
    public int getBufferedPercentage() {
        return 0;
    }

    @Override
    public void setListener(KernelListener listener) {
        this.listener = listener;
    }

    @Override
    public void release() {
        if (player != null) {
            try { player.release(); } catch (Throwable ignored) {}
            player = null;
        }
        surface = null;
        textureView = null;
        listener = null;
        pendingUrl = null;
        prepared = false;
    }

    @Override
    public String name() { return "System"; }

    private void post(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else main.post(r);
    }
}
