package com.zaka.moviebox.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.zaka.moviebox.R;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 vp;
    private final List<Fragment> fragments = new ArrayList<>();
    private View[] tabs;
    private ImageView[] icons;
    private TextView[] texts;
    private int cur = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        vp = findViewById(R.id.vp_main);
        vp.setPadding(0, Ui.statusBarHeight(this), 0, 0);

        vp.setUserInputEnabled(false);
        vp.setOffscreenPageLimit(3);

        fragments.add(new HomeFragment());
        fragments.add(new LiveFragment());
        fragments.add(new ShortFragment());
        fragments.add(new HistoryFragment());
        fragments.add(new MineFragment());

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) { return fragments.get(pos); }
            @Override public int getItemCount() { return fragments.size(); }
        });

        tabs = new View[]{findViewById(R.id.tab_home), findViewById(R.id.tab_live),
                findViewById(R.id.tabVideoContainer), findViewById(R.id.tab_history), findViewById(R.id.tab_setting)};
        icons = new ImageView[]{findViewById(R.id.ic_home), findViewById(R.id.ic_live),
                findViewById(R.id.ic_video), findViewById(R.id.ic_history), findViewById(R.id.ic_mine)};
        texts = new TextView[]{findViewById(R.id.tv_home), findViewById(R.id.tv_live),
                findViewById(R.id.tv_video), findViewById(R.id.tv_history), findViewById(R.id.tv_mine)};

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setOnClickListener(v -> switchTo(idx));
        }
        switchTo(0);
    }

    public void switchTo(int i) {
        cur = i;
        vp.setCurrentItem(i, false);
        int on = getColor(R.color.primary);
        int off = Color.parseColor("#FF999999");
        for (int k = 0; k < icons.length; k++) {
            icons[k].setColorFilter(k == i ? on : off);
            texts[k].setTextColor(k == i ? on : off);
            texts[k].getPaint().setFakeBoldText(k == i);
        }
    }

    @Override public void onBackPressed() {
        Fragment f = fragments.get(cur);
        if (f instanceof HomeFragment && ((HomeFragment) f).onBackPressed()) return;
        super.onBackPressed();
    }
}
