package com.zaka.moviebox.player;

/** 音轨 / 字幕轨道描述（阶段1预留，阶段3完善选择UI） */
public class TrackInfo {
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_SUBTITLE = 2;

    public final int type;
    public final int index;
    public final String id;
    public final String name;
    public final String language;

    public TrackInfo(int type, int index, String id, String name, String language) {
        this.type = type;
        this.index = index;
        this.id = id == null ? "" : id;
        this.name = name == null ? ("轨道" + index) : name;
        this.language = language == null ? "" : language;
    }

    public boolean isAudio() { return type == TYPE_AUDIO; }
    public boolean isSubtitle() { return type == TYPE_SUBTITLE; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (!language.isEmpty()) sb.append(" [").append(language).append("]");
        return sb.toString();
    }
}
