package com.zaka.moviebox.model;

import java.io.Serializable;

public class Episode implements Serializable {
    public String name;
    public String url;
    public Episode(String n, String u) { name = n; url = u; }
}
