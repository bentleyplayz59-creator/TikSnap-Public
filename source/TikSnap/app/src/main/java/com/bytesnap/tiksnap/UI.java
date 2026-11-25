package com.bytesnap.tiksnap;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class T {
    static boolean dark;
    static int bg, surface, surface2, text, sub, divider;
    static final int PURPLE = 0xFF8A2BFF, PINK = 0xFFFF1FB8, ORANGE = 0xFFFF7A1A, RED = 0xFFFF2D6F;
    static void apply(boolean d) {
        dark = d;
        if (d) { bg = 0xFF0F0B16; surface = 0xFF1B1524; surface2 = 0xFF282034; text = 0xFFF6F2FB; sub = 0xFFA99FBA; divider = 0xFF2B2338; }
        else { bg = 0xFFF7F3FB; surface = 0xFFFFFFFF; surface2 = 0xFFEFE8F7; text = 0xFF1C1426; sub = 0xFF71667F; divider = 0xFFE6DDF0; }
    }
}

interface Do { void go(); }

class U {
    static float den = 1;
    static int dp(float v) { return Math.round(v * den); }

    static GradientDrawable rect(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r));
        return g;
    }

    static GradientDrawable brand(float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{T.PURPLE, T.PINK, T.ORANGE});
        g.setCornerRadius(dp(r));
        return g;
    }

    static GradientDrawable stroke(int color, float r, float w) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0); g.setStroke(dp(w), color); g.setCornerRadius(dp(r));
        return g;
    }

    static TextView tv(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static ImageView icon(Context c, int res, int tint, float size) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        i.setColorFilter(tint);
        i.setLayoutParams(new ViewGroup.LayoutParams(dp(size), dp(size)));
        return i;
    }

    static void ripple(View v) {
        Drawable cur = v.getBackground();
        Drawable mask = cur != null ? null : new ColorDrawable(0xFF000000);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FF1FB8), cur, mask));
    }

    static void click(View v, final Do d) {
        v.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { d.go(); } });
    }

    static void gradText(TextView t) {
        float w = t.getPaint().measureText(t.getText().toString());
        t.getPaint().setShader(new LinearGradient(0, 0, Math.max(w, 1), 0, new int[]{T.PURPLE, T.PINK, T.ORANGE}, null, Shader.TileMode.CLAMP));
    }

    static LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    static LinearLayout.LayoutParams lp(int w, int h, float wt) { return new LinearLayout.LayoutParams(w, h, wt); }

    static LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    static String ago(long ts) {
        long d = Math.max(0, System.currentTimeMillis() - ts) / 1000;
        if (d < 60) return "now";
        if (d < 3600) return (d / 60) + "m";
        if (d < 86400) return (d / 3600) + "h";
        if (d < 604800) return (d / 86400) + "d";
        return (d / 604800) + "w";
    }

    static String fmt(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000f).replace(".0M", "M");
        if (n >= 10000) return String.format("%.1fK", n / 1000f).replace(".0K", "K");
        return java.text.NumberFormat.getInstance().format(n);
    }
}

class Img {
    interface CB { void done(Bitmap b); }
    static LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(24 * 1024 * 1024) {
        protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
    };
    static ExecutorService ex = Executors.newFixedThreadPool(2);
    static Handler h = new Handler(Looper.getMainLooper());

    static Bitmap sync(String path, int max) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int s = 1;
        while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        return BitmapFactory.decodeFile(path, o);
    }

    static void load(final String path, final int max, final CB cb) {
        Bitmap b = cache.get(path + max);
        if (b != null) { cb.done(b); return; }
        ex.execute(new Runnable() {
            public void run() {
                final Bitmap r = sync(path, max);
                if (r != null) cache.put(path + max, r);
                h.post(new Runnable() { public void run() { cb.done(r); } });
            }
        });
    }
}

class ArtView extends View {
    static final int[][] PAL = {
        {0xFF7A1CFF, 0xFFFF1FB8}, {0xFFFF1FB8, 0xFFFF7A1A}, {0xFF3B1BFF, 0xFF00C2FF}, {0xFFFF7A1A, 0xFFFFD200},
        {0xFF11998E, 0xFF38EF7D}, {0xFF4776E6, 0xFF8E54E3}, {0xFFFF512F, 0xFFDD2476}, {0xFF0F2027, 0xFF2C5364},
        {0xFFEE0979, 0xFFFF6A00}, {0xFF1A2980, 0xFF26D0CE}};
    int seed; String emoji = "", path; Bitmap bmp; int resourceId;
    float ratio = 0; boolean circle, ring, fitCenter; float corner = 0;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    Path clip = new Path();

    ArtView(Context c) { super(c); }

    void set(int seed, String emoji, String path) {
        this.seed = seed; this.emoji = emoji == null ? "" : emoji; this.path = path; this.resourceId = 0; bmp = null;
        if (path != null) {
            final String pp = path;
            Img.load(pp, 720, new Img.CB() {
                public void done(Bitmap b) { if (pp.equals(ArtView.this.path)) { bmp = b; invalidate(); } }
            });
        }
        invalidate();
    }

    void setResource(int id) { resourceId = id; path = null; bmp = null; invalidate(); }

    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        if (ratio > 0) setMeasuredDimension(w, (int) (w * ratio));
        else super.onMeasure(ws, hs);
    }

    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        float pad = ring ? U.dp(4) : 0;
        float l = pad, t = pad, r = w - pad, b = h - pad;
        if (ring) {
            p.setShader(new SweepGradient(w / 2f, h / 2f, new int[]{T.PURPLE, T.PINK, T.ORANGE, T.PINK, T.PURPLE}, null));
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(U.dp(2.5f));
            cv.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f - U.dp(1.5f), p);
            p.setShader(null); p.setStyle(Paint.Style.FILL);
        }
        cv.save();
        clip.reset();
        if (circle) clip.addCircle(w / 2f, h / 2f, Math.min(r - l, b - t) / 2f, Path.Direction.CW);
        else clip.addRoundRect(new RectF(l, t, r, b), U.dp(corner), U.dp(corner), Path.Direction.CW);
        cv.clipPath(clip);
        if (resourceId != 0) {
            android.graphics.drawable.Drawable d = getResources().getDrawable(resourceId);
            d.setBounds((int) l, (int) t, (int) r, (int) b);
            d.draw(cv);
        } else if (bmp != null) {
            p.setShader(null); p.setColor(Color.WHITE); p.setAlpha(255); p.setColorFilter(null);
            if (fitCenter) {
                float s = Math.min((r - l) / bmp.getWidth(), (b - t) / bmp.getHeight());
                float dw = bmp.getWidth() * s, dh = bmp.getHeight() * s;
                Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
                RectF dst = new RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f);
                cv.drawBitmap(bmp, src, dst, p);
            } else {
                float s = Math.max((r - l) / bmp.getWidth(), (b - t) / bmp.getHeight());
                float sw = (r - l) / s, sh = (b - t) / s;
                Rect src = new Rect((int) ((bmp.getWidth() - sw) / 2), (int) ((bmp.getHeight() - sh) / 2), (int) ((bmp.getWidth() + sw) / 2), (int) ((bmp.getHeight() + sh) / 2));
                cv.drawBitmap(bmp, src, new RectF(l, t, r, b), p);
            }
        } else {
            int[] pal = PAL[Math.abs(seed) % PAL.length];
            boolean flip = (seed % 2) == 0;
            p.setShader(new LinearGradient(l, flip ? t : b, r, flip ? b : t, pal[0], pal[1], Shader.TileMode.CLAMP));
            cv.drawRect(l, t, r, b, p);
            p.setShader(null);
            Random rnd = new Random(seed * 7919L + 13);
            float iw = r - l;
            for (int i = 0; i < 4; i++) {
                p.setColor(Color.argb(28 + rnd.nextInt(30), 255, 255, 255));
                cv.drawCircle(l + rnd.nextFloat() * iw, t + rnd.nextFloat() * (b - t), iw * (0.15f + rnd.nextFloat() * 0.35f), p);
            }
            p.setShader(new LinearGradient(0, t + (b - t) * 0.55f, 0, b, 0x00000000, 0x55000000, Shader.TileMode.CLAMP));
            cv.drawRect(l, t, r, b, p);
            p.setShader(null);
            if (emoji.length() > 0) {
                p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER);
                float ts = Math.min(iw, b - t) * (circle ? 0.5f : 0.4f);
                p.setTextSize(ts);
                cv.drawText(emoji, (l + r) / 2f, (t + b) / 2f + ts * 0.35f, p);
            }
        }
        cv.restore();
    }
}
