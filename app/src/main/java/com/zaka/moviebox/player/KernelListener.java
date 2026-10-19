package com.zaka.moviebox.player;

/** 内核事件回调（主线程触发由 MoviePlayer 保证） */
public interface KernelListener {
    /** 开始缓冲 / 准备中 */
    void onLoading();

    /** 已准备好，可播放 */
    void onPrepared();

    /** 真正开始渲染画面 */
    void onRenderingStart();

    /** 播放结束 */
    void onCompletion();

    /** 播放错误 */
    void onError(String message);

    /** 播放/暂停状态变化 */
    void onIsPlayingChanged(boolean isPlaying);

    /** 进度或时长变化（可选使用，也可自行轮询） */
    default void onProgress(long position, long duration) {}

    /** 视频尺寸变化 */
    default void onVideoSizeChanged(int width, int height) {}
}
