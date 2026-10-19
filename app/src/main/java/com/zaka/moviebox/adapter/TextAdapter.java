package com.zaka.moviebox.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zaka.moviebox.R;

import java.util.ArrayList;
import java.util.List;

public class TextAdapter extends RecyclerView.Adapter<TextAdapter.VH> {
    public interface OnClick { void onClick(int index, String text); }

    private final List<String> data = new ArrayList<>();
    private final List<String> subs = new ArrayList<>();
    private int selected = -1;
    private final OnClick click;

    public TextAdapter(OnClick c) { click = c; }

    public void setData(List<String> l) {
        data.clear(); subs.clear();
        if (l != null) data.addAll(l);
        notifyDataSetChanged();
    }

    public void setDataWithSub(List<String> l, List<String> s) {
        data.clear(); subs.clear();
        if (l != null) data.addAll(l);
        if (s != null) subs.addAll(s);
        notifyDataSetChanged();
    }

    public void setSelected(int i) { selected = i; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_text_sub, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        h.name.setText(data.get(pos));
        if (pos < subs.size() && subs.get(pos) != null && !subs.get(pos).isEmpty()) {
            h.sub.setVisibility(View.VISIBLE);
            h.sub.setText(subs.get(pos));
        } else h.sub.setVisibility(View.GONE);
        h.name.setTextColor(pos == selected
                ? h.name.getResources().getColor(R.color.primary)
                : h.name.getResources().getColor(R.color.text_main));
        h.itemView.setOnClickListener(v -> {
            selected = h.getAdapterPosition();
            notifyDataSetChanged();
            if (click != null) click.onClick(selected, data.get(selected));
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, sub;
        VH(View v) { super(v); name = v.findViewById(R.id.name); sub = v.findViewById(R.id.sub); }
    }
}
