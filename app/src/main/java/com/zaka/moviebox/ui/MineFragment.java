package com.zaka.moviebox.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.util.Ui;

public class MineFragment extends Fragment {
    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_mine, p, false);

        v.findViewById(R.id.btnBrowser).setOnClickListener(x ->
                startActivity(new Intent(getContext(), BrowserActivity.class)));

        ((TextView) v.findViewById(R.id.tvSourceCount)).setText(
                App.get().store.sites().size() + " 个");

        v.findViewById(R.id.itemSource).setOnClickListener(x ->
                startActivity(new Intent(getContext(), SettingsActivity.class).putExtra("page", "source")));

        v.findViewById(R.id.itemPlaySetting).setOnClickListener(x ->
                startActivity(new Intent(getContext(), SettingsActivity.class).putExtra("page", "play")));

        v.findViewById(R.id.itemHomeSetting).setOnClickListener(x ->
                startActivity(new Intent(getContext(), SettingsActivity.class).putExtra("page", "home")));

        v.findViewById(R.id.itemClear).setOnClickListener(x -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("清理缓存")
                    .setMessage("将清空播放历史、搜索历史与图片缓存，收藏会保留。")
                    .setPositiveButton("清理", (d, w) -> {
                        App.get().store.clearHistory();
                        App.get().store.clearSearchHistory();
                        Ui.toast(getContext(), "已清理");
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        v.findViewById(R.id.itemAbout).setOnClickListener(x -> {
            startActivity(new Intent(getContext(), SettingsActivity.class).putExtra("page", "about"));
        });

        v.findViewById(R.id.itemUpdate).setOnClickListener(x ->
                Ui.toast(getContext(), "当前已是最新版本 v1.0"));
        return v;
    }
}
