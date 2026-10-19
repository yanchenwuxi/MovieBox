package com.zaka.moviebox.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.model.Episode;
import com.zaka.moviebox.model.PlayLine;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BrowserActivity extends AppCompatActivity {
    private WebView web;
    private EditText et;
    private ProgressBar bar;
    private final Set<String> sniffed = new LinkedHashSet<>();

    private static final String[] RECOMMEND = {
            "https://www.bing.com", "https://m.baidu.com", "https://www.google.com"
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_browser);
        Ui.statusBarPadding(this, findViewById(R.id.etUrl).getRootView());

        web = findViewById(R.id.webView);
        et = findViewById(R.id.etUrl);
        bar = findViewById(R.id.progressBar);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView v, String url, Bitmap f) {
                et.setText(url);
            }
            @Override public void onPageFinished(WebView v, String url) {
                App.get().store.addBrowserHistory(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Throwable ignored) {}
                return true;
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                checkSniff(r.getUrl().toString());
                return null;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); else finish(); });
        findViewById(R.id.btnForward).setOnClickListener(v -> { if (web.canGoForward()) web.goForward(); });
        findViewById(R.id.btnRefresh).setOnClickListener(v -> web.reload());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnHome).setOnClickListener(v -> web.loadUrl("about:blank"));
        findViewById(R.id.btnSniff).setOnClickListener(v -> showSniffed());
        findViewById(R.id.btnHistoryUrl).setOnClickListener(v -> showRecommend());

        et.setOnEditorActionListener((v, a, e) -> {
            String t = et.getText().toString().trim();
            if (t.isEmpty()) return true;
            load(t);
            return true;
        });

        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) url = "https://www.bing.com";
        load(url);
    }

    private void load(String t) {
        if (!t.startsWith("http")) {
            if (t.contains(".") && !t.contains(" ")) t = "https://" + t;
            else t = "https://www.bing.com/search?q=" + Uri.encode(t);
        }
        web.loadUrl(t);
    }

    private void checkSniff(String u) {
        if (u == null) return;
        String l = u.toLowerCase();
        if (l.contains(".m3u8") || l.contains(".mp4") || l.contains(".flv") || l.contains(".m4v")) {
            if (!sniffed.contains(u)) {
                sniffed.add(u);
                runOnUiThread(() -> Ui.toast(this, "嗅探到视频地址（" + sniffed.size() + "）"));
            }
        }
    }

    private void showSniffed() {
        if (sniffed.isEmpty()) {
            Ui.toast(this, "暂未嗅探到视频，先播放一段试试");
            return;
        }
        List<String> list = new ArrayList<>(sniffed);
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("嗅探到的播放地址");
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.zaka.moviebox.adapter.TextAdapter ad = new com.zaka.moviebox.adapter.TextAdapter((i, t) -> {
            playUrl(list.get(i));
            d.dismiss();
        });
        List<String> names = new ArrayList<>();
        for (String s : list) names.add(s.length() > 60 ? s.substring(0, 60) + "…" : s);
        ad.setData(names);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    private void playUrl(String url) {
        Vod vod = new Vod();
        vod.siteKey = "browser";
        vod.siteName = "浏览器嗅探";
        vod.id = String.valueOf(url.hashCode());
        vod.name = web.getTitle() == null ? "网页视频" : web.getTitle();
        PlayLine l = new PlayLine("嗅探线路");
        l.episodes.add(new Episode("正片", url));
        vod.lines.add(l);
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("vod", vod);
        startActivity(i);
    }

    private void showRecommend() {
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("常用站点 / 浏览记录");
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        List<String> urls = new ArrayList<>();
        urls.addAll(App.get().store.browserHistory());
        for (String r : RECOMMEND) if (!urls.contains(r)) urls.add(r);

        com.zaka.moviebox.adapter.TextAdapter ad = new com.zaka.moviebox.adapter.TextAdapter((i, t) -> {
            load(urls.get(i));
            d.dismiss();
        });
        ad.setData(urls);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
