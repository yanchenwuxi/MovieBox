package com.zaka.moviebox.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.zaka.moviebox.R;
import com.zaka.moviebox.model.Vod;
import com.zaka.moviebox.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VH> {
    public interface OnClick { void onClick(Vod v); }

    private final List<Vod> data = new ArrayList<>();
    private final OnClick click;
    private final float ratio;
    private int cardW = 0;

    public VideoAdapter(OnClick c) { this(c, 280f / 220f); }

    public VideoAdapter(OnClick c, float hRatio) { click = c; ratio = hRatio; }

    public void setData(List<Vod> l) {
        data.clear();
        if (l != null) data.addAll(l);
        notifyDataSetChanged();
    }

    public void addData(List<Vod> l) {
        if (l == null) return;
        int s = data.size();
        data.addAll(l);
        notifyItemRangeInserted(s, l.size());
    }

    public List<Vod> getData() { return data; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_video_grid, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Vod v = data.get(pos);
        h.name.setText(v.name);

        if (cardW == 0) {
            int sw = h.itemView.getResources().getDisplayMetrics().widthPixels;
            int span = 3;
            cardW = (sw - Ui.dp(h.itemView.getContext(), 10)) / span - Ui.dp(h.itemView.getContext(), 6);
        }
        ViewGroup.LayoutParams lp = h.card.getLayoutParams();
        if (lp != null) {
            lp.height = (int) (cardW * ratio);
            h.card.setLayoutParams(lp);
        }

        String note = v.note == null ? "" : v.note;
        boolean isFlag = note.contains("更新");
        if (!TextUtils.isEmpty(note) && !isFlag) {
            h.note.setVisibility(View.VISIBLE);
            h.note.setText(note);
        } else h.note.setVisibility(View.GONE);

        if (isFlag) {
            h.flag.setVisibility(View.VISIBLE);
            h.flag.setText(note);
        } else h.flag.setVisibility(View.GONE);

        String pic = v.pic;
        if (pic != null && pic.startsWith("assets/")) {
            Glide.with(h.iv.getContext()).load("file:///android_asset/" + pic.substring(7))
                    .apply(new RequestOptions().transform(new RoundedCorners(Ui.dp(h.itemView.getContext(), 8))))
                    .placeholder(R.drawable.bg_img_placeholder).into(h.iv);
        } else {
            Glide.with(h.iv.getContext()).load(pic)
                    .apply(new RequestOptions().transform(new RoundedCorners(Ui.dp(h.itemView.getContext(), 8))))
                    .placeholder(R.drawable.bg_img_placeholder).into(h.iv);
        }

        h.itemView.setOnClickListener(x -> { if (click != null) click.onClick(v); });
    }

    private String flagOf(Vod v) {
        if (!TextUtils.isEmpty(v.note) && (v.note.contains("更新") || v.note.contains("集"))) return v.note;
        return "";
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout card;
        ImageView iv;
        TextView name, note, flag;
        VH(View v) {
            super(v);
            card = v.findViewById(R.id.thumbCard);
            iv = v.findViewById(R.id.ivThumb);
            name = v.findViewById(R.id.tvName);
            note = v.findViewById(R.id.tvNote);
            flag = v.findViewById(R.id.tvFlag);
        }
    }
}
