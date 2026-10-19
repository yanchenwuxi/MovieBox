package com.zaka.moviebox.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.VideoAdapter;
import com.zaka.moviebox.model.Vod;

import java.util.List;

public class HistoryFragment extends Fragment {
    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_history, p, false);
        TabLayout tab = v.findViewById(R.id.tabLayout);
        RecyclerView rv = v.findViewById(R.id.rvList);
        // 兼容布局里可能没有 rvList 的情况
        View rvView = v.findViewById(R.id.rvList);
        if (rvView instanceof RecyclerView) rv = (RecyclerView) rvView;

        tab.addTab(tab.newTab().setText("播放历史"));
        tab.addTab(tab.newTab().setText("我的收藏"));

        RecyclerView finalRv = rv;
        finalRv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        VideoAdapter ad = new VideoAdapter(x -> {
            android.content.Intent i = new android.content.Intent(getContext(), DetailActivity.class);
            i.putExtra("vod", x);
            startActivity(i);
        });
        finalRv.setAdapter(ad);

        Runnable refresh = () -> {
            List<Vod> list = tab.getSelectedTabPosition() == 0
                    ? App.get().store.history() : App.get().store.collects();
            if (tab.getSelectedTabPosition() == 0) {
                for (Vod x : list) {
                    String label = App.get().store.lastWatchLabel(x.key());
                    if (label != null && !label.isEmpty()) x.note = label;
                }
            }
            ad.setData(list);
        };
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { refresh.run(); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        refresh.run();
        return v;
    }
}
