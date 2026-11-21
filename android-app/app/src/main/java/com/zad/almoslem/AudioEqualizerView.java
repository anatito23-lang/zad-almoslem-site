package com.zad.almoslem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Lightweight playback animation driven by the actual shared audio state. */
public final class AudioEqualizerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] kinds;
    private long frame = 0;

    public AudioEqualizerView(Context context, String... activeKinds) {
        super(context);
        kinds = activeKinds == null ? new String[0] : activeKinds.clone();
        paint.setColor(android.graphics.Color.rgb(50,199,181));
        setMinimumHeight(dp(34));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean isPlaying() {
        SharedPreferences p = getContext().getSharedPreferences("zad_audio", Context.MODE_PRIVATE);
        if (!p.getBoolean("radio_playing", false) || p.getBoolean("radio_paused", false)) return false;
        String active = p.getString("active_kind", "");
        for (String kind : kinds) if (kind.equals(active)) return true;
        return false;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = 9;
        float density = getResources().getDisplayMetrics().density;
        float barWidth = dp(5);
        float gap = dp(7);
        float total = count * barWidth + (count - 1) * gap;
        float left = (getWidth() - total) / 2f;
        float centerY = getHeight() / 2f;
        boolean playing = isPlaying();
        for (int i = 0; i < count; i++) {
            double phase = (frame * 0.72) + i * 0.78;
            float height = playing ? dp(7) + (float) (Math.abs(Math.sin(phase)) * dp(23)) : dp(5);
            float x = left + i * (barWidth + gap);
            canvas.drawRoundRect(x, centerY - height / 2f, x + barWidth,
                    centerY + height / 2f, dp(3), dp(3), paint);
        }
        if (playing) {
            frame++;
            postInvalidateDelayed(130);
        }
    }
}
