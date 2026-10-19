package com.zaka.moviebox.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.VideoAdapter;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.net.VodRepository;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchActivity extends AppCompatActivity {
    private EditText et;
    private View hintLayout, resultLayout;
    private TextView stat;
    private VideoAdapter adapter;
    private ChipGroup chipHistory, chipHot;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_search);
        Ui.statusBarPadding(this, findViewById(R.id.layoutHint).getRootView());

        et = findViewById(R.id.etSearch);
        hintLayout = findViewById(R.id.layoutHint);
        resultLayout = findViewById(R.id.layoutResult);
        stat = findViewById(R.id.tvSearchStat);
        chipHistory = findViewById(R.id.chipHistory);
        chipHot = findViewById(R.id.chipHot);

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new VideoAdapter(v -> {
            Intent i = new Intent(this, DetailActivity.class);
            i.putExtra("vod", v);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> doSearch(et.getText().toString()));
        findViewById(R.id.btnClear).setOnClickListener(v -> { et.setText(""); });
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> {
            App.get().store.clearSearchHistory();
            buildHistory();
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                findViewById(R.id.btnClear).setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        et.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch(et.getText().toString());
                return true;
            }
            return false;
        });

        buildHistory();
        buildHot();
    }

    private void buildHistory() {
        chipHistory.removeAllViews();
        List<String> h = App.get().store.searchHistory();
        if (h.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("暂无搜索历史");
            t.setTextColor(getColor(R.color.text_light));
            t.setTextSize(12);
            chipHistory.addView(t);
            return;
        }
        for (String s : h) {
            Chip c = new Chip(this);
            c.setText(s);
            c.setOnClickListener(v -> { et.setText(s); doSearch(s); });
            chipHistory.addView(c);
        }
    }

    private void buildHot() {
        chipHot.removeAllViews();
        String[] hot = {"庆余年", "狂飙", "三体", "甄嬛传", "流浪地球", "繁花", "漫长的季节", "狂野时代"};
        for (String s : hot) {
            Chip c = new Chip(this);
            c.setText(s);
            c.setOnClickListener(v -> { et.setText(s); doSearch(s); });
            chipHot.addView(c);
        }
    }

    private void doSearch(String kw) {
        kw = kw == null ? "" : kw.trim();
        if (kw.isEmpty()) { Ui.toast(this, "请输入搜索内容"); return; }
        App.get().store.addSearchHistory(kw);
        buildHistory();

        hintLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        adapter.setData(new ArrayList<>());

        List<Site> sites = new ArrayList<>();
        for (Site s : App.get().store.sites())
            if (s.searchable == 1 && !"demo".equals(s.key)) sites.add(s);

        final int total = sites.size() + 1;
        AtomicInteger done = new AtomicInteger(0);
        stat.setText("搜索中 0/" + total);

        final String keyword = kw;

        // 演示源
        App.get().repo.pool().execute(() -> {
            List<Vod> demo = new ArrayList<>();
            for (Vod v : App.get().repo.demoList("")) {
                if (v.name.contains(keyword)) demo.add(v);
            }
            App.get().repo.main().post(() -> {
                adapter.addData(demo);
                int d = done.incrementAndGet();
                stat.setText("搜索完成 " + d + "/" + total + " · 已找到 " + adapter.getItemCount());
            });
        });

        for (Site s : sites) {
            App.get().repo.search(s, keyword, 1, new VodRepository.Cb<List<Vod>>() {
                @Override public void ok(List<Vod> l) {
                    adapter.addData(l);
                    int d = done.incrementAndGet();
                    stat.setText("搜索完成 " + d + "/" + total + " · 已找到 " + adapter.getItemCount()
                            + (adapter.getItemCount() == 0 && d == total ? "（换个词试试）" : ""));
                }
                @Override public void fail(String msg) {
                    int d = done.incrementAndGet();
                    stat.setText("搜索完成 " + d + "/" + total);
                }
            });
        }
    }
}
