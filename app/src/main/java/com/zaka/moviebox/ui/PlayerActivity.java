package com.zaka.moviebox.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.EpisodeAdapter;
import com.zaka.moviebox.adapter.TextAdapter;
import com.zaka.moviebox.model.Episode;
import com.zaka.moviebox.model.PlayLine;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.player.KernelListener;
import com.zaka.moviebox.player.MoviePlayer;
import com.zaka.moviebox.util.Ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 阶段1：播放逻辑下沉到 MoviePlayer，Activity 只负责 UI 与业务编排。
 */
public class PlayerActivity extends AppCompatActivity {
    private MoviePlayer player;
    private PlayerView playerView;
    private Vod vod;
    private int lineIndex, epIndex;
    private String currentUrl;

    private View controlLayer, loadingView;
    private TextView tvTitle, tvCur, tvTotal, tvClock, tvError;
    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private boolean locked = false, showing = true;
    private boolean dragging = false;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.fullscreen(this);
        setContentView(R.layout.activity_player);

        vod = (Vod) getIntent().getSerializableExtra("vod");
        lineIndex = getIntent().getIntExtra("line", 0);
        epIndex = getIntent().getIntExtra("ep", 0);
        if (vod == null) { finish(); return; }

        bindViews();
        bindClicks();
        initPlayer();
        playEpisode(epIndex);
        ui.post(clock);
    }

    private void bindViews() {
        playerView = findViewById(R.id.playerView);
        controlLayer = findViewById(R.id.controlLayer);
        loadingView = findViewById(R.id.loadingView);
        tvTitle = findViewById(R.id.tvTitle);
        tvCur = findViewById(R.id.tvCurTime);
        tvTotal = findViewById(R.id.tvTotalTime);
        tvClock = findViewById(R.id.tvClock);
        tvError = findViewById(R.id.tvError);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void bindClicks() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> togglePlay());
        findViewById(R.id.btnNext).setOnClickListener(v -> playEpisode(epIndex + 1));
        findViewById(R.id.btnPrev).setOnClickListener(v -> playEpisode(epIndex - 1));
        findViewById(R.id.btnSpeed).setOnClickListener(v -> showSpeed());
        findViewById(R.id.btnScale).setOnClickListener(v -> showScale());
        findViewById(R.id.btnEpisodes).setOnClickListener(v -> showEpisodes());
        findViewById(R.id.btnLines).setOnClickListener(v -> showLines());
        findViewById(R.id.btnFullscreen).setOnClickListener(v -> toggleOrientation());
        findViewById(R.id.btnLock).setOnClickListener(v -> {
            locked = !locked;
            Ui.toast(this, locked ? "已锁定屏幕" : "已解锁");
            if (locked) hideControl();
        });

        final android.view.GestureDetector gd = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                        toggleControl();
                        return true;
                    }
                    @Override public boolean onDoubleTap(android.view.MotionEvent e) {
                        togglePlay();
                        return true;
                    }
                });
        playerView.setOnTouchListener((v, e) -> { gd.onTouchEvent(e); return true; });
        controlLayer.setOnClickListener(v -> toggleControl());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && player != null) {
                    long dur = player.getDuration();
                    if (dur > 0) tvCur.setText(fmt(dur * p / 1000));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                dragging = false;
                if (player != null) {
                    long dur = player.getDuration();
                    if (dur > 0) player.seekTo(dur * s.getProgress() / 1000);
                }
            }
        });
    }

    private void initPlayer() {
        player = new MoviePlayer(this);
        player.setKernelType(App.get().store.playKernel());
        player.setSpeed(App.get().store.speed());
        player.setScaleMode(App.get().store.scale());
        player.attachPlayerView(playerView);
        player.setListener(new KernelListener() {
            @Override public void onLoading() {
                loadingView.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.GONE);
            }

            @Override public void onPrepared() {
                loadingView.setVisibility(View.GONE);
                tvTotal.setText(fmt(player.getDuration()));
            }

            @Override public void onRenderingStart() {
                loadingView.setVisibility(View.GONE);
            }

            @Override public void onCompletion() {
                Ui.toast(PlayerActivity.this, "播放结束");
            }

            @Override public void onError(String message) {
                loadingView.setVisibility(View.GONE);
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("播放失败\n" + (message == null ? "" : message)
                        + "\n\n试试：换线路 / 用内置浏览器嗅探");
                tvError.setOnClickListener(v -> {
                    Intent i = new Intent(PlayerActivity.this, BrowserActivity.class);
                    i.putExtra("url", currentUrl);
                    startActivity(i);
                });
            }

            @Override public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        });
    }

    private void playEpisode(int idx) {
        if (vod.lines == null || vod.lines.isEmpty()) { finish(); return; }
        if (lineIndex < 0 || lineIndex >= vod.lines.size()) lineIndex = 0;
        PlayLine line = vod.lines.get(lineIndex);
        if (line.episodes == null || idx < 0 || idx >= line.episodes.size()) {
            Ui.toast(this, "没有更多集数了");
            return;
        }
        epIndex = idx;
        Episode ep = line.episodes.get(idx);
        currentUrl = ep.url;
        tvTitle.setText(vod.name + " · " + ep.name);

        if (!isDirect(currentUrl)) {
            loadingView.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("该地址需要解析\n点击此处用内置浏览器打开并嗅探");
            tvError.setOnClickListener(v -> {
                Intent i = new Intent(this, BrowserActivity.class);
                i.putExtra("url", currentUrl);
                startActivity(i);
            });
            return;
        }

        loadingView.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        player.setDataSource(currentUrl);
        player.prepare();
        long pos = App.get().store.lastPos(posKey());
        if (pos > 5000) player.seekTo(pos);
        player.play();

        App.get().store.addHistory(vod);
        savePosition();
    }

    private String posKey() {
        return vod.key() + "#" + lineIndex + "#" + epIndex;
    }

    private boolean isDirect(String u) {
        if (u == null) return false;
        String s = u.toLowerCase(Locale.US);
        return s.contains(".m3u8") || s.contains(".mp4") || s.contains(".flv")
                || s.contains(".mkv") || s.contains(".ts") || s.startsWith("rtmp")
                || s.contains(".m4v")
                || (s.startsWith("http") && (s.contains("stream") || s.contains("playlist")));
    }

    private void savePosition() {
        if (player == null) return;
        App.get().store.setLastPos(posKey(), player.getCurrentPosition());
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) player.pause();
        else player.play();
    }

    private void toggleControl() {
        if (locked && !showing) { Ui.toast(this, "屏幕已锁定"); return; }
        if (showing) hideControl(); else showControl();
    }

    private void showControl() {
        showing = true;
        controlLayer.setVisibility(View.VISIBLE);
        controlLayer.animate().alpha(1f).setDuration(150).start();
    }

    private void hideControl() {
        showing = false;
        controlLayer.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> controlLayer.setVisibility(View.GONE)).start();
    }

    private void toggleOrientation() {
        if (getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void showSpeed() {
        float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
        List<String> names = new ArrayList<>();
        for (float s : speeds) names.add(s + "x");
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("播放倍速");
        RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TextAdapter ad = new TextAdapter((i, t) -> {
            player.setSpeed(speeds[i]);
            App.get().store.setSpeed(speeds[i]);
            Ui.toast(this, "倍速 " + speeds[i] + "x");
            d.dismiss();
        });
        ad.setData(names);
        int sel = 2;
        for (int i = 0; i < speeds.length; i++) {
            if (Math.abs(speeds[i] - player.getSpeed()) < 0.01f) { sel = i; break; }
        }
        ad.setSelected(sel);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    private void showScale() {
        String[] names = {"默认", "填充", "原始", "裁剪"};
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("画面比例");
        RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TextAdapter ad = new TextAdapter((i, t) -> {
            player.setScaleMode(i);
            App.get().store.setScale(i);
            Ui.toast(this, names[i]);
            d.dismiss();
        });
        List<String> list = new ArrayList<>();
        for (String n : names) list.add(n);
        ad.setData(list);
        ad.setSelected(App.get().store.scale());
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    private void showEpisodes() {
        if (vod.lines.isEmpty()) return;
        PlayLine line = vod.lines.get(lineIndex);
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_episode, null);
        ((TextView) v.findViewById(R.id.title)).setText("选集 · " + line.name);
        TabLayout tabs = v.findViewById(R.id.tabLines);
        for (PlayLine l : vod.lines) {
            tabs.addTab(tabs.newTab().setText(l.name));
        }
        if (lineIndex < tabs.getTabCount()) {
            TabLayout.Tab tab = tabs.getTabAt(lineIndex);
            if (tab != null) tab.select();
        }

        RecyclerView rv = v.findViewById(R.id.rvEpisodes);
        rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 4));
        EpisodeAdapter epAdapter = new EpisodeAdapter(i -> {
            playEpisode(i);
            d.dismiss();
        });
        epAdapter.setData(line.episodes, epIndex);
        rv.setAdapter(epAdapter);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                lineIndex = t.getPosition();
                epAdapter.setData(vod.lines.get(lineIndex).episodes, 0);
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        d.setContentView(v);
        d.show();
    }

    private void showLines() {
        List<String> names = new ArrayList<>();
        for (PlayLine l : vod.lines) names.add(l.name + "（" + l.episodes.size() + "集）");
        BottomSheetDialog d = new BottomSheetDialog(this, R.style.Theme_MovieBox_BottomDialog);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("切换线路");
        RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        TextAdapter ad = new TextAdapter((i, t) -> {
            lineIndex = i;
            playEpisode(0);
            d.dismiss();
        });
        ad.setData(names);
        ad.setSelected(lineIndex);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    private final Runnable clock = new Runnable() {
        @Override public void run() {
            tvClock.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
            if (player != null && !dragging) {
                long dur = player.getDuration();
                long pos = player.getCurrentPosition();
                if (dur > 0) {
                    seekBar.setProgress((int) (pos * 1000 / dur));
                    tvCur.setText(fmt(pos));
                    tvTotal.setText(fmt(dur));
                }
            }
            ui.postDelayed(this, 800);
        }
    };

    private String fmt(long ms) {
        if (ms <= 0) return "00:00";
        int s = (int) (ms / 1000);
        int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.CHINA, "%d:%02d:%02d", h, m, sec)
                : String.format(Locale.CHINA, "%02d:%02d", m, sec);
    }

    @Override protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
        savePosition();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
