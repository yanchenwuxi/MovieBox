package com.zaka.moviebox.util;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;

public class Ui {
    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static void toast(Context c, String msg) {
        try {
            android.widget.Toast t = android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_SHORT);
            t.show();
        } catch (Throwable ignored) {}
    }

    public static void statusBarPadding(Context c, View v) {
        if (v == null) return;
        int h = statusBarHeight(c);
        v.setPadding(v.getPaddingLeft(), h, v.getPaddingRight(), v.getPaddingBottom());
    }

    public static int statusBarHeight(Context c) {
        int res = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return res > 0 ? c.getResources().getDimensionPixelSize(res) : (int) (24 * c.getResources().getDisplayMetrics().density);
    }

    public static void fullscreen(Activity a) {
        a.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View d = a.getWindow().getDecorView();
        d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
