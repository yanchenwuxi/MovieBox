package com.zaka.moviebox.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.EpisodeAdapter;
import com.zaka.moviebox.adapter.VideoAdapter;
import com.zaka.moviebox.model.PlayLine;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.net.VodRepository;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private Vod vod;
    private int lineIndex = 0, epIndex = 0;
    private TabLayout tabLines;
    private RecyclerView rvEpisodes;
    private EpisodeAdapter epAdapter;
    private TextView tvPlay;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_detail);
        vod = (Vod) getIntent().getSerializableExtra("vod");
        if (vod == null) { finish(); return; }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnIntro).setOnClickListener(v -> showIntro());
        findViewById(R.id.btnSwitch).setOnClickListener(v -> quickSearch());
        tvPlay = findViewById(R.id.btnPlay).findViewById(R.id.tvPlayLabel);
        findViewById(R.id.btnPlay).setOnClickListener(v -> play(0));

        tabLines = findViewById(R.id.tabLines);
        rvEpisodes = findViewById(R.id.rvEpisodes);
        rvEpisodes.setLayoutManager(new GridLayoutManager(this, 4));
        epAdapter = new EpisodeAdapter(i -> { epIndex = i; play(i); });
        rvEpisodes.setAdapter(epAdapter);

        bind();
        loadDetail();
    }

    private void bind() {
        ((TextView) findViewById(R.id.tvName)).setText(vod.name);
        TextView score = findViewById(R.id.tvScore);
        if (!TextUtils.isEmpty(vod.score) && !"0.0".equals(vod.score)) {
            score.setVisibility(View.VISIBLE);
            score.setText("★ " + vod.score);
        } else score.setVisibility(View.GONE);

        TextView type = findViewById(R.id.tvType);
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(vod.year)) sb.append(vod.year).append(" · ");
        if (!TextUtils.isEmpty(vod.area)) sb.append(vod.area).append(" · ");
        if (!TextUtils.isEmpty(vod.typeName)) sb.append(vod.typeName);
        type.setText(sb);

        TextView actor = findViewById(R.id.tvActor);
        StringBuilder ab = new StringBuilder();
        if (!TextUtils.isEmpty(vod.actor)) ab.append("主演：").append(vod.actor);
        if (!TextUtils.isEmpty(vod.director)) ab.append(ab.length() > 0 ? "  " : "").append("导演：").append(vod.director);
        actor.setText(ab);
        actor.setVisibility(ab.length() == 0 ? View.GONE : View.VISIBLE);

        TextView desc = findViewById(R.id.tvDesc);
        desc.setText(TextUtils.isEmpty(vod.desc) ? "暂无简介" : vod.desc);

        ImageView poster = findViewById(R.id.ivPoster);
        ImageView backdrop = findViewById(R.id.ivBackdrop);
        loadImg(vod.pic, poster);
        loadImg(vod.pic, backdrop);

        bindLines();
        loadRecommend();
    }

    private void loadRecommend() {
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvRecommend);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        VideoAdapter ad = new VideoAdapter(v -> {
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra("vod", v);
            startActivity(i);
            finish();
        });
        rv.setAdapter(ad);
        List<Vod> rec = new ArrayList<>();
        for (Vod v : App.get().repo.demoList(vod.typeName)) {
            if (!v.name.equals(vod.name) && rec.size() < 6) rec.add(v);
        }
        if (rec.isEmpty()) {
            for (Vod v : App.get().repo.demoList("")) {
                if (!v.name.equals(vod.name) && rec.size() < 6) rec.add(v);
            }
        }
        ad.setData(rec);
    }

    private void loadImg(String url, ImageView iv) {
        if (url != null && url.startsWith("assets/")) {
            Glide.with(this).load("file:///android_asset/" + url.substring(7))
                    .placeholder(R.drawable.bg_img_placeholder).into(iv);
        } else {
            Glide.with(this).load(url).placeholder(R.drawable.bg_img_placeholder).into(iv);
        }
    }

    private void bindLines() {
        tabLines.removeAllTabs();
        if (vod.lines.isEmpty()) {
            tvPlay.setText("暂无可播放线路");
            return;
        }
        for (PlayLine l : vod.lines) tabLines.addTab(tabLines.newTab().setText(l.name));
        tabLines.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                lineIndex = t.getPosition();
                epIndex = 0;
                bindEpisodes();
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        bindEpisodes();
    }

    private void bindEpisodes() {
        if (lineIndex >= vod.lines.size()) return;
        PlayLine l = vod.lines.get(lineIndex);
        epAdapter.setData(l.episodes, epIndex);
        tvPlay.setText(l.episodes.isEmpty() ? "暂无可播放剧集" : "立即播放 · " + l.episodes.get(0).name);
    }

    private void loadDetail() {
        if ("demo".equals(vod.siteKey)) {
            App.get().repo.pool().execute(() -> {
                Vod full = App.get().repo.demoDetail(vod.id);
                if (full != null) App.get().repo.main().post(() -> { vod = full; bind(); });
            });
            return;
        }
        for (Site s : App.get().store.sites()) {
            if (s.key.equals(vod.siteKey)) {
                App.get().repo.detail(s, vod.id, new VodRepository.Cb<Vod>() {
                    @Override public void ok(Vod v) {
                        if (v.lines.isEmpty()) return;
                        vod = v;
                        bind();
                    }
                    @Override public void fail(String msg) {}
                });
                break;
            }
        }
    }

    private void play(int index) {
        if (vod.lines.isEmpty()) { Ui.toast(this, "暂无播放地址"); return; }
        epIndex = index;
        String url = vod.lines.get(lineIndex).episodes.get(index).url;
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("vod", vod);
        i.putExtra("line", lineIndex);
        i.putExtra("ep", index);
        startActivity(i);
    }

    /** 换源：在所有接口里搜索同名影片 */
    private void quickSearch() {
        Ui.toast(this, "正在其他源中搜索…");
        final java.util.List<Vod> found = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final java.util.List<Site> sites = new java.util.ArrayList<>();
        for (Site s : App.get().store.sites()) if (s.searchable == 1 && !"demo".equals(s.key)) sites.add(s);

        for (Vod v : App.get().repo.demoList("")) {
            if (v.name.equals(vod.name)) found.add(v);
        }
        if (sites.isEmpty()) { showQuickResults(found); return; }

        final java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(sites.size());
        for (Site s : sites) {
            App.get().repo.search(s, vod.name, 1, new VodRepository.Cb<List<Vod>>() {
                @Override public void ok(List<Vod> l) {
                    for (Vod x : l) if (x.name.equals(vod.name) && !x.lines.isEmpty()) found.add(x);
                    if (left.decrementAndGet() == 0) showQuickResults(found);
                }
                @Override public void fail(String msg) {
                    if (left.decrementAndGet() == 0) showQuickResults(found);
                }
            });
        }
    }

    private void showQuickResults(List<Vod> found) {
        if (isFinishing()) return;
        if (found.isEmpty()) { Ui.toast(this, "其他源没有找到同名影片"); return; }
        List<String> names = new ArrayList<>();
        for (Vod v : found) names.add(v.siteName + " · " + v.lines.size() + " 条线路");

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("换源播放");
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this, R.style.Theme_MovieBox_Dialog)
                .setView(v).create();
        com.zaka.moviebox.adapter.TextAdapter ad = new com.zaka.moviebox.adapter.TextAdapter((i, t) -> {
            DetailActivity.this.vod = found.get(i);
            lineIndex = 0; epIndex = 0;
            bind();
            loadDetail();
            d.dismiss();
        });
        ad.setData(names);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.show();
    }

    private void showIntro() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_intro, null);
        ((TextView) v.findViewById(R.id.title)).setText(vod.name + " · 简介");
        ((TextView) v.findViewById(R.id.desc)).setText(
                TextUtils.isEmpty(vod.desc) ? "暂无简介" : vod.desc);
        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this, R.style.Theme_MovieBox_Dialog)
                .setView(v).create();
        v.findViewById(R.id.btnOk).setOnClickListener(x -> d.dismiss());
        d.show();
    }
}
