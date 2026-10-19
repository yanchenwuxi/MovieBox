package com.zaka.moviebox;

import android.app.Application;

import com.zaka.moviebox.net.VodRepository;
import com.zaka.moviebox.util.Store;

public class App extends Application {
    private static App instance;
    public static App get() { return instance; }
    public Store store;
    public VodRepository repo;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        store = new Store(this);
        repo = new VodRepository(this);
    }
}
