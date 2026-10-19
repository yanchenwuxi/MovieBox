package com.zaka.moviebox.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.zaka.moviebox.R;

public class ShortFragment extends Fragment {
    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_short, p, false);
        if (b == null && getChildFragmentManager().findFragmentById(R.id.shortContainer) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.shortContainer, GridFragment.newInstance("demo", "短剧", "短剧"))
                    .commitAllowingStateLoss();
        }
        return v;
    }
}
