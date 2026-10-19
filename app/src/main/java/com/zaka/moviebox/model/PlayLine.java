package com.zaka.moviebox.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayLine implements Serializable {
    public String name;
    public List<Episode> episodes = new ArrayList<>();
    public PlayLine(String n) { name = n; }
}
