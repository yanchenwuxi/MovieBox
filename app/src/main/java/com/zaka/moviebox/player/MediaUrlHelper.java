package com.zaka.moviebox.player;

import java.util.Locale;

/** 播放地址判断与归一化 */
public final class MediaUrlHelper {
    private MediaUrlHelper() {}

    public static boolean isDirect(String url) {
        if (url == null) return false;
        String s = url.trim().toLowerCase(Locale.US);
        if (s.isEmpty()) return false;
        if (s.startsWith("file://") || s.startsWith("content://") || s.startsWith("android.resource://")) {
            return true;
        }
        if (s.startsWith("rtmp://") || s.startsWith("rtsp://") || s.startsWith("udp://") || s.startsWith("rtp://")) {
            return true;
        }
        if (hasExt(s, ".m3u8", ".mp4", ".mkv", ".flv", ".ts", ".m4v", ".webm", ".mov", ".mpd", ".m3u")) {
            return true;
        }
        // 常见流式路径特征
        if (s.startsWith("http://") || s.startsWith("https://")) {
            if (s.contains("playlist") || s.contains("/play/") || s.contains("stream")
                    || s.contains("video") || s.contains(".m3u8?") || s.contains(".mp4?")) {
                return true;
            }
            // 无扩展名的长 token 直链（保守：仅当明显是媒体 CDN）
            if (s.contains("cdn") && (s.contains("media") || s.contains("vod") || s.contains("hls"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHls(String url) {
        if (url == null) return false;
        String s = url.toLowerCase(Locale.US);
        return s.contains(".m3u8") || s.contains("m3u8?");
    }

    public static boolean isRtsp(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("rtsp://");
    }

    public static boolean isRtmp(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("rtmp://");
    }

    private static boolean hasExt(String s, String... exts) {
        for (String e : exts) {
            if (s.contains(e)) return true;
        }
        return false;
    }
}
