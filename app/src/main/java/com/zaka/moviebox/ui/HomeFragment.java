package com.zaka.moviebox.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.model.Site;
import com.zaka.moviebox.util.Store;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private TabLayout tab;
    private ViewPager2 vp;
    private final List<String[]> channels = new ArrayList<>();
    private final List<Fragment> pages = new ArrayList<>();
    private Site site;
    private TabLayoutMediator mediator;

    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_home, p, false);
        tab = v.findViewById(R.id.tabChannel);
        vp = v.findViewById(R.id.vpContent);
        vp.setOffscreenPageLimit(1);

        v.findViewById(R.id.searchBar).setOnClickListener(x ->
                startActivity(new Intent(getContext(), SearchActivity.class)));
        v.findViewById(R.id.btnSite).setOnClickListener(x -> showSites());
        v.findViewById(R.id.btnAdd).setOnClickListener(x -> showAddSource());

        loadSite();
        return v;
    }

    private Store store() { return App.get().store; }

    private void loadSite() {
        List<Site> sites = store().sites();
        if (sites.isEmpty()) {
            Site d = new Site("demo", "演示片源", "asset://demo");
            sites.add(d);
            store().saveSites(sites);
        }
        String key = store().lastSiteKey();
        site = null;
        for (Site s : sites) if (s.key.equals(key)) site = s;
        if (site == null) site = sites.get(0);

        buildChannels(site);
    }

    private void buildChannels(Site s) {
        channels.clear();
        pages.clear();

        if ("demo".equals(s.key)) {
            channels.add(new String[]{"", "推荐"});
            channels.add(new String[]{"电影", "电影"});
            channels.add(new String[]{"电视剧", "电视剧"});
            channels.add(new String[]{"综艺", "综艺"});
            channels.add(new String[]{"动漫", "动漫"});
            applyChannels();
            return;
        }

        App.get().repo.categories(s, new com.zaka.moviebox.net.VodRepository.Cb<List<String[]>>() {
            @Override public void ok(List<String[]> l) {
                if (!isAdded()) return;
                channels.clear();
                channels.add(new String[]{"", "推荐"});
                channels.addAll(l);
                applyChannels();
            }
            @Override public void fail(String msg) {
                if (!isAdded()) return;
                channels.clear();
                channels.add(new String[]{"", "推荐"});
                applyChannels();
            }
        });
    }

    private void applyChannels() {
        pages.clear();
        for (String[] c : channels) pages.add(GridFragment.newInstance(site.key, c[0], c[1]));

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) { return pages.get(pos); }
            @Override public int getItemCount() { return pages.size(); }
        });
        if (mediator != null) mediator.detach();
        mediator = new TabLayoutMediator(tab, vp, (t, pos) -> t.setText(channels.get(pos)[1]));
        mediator.attach();
    }

    private void showSites() {
        List<Site> sites = store().sites();
        List<String> names = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        for (Site s : sites) { names.add(s.name); subs.add(s.api); }
        int sel = sites.indexOf(site);

        com.google.android.material.bottomsheet.BottomSheetDialog d =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_setting, null);
        ((TextView) v.findViewById(R.id.title)).setText("选择接口源");
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        com.zaka.moviebox.adapter.TextAdapter ad = new com.zaka.moviebox.adapter.TextAdapter((i, t) -> {
            site = sites.get(i);
            store().setLastSiteKey(site.key);
            buildChannels(site);
            d.dismiss();
        });
        ad.setDataWithSub(names, subs);
        ad.setSelected(sel);
        rv.setAdapter(ad);
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        d.setContentView(v);
        d.show();
    }

    private void showAddSource() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(requireContext());
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_source, null);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.recyclerView);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

        List<Site> sites = store().sites();
        com.zaka.moviebox.adapter.TextAdapter ad = new com.zaka.moviebox.adapter.TextAdapter((i, t) -> {});
        List<String> n = new ArrayList<>(), s = new ArrayList<>();
        for (Site x : sites) { n.add(x.name); s.add(x.api); }
        ad.setDataWithSub(n, s);
        rv.setAdapter(ad);

        android.app.AlertDialog dlg = b.setView(v).create();
        v.findViewById(R.id.btnCancel).setOnClickListener(x -> dlg.dismiss());
        v.findViewById(R.id.btnAdd).setOnClickListener(x -> {
            android.widget.EditText et = v.findViewById(R.id.etUrl);
            String url = et.getText().toString().trim();
            if (url.isEmpty()) { Ui.toast(getContext(), "请输入接口地址"); return; }
            dlg.dismiss();
            addSource(url);
        });
        dlg.show();
    }

    private void addSource(String url) {
        Ui.toast(getContext(), "正在加载接口…");
        App.get().repo.loadConfig(url, new com.zaka.moviebox.net.VodRepository.Cb<List<Site>>() {
            @Override public void ok(List<Site> l) {
                List<Site> sites = store().sites();
                sites.addAll(l);
                store().saveSites(sites);
                store().setConfigUrl(url);
                Ui.toast(getContext(), "已添加 " + l.size() + " 个源");
                loadSite();
            }
            @Override public void fail(String msg) {
                // 当作裸 CMS 地址直接加
                List<Site> sites = store().sites();
                Site s = new Site("cms_" + System.currentTimeMillis(), "自定义源", url);
                sites.add(s);
                store().saveSites(sites);
                Ui.toast(getContext(), "已添加为 CMS 源");
                loadSite();
            }
        });
    }

    public boolean onBackPressed() { return false; }
}
