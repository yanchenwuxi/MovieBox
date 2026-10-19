package com.zaka.moviebox.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zaka.moviebox.R;
import com.zaka.moviebox.model.LiveChannel;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {
    public interface OnClick { void onClick(LiveChannel c); }
    private final List<LiveChannel> data = new ArrayList<>();
    private final OnClick click;

    public ChannelAdapter(OnClick c) { click = c; }

    public void setData(List<LiveChannel> l) {
        data.clear();
        if (l != null) data.addAll(l);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_channel, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        LiveChannel c = data.get(pos);
        h.name.setText(c.name);
        h.group.setText(c.group);
        String logo = c.name.length() > 4 ? c.name.substring(0, 4) : c.name;
        h.logo.setText(logo);
        h.itemView.setOnClickListener(v -> { if (click != null) click.onClick(c); });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView logo, name, group;
        VH(View v) {
            super(v);
            logo = v.findViewById(R.id.tvLogo);
            name = v.findViewById(R.id.tvName);
            group = v.findViewById(R.id.tvGroup);
        }
    }
}
