package com.zaka.moviebox.net;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Http {
    private static OkHttpClient client;
    public static final String UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";

    public static OkHttpClient client() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build();
        }
        return client;
    }

    public static void getAsync(String url, Callback cb) {
        getAsync(url, null, cb);
    }

    public static void getAsync(String url, String referer, Callback cb) {
        Request.Builder b = new Request.Builder().url(url).header("User-Agent", UA)
                .header("Accept", "*/*");
        if (referer != null && !referer.isEmpty()) b.header("Referer", referer);
        client().newCall(b.build()).enqueue(cb);
    }

    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, String referer) throws IOException {
        Request.Builder b = new Request.Builder().url(url).header("User-Agent", UA);
        if (referer != null && !referer.isEmpty()) b.header("Referer", referer);
        Response r = client().newCall(b.build()).execute();
        try {
            return r.body() == null ? "" : r.body().string();
        } finally {
            r.close();
        }
    }
}
