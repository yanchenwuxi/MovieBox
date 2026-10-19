package com.zaka.moviebox.player;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * 播放页手势：左半屏亮度、右半屏音量、横向快进快退、长按临时倍速。
 */
public class GestureHelper {
    public interface Callback {
        long getPosition();
        long getDuration();
        void seekTo(long ms);
        boolean isPlaying();
        void setSpeed(float speed);
        float getSpeed();
    }

    private final Activity activity;
    private final AudioManager audio;
    private final Callback cb;
    private TextView tipView;
    private boolean enabled = true;

    private float downX, downY;
    private int area; // 0 none 1 brightness 2 volume 3 seek
    private long seekStartPos;
    private int volStart;
    private float brightStart;
    private float normalSpeed = 1f;
    private boolean longPressBoost;
    private final android.view.GestureDetector detector;

    public GestureHelper(Activity activity, Callback cb) {
        this.activity = activity;
        this.cb = cb;
        this.audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        detector = new android.view.GestureDetector(activity,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override public void onLongPress(MotionEvent e) {
                        if (!enabled) return;
                        longPressBoost = true;
                        normalSpeed = cb.getSpeed();
                        cb.setSpeed(Math.max(normalSpeed, 2.0f));
                        showTip("长按倍速 " + cb.getSpeed() + "x");
                    }
                });
    }

    public void setTipView(TextView tip) { this.tipView = tip; }
    public void setEnabled(boolean e) { this.enabled = e; }

    public boolean onTouch(MotionEvent e) {
        if (!enabled) return false;
        detector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                area = 0;
                seekStartPos = cb.getPosition();
                volStart = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                brightStart = getBrightness();
                longPressBoost = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (area == 0) {
                    if (Math.abs(dx) < 12 && Math.abs(dy) < 12) break;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        area = 3; // seek
                    } else {
                        int w = activity.findViewById(android.R.id.content).getWidth();
                        area = downX < w / 2f ? 1 : 2;
                    }
                }
                if (area == 1) {
                    float delta = -dy / 600f;
                    setBrightness(clamp(brightStart + delta, 0.01f, 1f));
                    showTip("亮度 " + (int) (getBrightness() * 100) + "%");
                } else if (area == 2) {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int delta = (int) (-dy / 400f * max);
                    int v = clamp(volStart + delta, 0, max);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
                    showTip("音量 " + (max == 0 ? 0 : v * 100 / max) + "%");
                } else if (area == 3) {
                    long dur = cb.getDuration();
                    if (dur <= 0) break;
                    long offset = (long) (dx / 300f * 15000); // 300px ~ 15s
                    long target = clamp(seekStartPos + offset, 0, dur);
                    showTip((offset >= 0 ? "快进 " : "快退 ") + format(Math.abs(offset))
                            + "\n" + format(target) + " / " + format(dur));
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (area == 3) {
                    long dur = cb.getDuration();
                    if (dur > 0) {
                        float dx = e.getX() - downX;
                        long offset = (long) (dx / 300f * 15000);
                        cb.seekTo(clamp(seekStartPos + offset, 0, dur));
                    }
                }
                if (longPressBoost) {
                    cb.setSpeed(normalSpeed);
                    longPressBoost = false;
                }
                hideTip();
                area = 0;
                break;
        }
        return area != 0 || longPressBoost;
    }

    private float getBrightness() {
        Window w = activity.getWindow();
        float b = w.getAttributes().screenBrightness;
        if (b < 0) {
            try {
                b = Settings.System.getInt(activity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS) / 255f;
            } catch (Exception e) {
                b = 0.5f;
            }
        }
        return b;
    }

    private void setBrightness(float v) {
        Window w = activity.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = v;
        w.setAttributes(lp);
    }

    private void showTip(String s) {
        if (tipView == null) return;
        tipView.setText(s);
        tipView.setVisibility(TextView.VISIBLE);
    }

    private void hideTip() {
        if (tipView == null) return;
        tipView.setVisibility(TextView.GONE);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String format(long ms) {
        if (ms <= 0) return "00:00";
        int s = (int) (ms / 1000);
        int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec)
                : String.format("%02d:%02d", m, sec);
    }
}
