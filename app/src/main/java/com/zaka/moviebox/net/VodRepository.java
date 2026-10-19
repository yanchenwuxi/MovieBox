package com.zaka.moviebox.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaka.moviebox.model.Episode;
import com.zaka.moviebox.model.PlayLine;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.model.Vod;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/** 多源聚合：支持 TVBox 配置 / 苹果CMS 采集接口 */
public class VodRepository {
    private final Context ctx;
    private final ExecutorService pool = Executors.newFixedThreadPool(6);
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Cb<T> { void ok(T t); void fail(String msg); }

    public VodRepository(Context c) { ctx = c.getApplicationContext(); }

    public ExecutorService pool() { return pool; }
    public Handler main() { return main; }

    // ===================== 接口配置 =====================
    /** 拉取接口地址：支持 TVBox JSON 配置 或 裸 CMS 地址 */
    public void loadConfig(String url, Cb<List<Site>> cb) {
        if (url == null || url.trim().isEmpty()) { cb.fail("地址为空"); return; }
        url = url.trim();
        final String finalUrl = url;
        Http.getAsync(url, new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { cb.fail("网络错误"); }
            @Override public void onResponse(Call call, Response r) {
                try {
                    String body = r.body() == null ? "" : r.body().string();
                    List<Site> list = parseConfig(body, finalUrl);
                    if (list.isEmpty()) cb.fail("未解析到可用源");
                    else cb.ok(list);
                } catch (Throwable e) { cb.fail("解析失败"); }
            }
        });
    }

    private List<Site> parseConfig(String body, String from) {
        List<Site> out = new ArrayList<>();
        String t = body.trim();
        if (t.startsWith("{")) {
            try {
                JsonObject o = JsonParser.parseString(t).getAsJsonObject();
                JsonArray sites = o.has("sites") ? o.getAsJsonArray("sites") : null;
                if (sites != null) {
                    for (JsonElement e : sites) {
                        JsonObject s = e.getAsJsonObject();
                        Site site = new Site();
                        site.key = str(s, "key");
                        site.name = str(s, "name");
                        site.api = str(s, "api");
                        site.type = s.has("type") ? s.get("type").getAsInt() : 1;
                        site.searchable = s.has("searchable") ? s.get("searchable").getAsInt() : 1;
                        site.quickSearch = s.has("quickSearch") ? s.get("quickSearch").getAsInt() : 1;
                        site.ext = s.has("ext") ? s.get("ext").toString() : "";
                        if (!site.name.isEmpty() && !site.api.isEmpty()) out.add(site);
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (out.isEmpty()) {
            // 裸 CMS 地址
            Site s = new Site();
            s.key = "cms_" + Math.abs(from.hashCode());
            s.name = "自定义源" + (Math.abs(from.hashCode()) % 100);
            s.api = from;
            s.type = 1;
            out.add(s);
        }
        return out;
    }

    // ===================== 分类 =====================
    public void categories(Site site, Cb<List<String[]>> cb) {
        pool.execute(() -> {
            List<String[]> result = new ArrayList<>();
            try {
                String url = site.base() + "?ac=list";
                String body = Http.get(url);
                JsonObject o = JsonParser.parseString(body).getAsJsonObject();
                JsonArray cls = o.has("class") ? o.getAsJsonArray("class") : null;
                if (cls != null) {
                    for (JsonElement e : cls) {
                        JsonObject c = e.getAsJsonObject();
                        String id = c.has("type_id") ? c.get("type_id").getAsString() : "";
                        String name = c.has("type_name") ? c.get("type_name").getAsString() : "";
                        if (!id.isEmpty()) result.add(new String[]{id, name});
                    }
                }
            } catch (Throwable ignored) {}
            final List<String[]> r = result;
            main.post(() -> { if (r.isEmpty()) cb.fail("no-class"); else cb.ok(r); });
        });
    }

    // ===================== 列表 =====================
    public void list(Site site, String typeId, int page, Cb<List<Vod>> cb) {
        pool.execute(() -> {
            List<Vod> out = new ArrayList<>();
            final String[] err = new String[]{null};
            String[] patterns = new String[]{
                    site.base() + "?ac=videolist&pg=" + page + "&t=" + typeId,
                    site.base() + "?ac=list&pg=" + page + "&t=" + typeId
            };
            for (String u : patterns) {
                try {
                    String body = Http.get(u);
                    out = parseList(body, site);
                    if (!out.isEmpty()) break;
                } catch (Throwable e) { err[0] = e.getMessage(); }
            }
            final List<Vod> r = out;
            final String errMsg = err[0];
            main.post(() -> { if (r.isEmpty()) cb.fail(errMsg == null ? "无数据" : errMsg); else cb.ok(r); });
        });
    }

    // ===================== 详情 =====================
    public void detail(Site site, String id, Cb<Vod> cb) {
        pool.execute(() -> {
            Vod v = null;
            try {
                String[] patterns = new String[]{
                        site.base() + "?ac=detail&ids=" + id,
                        site.base() + "?ac=videolist&ids=" + id
                };
                for (String u : patterns) {
                    try {
                        List<Vod> l = parseList(Http.get(u), site);
                        if (!l.isEmpty()) { v = l.get(0); break; }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            final Vod r = v;
            main.post(() -> { if (r == null) cb.fail("详情获取失败"); else cb.ok(r); });
        });
    }

    // ===================== 搜索 =====================
    public void search(Site site, String kw, int page, Cb<List<Vod>> cb) {
        pool.execute(() -> {
            List<Vod> out = new ArrayList<>();
            try {
                String u = site.base() + "?ac=videolist&wd=" + java.net.URLEncoder.encode(kw, "UTF-8") + "&pg=" + page;
                out = parseList(Http.get(u), site);
            } catch (Throwable ignored) {}
            final List<Vod> r = out;
            main.post(() -> cb.ok(r));
        });
    }

    // ===================== 解析 =====================
    private List<Vod> parseList(String body, Site site) {
        List<Vod> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray list = root.has("list") ? root.getAsJsonArray("list") : null;
            if (list == null) return out;
            for (JsonElement el : list) {
                JsonObject o = el.getAsJsonObject();
                Vod v = new Vod();
                v.siteKey = site.key;
                v.siteName = site.name;
                v.id = str(o, "vod_id");
                v.name = str(o, "vod_name");
                v.pic = str(o, "vod_pic");
                v.note = str(o, "vod_remarks");
                v.year = str(o, "vod_year");
                v.area = str(o, "vod_area");
                v.typeName = str(o, "type_name");
                v.actor = str(o, "vod_actor");
                v.director = str(o, "vod_director");
                v.desc = stripHtml(str(o, "vod_content"));
                v.score = str(o, "vod_score");
                String from = str(o, "vod_play_from");
                String url = str(o, "vod_play_url");
                if (!url.isEmpty()) parsePlay(v, from, url);
                if (!v.name.isEmpty()) out.add(v);
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private void parsePlay(Vod v, String from, String playUrl) {
        String[] lineNames = from.split("\\$\\$\\$");
        String[] lines = playUrl.split("\\$\\$\\$");
        for (int i = 0; i < lines.length; i++) {
            String ln = i < lineNames.length && !lineNames[i].isEmpty() ? lineNames[i] : ("线路" + (i + 1));
            PlayLine line = new PlayLine(ln);
            for (String ep : lines[i].split("#")) {
                if (ep.trim().isEmpty()) continue;
                int p = ep.indexOf('$');
                if (p > 0) {
                    String n = ep.substring(0, p);
                    String u = ep.substring(p + 1);
                    if (!u.isEmpty()) line.episodes.add(new Episode(n, u));
                } else {
                    line.episodes.add(new Episode("正片", ep));
                }
            }
            if (!line.episodes.isEmpty()) v.lines.add(line);
        }
    }

    private static String stripHtml(String s) {
        return s == null ? "" : s.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
    }

    private static String str(JsonObject o, String k) {
        try {
            if (!o.has(k) || o.get(k).isJsonNull()) return "";
            return o.get(k).getAsString();
        } catch (Throwable e) { return ""; }
    }

    // ===================== 资源演示源 =====================
    public String readAsset(String path) {
        try (InputStream in = ctx.getAssets().open(path)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } catch (Throwable e) { return ""; }
    }

    public List<Vod> demoList(String typeName) {
        List<Vod> out = new ArrayList<>();
        try {
            String body = readAsset("demo_cms.json");
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray list = root.getAsJsonArray("list");
            for (JsonElement el : list) {
                JsonObject o = el.getAsJsonObject();
                if (typeName != null && !typeName.isEmpty()) {
                    String tn = str(o, "type_name");
                    if (!tn.contains(typeName)) continue;
                }
                Vod v = new Vod();
                v.siteKey = "demo";
                v.siteName = "演示片源";
                v.id = str(o, "vod_id");
                v.name = str(o, "vod_name");
                v.pic = str(o, "vod_pic");
                v.note = str(o, "vod_remarks");
                v.year = str(o, "vod_year");
                v.area = str(o, "vod_area");
                v.typeName = str(o, "type_name");
                v.actor = str(o, "vod_actor");
                v.director = str(o, "vod_director");
                v.desc = stripHtml(str(o, "vod_content"));
                v.score = str(o, "vod_score");
                parsePlay(v, str(o, "vod_play_from"), str(o, "vod_play_url"));
                out.add(v);
            }
        } catch (Throwable ignored) {}
        return out;
    }

    public Vod demoDetail(String id) {
        for (Vod v : demoList("")) if (v.id.equals(id)) return v;
        return null;
    }
}
