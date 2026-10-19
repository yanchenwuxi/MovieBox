package com.zaka.moviebox.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Vod implements Serializable {
    public String siteKey = "";
    public String siteName = "";
    public String id = "";
    public String name = "";
    public String pic = "";
    public String note = "";
    public String year = "";
    public String area = "";
    public String typeName = "";
    public String actor = "";
    public String director = "";
    public String desc = "";
    public String score = "";
    public List<PlayLine> lines = new ArrayList<>();

    public String key() { return siteKey + "@" + id; }

    public String firstPlayUrl() {
        for (PlayLine l : lines) if (!l.episodes.isEmpty()) return l.episodes.get(0).url;
        return "";
    }
}
