package com.zaka.moviebox.model;

import java.io.Serializable;

public class Site implements Serializable {
    public String key = "";
    public String name = "";
    public int type = 1;          // 1: cms json  0: xml
    public String api = "";
    public int searchable = 1;
    public int quickSearch = 1;
    public String ext = "";
    public String jar = "";
    public boolean enabled = true;

    public Site() {}
    public Site(String key, String name, String api) { this.key = key; this.name = name; this.api = api; }
    public String base() {
        String a = api == null ? "" : api.trim();
        int i = a.indexOf('?');
        String b = i > 0 ? a.substring(0, i) : a;
        if (!b.endsWith("/")) {
            int slash = b.lastIndexOf('/');
            if (slash > 8) b = b.substring(0, slash + 1);
        }
        return b;
    }
}
