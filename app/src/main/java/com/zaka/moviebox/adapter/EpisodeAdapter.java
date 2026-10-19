package com.zaka.moviebox.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zaka.moviebox.R;
import com.zaka.moviebox.model.Episode;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.VH> {
    public interface OnClick { void onClick(int index); }
    private final List<Episode> data = new ArrayList<>();
    private int selected = 0;
    private final OnClick click;

    public EpisodeAdapter(OnClick c) { click = c; }

    public void setData(List<Episode> l, int sel) {
        data.clear();
        if (l != null) data.addAll(l);
        selected = sel;
        notifyDataSetChanged();
    }

    public void setSelected(int i) { selected = i; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_episode, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Episode e = data.get(pos);
        h.tv.setText(e.name);
        if (pos == selected) {
            h.tv.setBackgroundResource(R.drawable.bg_item_selected);
            h.tv.setTextColor(Color.WHITE);
        } else {
            h.tv.setBackgroundResource(R.drawable.bg_item_normal);
            h.tv.setTextColor(h.tv.getResources().getColor(R.color.text_main));
        }
        h.tv.setOnClickListener(v -> {
            selected = h.getAdapterPosition();
            notifyDataSetChanged();
            if (click != null) click.onClick(selected);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(View v) { super(v); tv = v.findViewById(R.id.name); }
    }
}
