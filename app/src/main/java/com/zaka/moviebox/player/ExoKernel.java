package com.zaka.moviebox.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * Media3 ExoPlayer 内核实现（阶段1默认内核）
 */
public class ExoKernel implements IKernel {
    private Context appCtx;
    private ExoPlayer player;
    private PlayerView playerView;
    private KernelListener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Map<String, String> pendingHeaders;
    private String pendingUrl;
    private float speed = 1f;
    private int scaleMode = 0;

    @Override
    public void init(Context context) {
        this.appCtx = context.getApplicationContext();
        if (player != null) return;

        OkHttpClient ok = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        OkHttpDataSource.Factory okFactory = new OkHttpDataSource.Factory(ok)
                .setUserAgent("MovieBox/2.0 (Exo)");

        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(appCtx)
                        .setDataSourceFactory(okFactory);

        player = new ExoPlayer.Builder(appCtx)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (listener == null) return;
                if (state == Player.STATE_BUFFERING) {
                    post(() -> listener.onLoading());
                } else if (state == Player.STATE_READY) {
                    post(() -> listener.onPrepared());
                } else if (state == Player.STATE_ENDED) {
                    post(() -> listener.onCompletion());
                }
            }

            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (listener != null) post(() -> listener.onIsPlayingChanged(isPlaying));
            }

            @Override public void onPlayerError(PlaybackException error) {
                String msg = error.getMessage() == null ? "播放错误" : error.getMessage();
                if (listener != null) post(() -> listener.onError(msg));
            }

            @Override public void onRenderedFirstFrame() {
                if (listener != null) post(() -> listener.onRenderingStart());
            }

            @Override public void onVideoSizeChanged(VideoSize videoSize) {
                if (listener != null) {
                    post(() -> listener.onVideoSizeChanged(videoSize.width, videoSize.height));
                }
            }
        });

        player.setPlaybackSpeed(speed);
        applyScale();
    }

    private void post(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else main.post(r);
    }

    @Override
    public void attachPlayerView(Object view) {
        if (view instanceof PlayerView) {
            this.playerView = (PlayerView) view;
            if (player != null) {
                playerView.setPlayer(player);
                applyScale();
            }
        }
    }

    @Override
    public void setDataSource(String url, Map<String, String> headers) {
        pendingUrl = url;
        pendingHeaders = headers;
    }

    @Override
    public void prepare() {
        if (player == null || pendingUrl == null) return;
        MediaItem item = MediaItem.fromUri(pendingUrl);
        // 若后续需要自定义 header，可在此用 MediaItem.Builder + 自定义 DataSource
        player.setMediaItem(item);
        player.prepare();
    }

    @Override public void play() { if (player != null) player.play(); }

    @Override public void pause() { if (player != null) player.pause(); }

    @Override public void stop() { if (player != null) player.stop(); }

    @Override
    public void seekTo(long ms) {
        if (player != null) player.seekTo(ms);
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
        if (player != null) player.setPlaybackSpeed(speed);
    }

    @Override
    public void setSurface(Surface surface) {
        // PlayerView 托管时不需要
    }

    @Override
    public void setVolume(float left, float right) {
        if (player != null) player.setVolume((left + right) / 2f);
    }

    @Override public boolean isPlaying() { return player != null && player.isPlaying(); }

    @Override public long getCurrentPosition() { return player == null ? 0 : player.getCurrentPosition(); }

    @Override public long getDuration() { return player == null ? 0 : Math.max(0, player.getDuration()); }

    @Override
    public int getBufferedPercentage() {
        return player == null ? 0 : player.getBufferedPercentage();
    }

    @Override
    public void setScaleMode(int mode) {
        this.scaleMode = mode;
        applyScale();
    }

    private void applyScale() {
        if (playerView == null) return;
        int resize;
        switch (scaleMode) {
            case 1: resize = AspectRatioFrameLayout.RESIZE_MODE_FILL; break;
            case 2: resize = AspectRatioFrameLayout.RESIZE_MODE_FIT; break;
            case 3: resize = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; break;
            default: resize = AspectRatioFrameLayout.RESIZE_MODE_FIT; break;
        }
        playerView.setResizeMode(resize);
    }

    @Override
    public void setListener(KernelListener listener) {
        this.listener = listener;
    }

    @Override
    public void release() {
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        listener = null;
        pendingUrl = null;
        pendingHeaders = null;
    }

    @Override
    public List<TrackInfo> getAudioTracks() {
        return collectTracks(C.TRACK_TYPE_AUDIO);
    }

    @Override
    public List<TrackInfo> getSubtitleTracks() {
        return collectTracks(C.TRACK_TYPE_TEXT);
    }

    private List<TrackInfo> collectTracks(int type) {
        List<TrackInfo> out = new ArrayList<>();
        if (player == null) return out;
        Tracks tracks = player.getCurrentTracks();
        int global = 0;
        for (Tracks.Group g : tracks.getGroups()) {
            if (g.getType() != type) continue;
            for (int i = 0; i < g.length; i++) {
                if (!g.isTrackSupported(i)) continue;
                androidx.media3.common.Format f = g.getTrackFormat(i);
                String lang = f.language == null ? "" : f.language;
                String label = f.label;
                if (label == null || label.isEmpty()) {
                    label = lang.isEmpty() ? ("轨道" + (global + 1)) : lang;
                }
                int t = type == C.TRACK_TYPE_AUDIO ? TrackInfo.TYPE_AUDIO : TrackInfo.TYPE_SUBTITLE;
                out.add(new TrackInfo(t, global, g.getMediaTrackGroup().id + ":" + i, label, lang));
                global++;
            }
        }
        return out;
    }

    @Override
    public void selectTrack(TrackInfo track) {
        if (player == null || track == null) return;
        try {
            Tracks tracks = player.getCurrentTracks();
            int type = track.isAudio() ? C.TRACK_TYPE_AUDIO : C.TRACK_TYPE_TEXT;
            int global = 0;
            for (Tracks.Group g : tracks.getGroups()) {
                if (g.getType() != type) continue;
                for (int i = 0; i < g.length; i++) {
                    if (!g.isTrackSupported(i)) continue;
                    if (global == track.index) {
                        player.setTrackSelectionParameters(
                                player.getTrackSelectionParameters()
                                        .buildUpon()
                                        .setOverrideForType(
                                                new TrackSelectionOverride(g.getMediaTrackGroup(), i))
                                        .build());
                        return;
                    }
                    global++;
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override public String name() { return "Exo"; }

    /** 供需要直接访问底层播放器的场景（一般不推荐） */
    public ExoPlayer getExoPlayer() { return player; }
}
