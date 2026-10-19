package com.zaka.moviebox.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.player.KernelFactory;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private LinearLayout container;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        Ui.statusBarPadding(this, findViewById(R.id.title));

        container = findViewById(R.id.container);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String page = getIntent().getStringExtra("page");
        if ("source".equals(page)) { setTitle("接口 / 源管理"); buildSource(); }
        else if ("about".equals(page)) { setTitle("关于"); buildAbout(); }
        else if ("play".equals(page)) { setTitle("播放设置"); buildPlay(); }
        else { setTitle("首页显示"); buildHome(); }
    }

    private void setTitle(String s) { ((TextView) findViewById(R.id.title)).setText(s); }

    private View item(String title, String value, Runnable click) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_setting, container, false);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        TextView tv = v.findViewById(R.id.value);
        tv.setText(value == null ? "" : value);
        v.setOnClickListener(x -> { if (click != null) click.run(); });
        container.addView(v);
        return v;
    }

    private void buildSource() {
        List<Site> sites = App.get().store.sites();
        item("当前源数量", sites.size() + " 个", null);
        item("添加接口地址", "支持 TVBox json / CMS 链接", this::showAddDialog);
        item("恢复演示源", "首次可用", () -> {
            List<Site> l = App.get().store.sites();
            for (Site s : l) if ("demo".equals(s.key)) { Ui.toast(this, "已存在"); return; }
            l.add(0, new Site("demo", "演示片源", "asset://demo"));
            App.get().store.saveSites(l);
            Ui.toast(this, "已恢复");
        });
        item("清空全部源", "谨慎操作", () -> {
            List<Site> l = new ArrayList<>();
            l.add(new Site("demo", "演示片源", "asset://demo"));
            App.get().store.saveSites(l);
            Ui.toast(this, "已清空并保留演示源");
            recreate();
        });
    }

    private void showAddDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_source, null);
        android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
                .setView(v).create();
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        v.findViewById(R.id.btnAdd).setOnClickListener(x -> {
            String url = ((android.widget.EditText) v.findViewById(R.id.etUrl)).getText().toString().trim();
            if (url.isEmpty()) { Ui.toast(this, "请输入地址"); return; }
            d.dismiss();
            App.get().repo.loadConfig(url, new com.zaka.moviebox.net.VodRepository.Cb<List<Site>>() {
                @Override public void ok(List<Site> l) {
                    List<Site> sites = App.get().store.sites();
                    sites.addAll(l);
                    App.get().store.saveSites(sites);
                    Ui.toast(SettingsActivity.this, "已添加 " + l.size() + " 个源");
                    recreate();
                }
                @Override public void fail(String msg) {
                    List<Site> sites = App.get().store.sites();
                    sites.add(new Site("cms_" + System.currentTimeMillis(), "自定义源", url));
                    App.get().store.saveSites(sites);
                    Ui.toast(SettingsActivity.this, "已作为 CMS 源添加");
                    recreate();
                }
            });
        });
        d.show();
    }

    private void buildPlay() {
        int kernel = App.get().store.playKernel();
        item("播放器内核", KernelFactory.displayName(kernel), () -> {
            String[] names = KernelFactory.optionNames();
            int[] opts = KernelFactory.OPTIONS;
            new android.app.AlertDialog.Builder(this)
                    .setTitle("播放器内核")
                    .setItems(names, (d, w) -> {
                        App.get().store.setPlayKernel(opts[w]);
                        Ui.toast(this, "已切换为 " + names[w]);
                        recreate();
                    }).show();
        });
        item("默认倍速", App.get().store.speed() + "x", () -> {
            String[] s = {"0.75", "1.0", "1.25", "1.5", "2.0"};
            float[] f = {0.75f, 1f, 1.25f, 1.5f, 2f};
            new android.app.AlertDialog.Builder(this).setTitle("默认倍速")
                    .setItems(s, (d, w) -> { App.get().store.setSpeed(f[w]); recreate(); }).show();
        });
        String[] scaleNames = {"默认", "填充", "原始", "裁剪"};
        int scale = App.get().store.scale();
        item("画面比例", scaleNames[Math.max(0, Math.min(scale, scaleNames.length - 1))], () ->
                new android.app.AlertDialog.Builder(this).setTitle("画面比例")
                        .setItems(scaleNames, (d, w) -> {
                            App.get().store.setScale(w);
                            recreate();
                        }).show()
        );
        item("IJK 硬解 / 软解", "阶段2 开放", null);
        item("手势调节亮度音量", "阶段3 开放", null);
        item("说明", "阶段1 已完成多内核架构，当前可用 Exo / 自动", null);
    }

    private void buildHome() {
        item("首页频道", "跟随接口分类", null);
        item("每行卡片数", "3 列", null);
        item("启动页", "主页", null);
        item("深色模式", "跟随系统", null);
    }

    private void buildAbout() {
        item("版本", "v1.1-phase1（播放器架构升级）", null);
        item("内置浏览器", "支持网页视频嗅探", () -> startActivity(
                new android.content.Intent(this, BrowserActivity.class)));
        item("说明", "本应用不含任何广告 SDK，数据来自用户自行配置的采集接口。", null);
        item("免责声明", "仅供个人学习研究，请勿用于商业用途。", null);
    }
}
