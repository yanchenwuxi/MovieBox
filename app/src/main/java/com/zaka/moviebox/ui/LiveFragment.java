package com.zaka.moviebox.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaka.moviebox.App;
import com.zaka.moviebox.R;
import com.zaka.moviebox.adapter.ChannelAdapter;
import com.zaka.moviebox.model.LiveChannel;

import java.util.ArrayList;
import java.util.List;

public class LiveFragment extends Fragment {
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView tvName, tvEpg;
    private ChannelAdapter adapter;
    private final List<LiveChannel> channels = new ArrayList<>();

    @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup p, Bundle b) {
        View v = inf.inflate(R.layout.fragment_live, p, false);
        playerView = v.findViewById(R.id.playerView);
        tvName = v.findViewById(R.id.tvLiveName);
        tvEpg = v.findViewById(R.id.tvEpg);

        RecyclerView rv = v.findViewById(R.id.rvChannels);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChannelAdapter(this::play);
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnFull).setOnClickListener(x -> {
            android.content.Intent i = new android.content.Intent(getContext(), PlayerActivity.class);
            com.zaka.moviebox.model.Vod vod = new com.zaka.moviebox.model.Vod();
            vod.name = tvName.getText().toString();
            vod.siteKey = "live";
            vod.id = "live";
            com.zaka.moviebox.model.PlayLine line = new com.zaka.moviebox.model.PlayLine("直播");
            line.episodes.add(new com.zaka.moviebox.model.Episode("直播", currentUrl));
            vod.lines.add(line);
            i.putExtra("vod", vod);
            startActivity(i);
        });

        loadChannels();
        return v;
    }

    private String currentUrl = "";

    private void loadChannels() {
        channels.clear();
        try {
            String body = App.get().repo.readAsset("live.json");
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("channels");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                channels.add(new LiveChannel(o.get("name").getAsString(),
                        o.get("url").getAsString(),
                        o.has("group") ? o.get("group").getAsString() : "默认"));
            }
        } catch (Throwable ignored) {}
        adapter.setData(channels);
        if (!channels.isEmpty()) play(channels.get(0));
    }

    private void play(LiveChannel c) {
        currentUrl = c.url;
        tvName.setText(c.name);
        tvEpg.setText("正在直播 · " + c.group);
        if (player == null) {
            player = new ExoPlayer.Builder(requireContext()).build();
            playerView.setPlayer(player);
        }
        player.setMediaItem(MediaItem.fromUri(c.url));
        player.prepare();
        player.play();
    }

    @Override public void onPause() { super.onPause(); if (player != null) player.pause(); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (player != null) { player.release(); player = null; }
    }
}
