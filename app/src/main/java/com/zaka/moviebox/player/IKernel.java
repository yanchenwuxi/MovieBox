package com.zaka.moviebox.player;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 播放内核统一接口。
 * 阶段1：ExoKernel 实现；阶段2：补充 IjkKernel / SystemKernel。
 */
public interface IKernel {

    int KERNEL_AUTO = 0;
    int KERNEL_EXO = 1;
    int KERNEL_IJK = 2;
    int KERNEL_SYSTEM = 3;

    void init(Context context);

    void setDataSource(String url, Map<String, String> headers);

    void prepare();

    void play();

    void pause();

    void stop();

    void seekTo(long ms);

    void setSpeed(float speed);

    /**
     * 绑定渲染视图。Exo 优先使用官方 PlayerView；
     * 其它内核可使用 Surface / TextureView。
     */
    default void attachPlayerView(Object playerView) {}

    default void setSurface(Surface surface) {}

    default void setSurfaceView(SurfaceView view) {}

    default void setTextureView(TextureView view) {}

    void setVolume(float left, float right);

    boolean isPlaying();

    long getCurrentPosition();

    long getDuration();

    int getBufferedPercentage();

    default List<TrackInfo> getAudioTracks() { return Collections.emptyList(); }

    default List<TrackInfo> getSubtitleTracks() { return Collections.emptyList(); }

    default void selectTrack(TrackInfo track) {}

    /** 画面比例：0默认 1填充 2原始 3拉伸 */
    default void setScaleMode(int mode) {}

    void setListener(KernelListener listener);

    void release();

    /** 内核显示名 */
    String name();
}
