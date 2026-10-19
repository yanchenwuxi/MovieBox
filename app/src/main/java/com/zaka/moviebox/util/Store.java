package com.zaka.moviebox.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.player.IKernel;

import java.util.ArrayList;
import java.util.List;

public class Store {
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public Store(Context c) {
        sp = c.getSharedPreferences("moviebox", Context.MODE_PRIVATE);
    }

    // ---------- 接口 ----------
    public List<Site> sites() {
        String s = sp.getString("sites", "");
        if (s.isEmpty()) return new ArrayList<>();
        try {
            List<Site> l = gson.fromJson(s, new TypeToken<List<Site>>(){}.getType());
            return l == null ? new ArrayList<>() : l;
        } catch (Throwable e) { return new ArrayList<>(); }
    }

    public void saveSites(List<Site> l) { sp.edit().putString("sites", gson.toJson(l)).apply(); }

    public String configUrl() { return sp.getString("config_url", ""); }
    public void setConfigUrl(String u) { sp.edit().putString("config_url", u).apply(); }

    // ---------- 历史 ----------
    public List<Vod> history() {
        try {
            List<Vod> l = gson.fromJson(sp.getString("history", "[]"), new TypeToken<List<Vod>>(){}.getType());
            return l == null ? new ArrayList<>() : l;
        } catch (Throwable e) { return new ArrayList<>(); }
    }

    public void addHistory(Vod v) {
        if (v == null || v.name == null) return;
        List<Vod> l = history();
        for (int i = l.size() - 1; i >= 0; i--) {
            if (l.get(i).key().equals(v.key())) l.remove(i);
        }
        Vod c = new Vod();
        c.siteKey = v.siteKey; c.siteName = v.siteName; c.id = v.id; c.name = v.name;
        c.pic = v.pic; c.note = v.note; c.year = v.year; c.typeName = v.typeName;
        l.add(0, c);
        while (l.size() > 200) l.remove(l.size() - 1);
        sp.edit().putString("history", gson.toJson(l)).apply();
        sp.edit().putLong("last_" + c.key(), System.currentTimeMillis()).apply();
    }

    public void clearHistory() { sp.edit().putString("history", "[]").apply(); }

    /** 播放进度 key 建议：vodKey#line#ep */
    public long lastPos(String key) { return sp.getLong("pos_" + key, 0); }
    public void setLastPos(String key, long pos) { sp.edit().putLong("pos_" + key, pos).apply(); }

    // ---------- 收藏 ----------
    public List<Vod> collects() {
        try {
            List<Vod> l = gson.fromJson(sp.getString("collect", "[]"), new TypeToken<List<Vod>>(){}.getType());
            return l == null ? new ArrayList<>() : l;
        } catch (Throwable e) { return new ArrayList<>(); }
    }

    public boolean isCollected(Vod v) {
        if (v == null) return false;
        for (Vod x : collects()) if (x.key().equals(v.key())) return true;
        return false;
    }

    public void toggleCollect(Vod v) {
        List<Vod> l = collects();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).key().equals(v.key())) { l.remove(i); saveCollect(l); return; }
        }
        Vod c = new Vod();
        c.siteKey = v.siteKey; c.siteName = v.siteName; c.id = v.id; c.name = v.name;
        c.pic = v.pic; c.note = v.note; c.year = v.year; c.typeName = v.typeName;
        l.add(0, c);
        saveCollect(l);
    }

    private void saveCollect(List<Vod> l) { sp.edit().putString("collect", gson.toJson(l)).apply(); }

    // ---------- 设置 ----------
    public float speed() { return sp.getFloat("speed", 1.0f); }
    public void setSpeed(float s) { sp.edit().putFloat("speed", s).apply(); }

    public int scale() { return sp.getInt("scale", 0); }
    public void setScale(int s) { sp.edit().putInt("scale", s).apply(); }

    /** 播放内核：0自动 1Exo 2IJK 3系统（阶段1仅 0/1 生效） */
    public int playKernel() { return sp.getInt("play_kernel", IKernel.KERNEL_AUTO); }
    public void setPlayKernel(int k) { sp.edit().putInt("play_kernel", k).apply(); }

    /** 同内核失败重试次数 */
    public int retryCount() { return sp.getInt("retry_count", 1); }
    public void setRetryCount(int n) { sp.edit().putInt("retry_count", Math.max(0, n)).apply(); }

    /** AUTO 模式下是否允许 Exo 失败后切系统播放器 */
    public boolean autoFailover() { return sp.getBoolean("auto_failover", true); }
    public void setAutoFailover(boolean v) { sp.edit().putBoolean("auto_failover", v).apply(); }

    public boolean autoNext() { return sp.getBoolean("auto_next", true); }
    public void setAutoNext(boolean v) { sp.edit().putBoolean("auto_next", v).apply(); }

    public boolean gestureEnable() { return sp.getBoolean("gesture_enable", true); }
    public void setGestureEnable(boolean v) { sp.edit().putBoolean("gesture_enable", v).apply(); }

    public void setLastWatch(String vodKey, int line, int ep, String epName) {
        sp.edit()
                .putInt("lw_line_" + vodKey, line)
                .putInt("lw_ep_" + vodKey, ep)
                .putString("lw_epn_" + vodKey, epName == null ? "" : epName)
                .apply();
    }

    public String lastWatchLabel(String vodKey) {
        String epn = sp.getString("lw_epn_" + vodKey, "");
        long pos = sp.getLong("lw_pos_" + vodKey, 0);
        if (epn.isEmpty() && pos <= 0) return "";
        String t = formatPos(pos);
        if (!epn.isEmpty() && !t.isEmpty()) return "看到 " + epn + " · " + t;
        if (!epn.isEmpty()) return "看到 " + epn;
        return "看到 " + t;
    }

    public void setLastWatchProgress(String vodKey, long pos, long dur) {
        sp.edit().putLong("lw_pos_" + vodKey, pos).putLong("lw_dur_" + vodKey, dur).apply();
    }

    private static String formatPos(long ms) {
        if (ms <= 0) return "";
        int s = (int) (ms / 1000);
        int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    public String searchHistoryJson() { return sp.getString("search_history", "[]"); }
    public List<String> searchHistory() {
        try {
            List<String> l = gson.fromJson(searchHistoryJson(), new TypeToken<List<String>>(){}.getType());
            return l == null ? new ArrayList<>() : l;
        } catch (Throwable e) { return new ArrayList<>(); }
    }
    public void addSearchHistory(String kw) {
        List<String> l = searchHistory();
        l.remove(kw);
        l.add(0, kw);
        while (l.size() > 20) l.remove(l.size() - 1);
        sp.edit().putString("search_history", gson.toJson(l)).apply();
    }
    public void clearSearchHistory() { sp.edit().putString("search_history", "[]").apply(); }

    public String lastSiteKey() { return sp.getString("last_site", ""); }
    public void setLastSiteKey(String k) { sp.edit().putString("last_site", k).apply(); }

    public List<String> browserHistory() {
        try {
            List<String> l = gson.fromJson(sp.getString("browser_history", "[]"), new TypeToken<List<String>>(){}.getType());
            return l == null ? new ArrayList<>() : l;
        } catch (Throwable e) { return new ArrayList<>(); }
    }
    public void addBrowserHistory(String url) {
        List<String> l = browserHistory();
        l.remove(url);
        l.add(0, url);
        while (l.size() > 30) l.remove(l.size() - 1);
        sp.edit().putString("browser_history", gson.toJson(l)).apply();
    }
}
