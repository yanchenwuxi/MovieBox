package com.zaka.moviebox.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.VideoAdapter;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.net.VodRepository;
import com.zaka.moviebox.util.Store;
import com.zaka.moviebox.util.Ui;

import java.util.List;

public class GridFragment extends Fragment {
    private static final String ARG_KEY = "site", ARG_TYPE = "type", ARG_NAME = "name";

    private String siteKey, typeId, typeName;
    private VideoAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView empty;
    private int page = 1;
    private boolean loading = false, noMore = false;
    private Site site;

    public static GridFragment newInstance(String key, String type, String name) {
        GridFragment f = new GridFragment();
        Bundle b = new Bundle();
        b.putString(ARG_KEY, key);
        b.putString(ARG_TYPE, type);
        b.putString(ARG_NAME, name);
        f.setArguments(b);
        return f;
    }

    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_grid, p, false);
        Bundle a = getArguments();
        siteKey = a.getString(ARG_KEY, "");
        typeId = a.getString(ARG_TYPE, "");
        typeName = a.getString(ARG_NAME, "");

        refresh = v.findViewById(R.id.refreshLayout);
        empty = v.findViewById(R.id.tvEmpty);
        RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new VideoAdapter(this::openDetail);
        rv.setAdapter(adapter);
        refresh.setColorSchemeResources(R.color.primary);
        refresh.setOnRefreshListener(() -> { page = 1; noMore = false; load(); });

        v.findViewById(R.id.fab).setOnClickListener(x -> { page = 1; noMore = false; load(); });

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView r, int dx, int dy) {
                if (dy <= 0 || loading || noMore) return;
                GridLayoutManager lm = (GridLayoutManager) r.getLayoutManager();
                if (lm == null) return;
                if (lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 4) {
                    page++;
                    load();
                }
            }
        });

        load();
        return v;
    }

    private Store store() { return App.get().store; }

    private Site findSite() {
        for (Site s : store().sites()) if (s.key.equals(siteKey)) return s;
        return null;
    }

    private void load() {
        if (loading) return;
        loading = true;
        refresh.setRefreshing(page == 1);

        if ("demo".equals(siteKey)) {
            App.get().repo.pool().execute(() -> {
                List<Vod> l = App.get().repo.demoList("推荐".equals(typeName) ? "" : typeName);
                App.get().repo.main().post(() -> {
                    if (!isAdded()) return;
                    loading = false;
                    refresh.setRefreshing(false);
                    noMore = true;
                    adapter.setData(l);
                    empty.setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
                });
            });
            return;
        }

        site = findSite();
        if (site == null) {
            loading = false;
            refresh.setRefreshing(false);
            empty.setText("源已失效，请换源");
            empty.setVisibility(View.VISIBLE);
            return;
        }

        App.get().repo.list(site, typeId, page, new VodRepository.Cb<List<Vod>>() {
            @Override public void ok(List<Vod> l) {
                if (!isAdded()) return;
                loading = false;
                refresh.setRefreshing(false);
                if (page == 1) adapter.setData(l);
                else adapter.addData(l);
                if (l.size() < 10) noMore = true;
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void fail(String msg) {
                if (!isAdded()) return;
                loading = false;
                refresh.setRefreshing(false);
                if (page > 1) page--;
                if (adapter.getItemCount() == 0) {
                    empty.setText("加载失败，下拉重试 / 换源");
                    empty.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void openDetail(Vod v) {
        Intent i = new Intent(getContext(), DetailActivity.class);
        i.putExtra("vod", v);
        startActivity(i);
    }
}
