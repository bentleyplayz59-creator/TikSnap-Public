package com.bytesnap.tiksnap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    static final int REQ_POST = 11, REQ_AVATAR = 12;
    FrameLayout root, content;
    ImageView[] navIcons = new ImageView[5];
    TextView[] navLabels = new TextView[5];
    TextView badge;
    int tab = 0;
    ArrayList<View> stack = new ArrayList<View>();
    FeedAdapter feedAdapter;
    ValueAnimator storyAnim;
    EditText discoverEt, captionEt;
    ArtView createPrev, editAvatarView;
    String pendingPath, editAvatarPath;
    View createHint;

    // ---------- lifecycle ----------
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        U.den = getResources().getDisplayMetrics().density;
        Data.init(this);
        applyTheme();
        buildRoot();
        showTab(0);
        showSplash();
    }

    void applyTheme() {
        int m = Data.sp.getInt("theme", 0);
        boolean sys = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        T.apply(m == 2 || (m == 0 && sys));
        getWindow().setStatusBarColor(T.bg);
        getWindow().setNavigationBarColor(T.surface);
        int f = getWindow().getDecorView().getSystemUiVisibility();
        if (T.dark) f &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x10);
        else f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x10;
        getWindow().getDecorView().setSystemUiVisibility(f);
    }

    void rebuild(boolean openSettings) {
        applyTheme();
        stack.clear();
        buildRoot();
        showTab(tab);
        if (openSettings) push(buildSettings());
    }

    public void onBackPressed() {
        if (!stack.isEmpty()) pop();
        else if (tab != 0) showTab(0);
        else super.onBackPressed();
    }

    // ---------- helpers ----------
    int dp(float v) { return U.dp(v); }
    LinearLayout vbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout hbox() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    ImageView ibtn(int res, int tint) {
        ImageView i = new ImageView(this);
        i.setImageResource(res); i.setColorFilter(tint);
        i.setPadding(dp(10), dp(10), dp(10), dp(10));
        i.setLayoutParams(U.lp(dp(44), dp(44)));
        U.ripple(i);
        return i;
    }

    LinearLayout header(String title, boolean back, View... right) {
        LinearLayout h = hbox();
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setMinimumHeight(dp(56));
        if (back) {
            h.setPadding(dp(6), 0, dp(10), 0);
            ImageView b = ibtn(R.drawable.ic_back, T.text);
            U.click(b, new Do() { public void go() { onBackPressed(); } });
            h.addView(b);
        } else h.setPadding(dp(16), 0, dp(10), 0);
        TextView t = U.tv(this, title, 20, T.text, true);
        h.addView(t, U.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        for (View v : right) h.addView(v);
        return h;
    }

    void hideKb(View v) {
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    ArtView avatar(User u, float size, boolean ring) {
        ArtView a = new ArtView(this);
        a.circle = true; a.ring = ring;
        a.set(u.seed, u.emoji, u.avatarPath);
        a.setLayoutParams(new ViewGroup.LayoutParams(dp(size), dp(size)));
        return a;
    }

    TextView pillBtn(String s, boolean filled) {
        TextView t = U.tv(this, s, 14, filled ? 0xFFFFFFFF : T.text, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(18), dp(9), dp(18), dp(9));
        t.setBackground(filled ? U.brand(20) : U.stroke(T.divider, 20, 1.5f));
        if (!filled) t.setBackground(U.rect(T.surface2, 20));
        U.ripple(t);
        return t;
    }

    void styleFollow(TextView b, boolean following) {
        b.setText(following ? "Following" : "Follow");
        b.setTextColor(following ? T.text : 0xFFFFFFFF);
        b.setBackground(following ? U.rect(T.surface2, 20) : U.brand(20));
        U.ripple(b);
    }

    // ---------- root / nav ----------
    void buildRoot() {
        root = new FrameLayout(this);
        root.setBackgroundColor(T.bg);
        LinearLayout main = vbox();
        content = new FrameLayout(this);
        main.addView(content, U.lp(-1, 0, 1f));
        main.addView(buildNav(), U.lp(-1, -2));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    View buildNav() {
        int[] ic = {R.drawable.ic_home, R.drawable.ic_search, R.drawable.ic_add, R.drawable.ic_bell, R.drawable.ic_person};
        String[] lb = {"Home", "Discover", "", "Notifications", "Profile"};
        LinearLayout wrap = vbox();
        wrap.setBackgroundColor(T.surface);
        View line = new View(this);
        line.setBackgroundColor(T.divider);
        wrap.addView(line, U.lp(-1, 1));
        LinearLayout row = hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout it = vbox();
            it.setGravity(Gravity.CENTER);
            it.setMinimumHeight(dp(50));
            if (i == 2) {
                FrameLayout pill = new FrameLayout(this);
                pill.setBackground(U.brand(14));
                ImageView p = U.icon(this, R.drawable.ic_add, 0xFFFFFFFF, 24);
                pill.addView(p, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
                it.addView(pill, U.lp(dp(52), dp(36)));
                navIcons[i] = null;
            } else {
                FrameLayout box = new FrameLayout(this);
                ImageView im = U.icon(this, ic[i], T.sub, 24);
                box.addView(im, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER_HORIZONTAL));
                if (i == 3) {
                    badge = U.tv(this, "", 9, 0xFFFFFFFF, true);
                    badge.setGravity(Gravity.CENTER);
                    badge.setBackground(U.rect(T.RED, 8));
                    badge.setMinWidth(dp(16)); badge.setMinHeight(dp(16));
                    badge.setPadding(dp(3), 0, dp(3), 0);
                    FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                    bp.leftMargin = dp(16); bp.topMargin = dp(-4);
                    box.addView(badge, bp);
                }
                it.addView(box, U.lp(dp(40), dp(24)));
                TextView l = U.tv(this, lb[i], 10, T.sub, false);
                l.setSingleLine(true);
                l.setGravity(Gravity.CENTER);
                it.addView(l, U.lpm(-2, -2, 0, 2, 0, 0));
                navIcons[i] = im; navLabels[i] = l;
            }
            it.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(idx); } });
            row.addView(it, U.lp(0, -2, 1f));
        }
        wrap.addView(row);
        return wrap;
    }

    void updateNav() {
        for (int i = 0; i < 5; i++) {
            if (navIcons[i] == null) continue;
            int c = i == tab ? T.PINK : T.sub;
            navIcons[i].setColorFilter(c);
            navLabels[i].setTextColor(c);
            navLabels[i].setTypeface(i == tab ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
        refreshBadge();
    }

    void refreshBadge() {
        int n = tab == 3 ? 0 : Data.unseen();
        badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        badge.setText(n > 9 ? "9+" : String.valueOf(n));
    }

    void showTab(int i) {
        tab = i;
        stopStory();
        stack.clear();
        content.removeAllViews();
        feedAdapter = null;
        View v;
        switch (i) {
            case 1: v = buildDiscover(); break;
            case 2: v = buildCreate(); break;
            case 3: v = buildNotifs(); break;
            case 4: v = buildProfile(Data.me, true); break;
            default: v = buildHome();
        }
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        v.setAlpha(0); v.setTranslationY(dp(14));
        v.animate().alpha(1).translationY(0).setDuration(220).start();
        if (i == 3) Data.markSeen();
        updateNav();
    }

    void push(View v) {
        v.setClickable(true);
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
        stack.add(v);
        v.setTranslationX(root.getWidth() > 0 ? root.getWidth() : dp(360));
        v.animate().translationX(0).setDuration(240).start();
    }

    void pop() {
        if (stack.isEmpty()) return;
        final View v = stack.remove(stack.size() - 1);
        stopStory();
        hideKb(v);
        v.animate().translationX(root.getWidth()).setDuration(200).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator a) { content.removeView(v); }
        }).start();
        if (feedAdapter != null) feedAdapter.notifyDataSetChanged();
    }

    void stopStory() {
        if (storyAnim != null) { storyAnim.cancel(); storyAnim = null; }
    }

    void showSplash() {
        final FrameLayout sp = new FrameLayout(this);
        sp.setBackground(U.brand(0));
        sp.setClickable(true);
        LinearLayout col = vbox();
        col.setGravity(Gravity.CENTER);
        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        col.addView(logo, U.lp(dp(120), dp(120)));
        final TextView name = U.tv(this, "TikSnap", 38, 0xFFFFFFFF, true);
        name.setLetterSpacing(0.04f);
        col.addView(name, U.lpm(-2, -2, 0, 18, 0, 0));
        final TextView tag = U.tv(this, "Snap it. Tik it. Share it.", 14, 0xCCFFFFFF, false);
        col.addView(tag, U.lpm(-2, -2, 0, 4, 0, 0));
        sp.addView(col, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));
        logo.setScaleX(0.55f); logo.setScaleY(0.55f); logo.setAlpha(0f);
        logo.animate().scaleX(1).scaleY(1).alpha(1).setDuration(650).setInterpolator(new OvershootInterpolator(1.6f)).start();
        name.setAlpha(0); tag.setAlpha(0);
        name.animate().alpha(1).setStartDelay(250).setDuration(450).start();
        tag.animate().alpha(1).setStartDelay(450).setDuration(450).start();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                sp.animate().alpha(0).setDuration(350).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator a) { root.removeView(sp); }
                }).start();
            }
        }, 1700);
    }

    // ---------- HOME ----------
    View buildHome() {
        LinearLayout col = vbox();
        LinearLayout h = hbox();
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(16), 0, dp(8), 0);
        h.setMinimumHeight(dp(56));
        ImageView lg = new ImageView(this);
        lg.setImageResource(R.drawable.logo);
        h.addView(lg, U.lpm(dp(30), dp(30), 0, 0, 10, 0));
        TextView t = U.tv(this, "TikSnap", 25, T.text, true);
        U.gradText(t);
        h.addView(t, U.lp(0, -2, 1f));
        ImageView bell = ibtn(R.drawable.ic_bell, T.text);
        U.click(bell, new Do() { public void go() { showTab(3); } });
        h.addView(bell);
        col.addView(h);
        ListView lv = new ListView(this);
        lv.setDivider(null); lv.setDividerHeight(0);
        lv.setSelector(new android.graphics.drawable.ColorDrawable(0));
        lv.setVerticalScrollBarEnabled(false);
        lv.setClipToPadding(false);
        lv.setPadding(0, 0, 0, dp(8));
        lv.addHeaderView(stories(), null, false);
        feedAdapter = new FeedAdapter();
        lv.setAdapter(feedAdapter);
        col.addView(lv, U.lp(-1, 0, 1f));
        return col;
    }

    View stories() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = hbox();
        row.setPadding(dp(10), dp(4), dp(10), dp(10));
        row.addView(storyItem(Data.me, true));
        for (int i = 0; i < 8; i++) row.addView(storyItem(Data.users.get(i), false));
        hs.addView(row);
        return hs;
    }

    View storyItem(final User u, final boolean mine) {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(6), 0, dp(6), 0);
        FrameLayout f = new FrameLayout(this);
        f.addView(avatar(u, 68, !mine), new FrameLayout.LayoutParams(dp(68), dp(68)));
        if (mine) {
            TextView plus = U.tv(this, "+", 15, 0xFFFFFFFF, true);
            plus.setGravity(Gravity.CENTER);
            plus.setBackground(U.brand(10));
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.BOTTOM | Gravity.END);
            f.addView(plus, pp);
        }
        c.addView(f, U.lp(dp(68), dp(68)));
        TextView n = U.tv(this, mine ? "Your story" : u.name.split(" ")[0], 11.5f, T.text, false);
        n.setSingleLine(true);
        n.setGravity(Gravity.CENTER);
        c.addView(n, U.lpm(dp(70), -2, 0, 4, 0, 0));
        U.click(c, new Do() { public void go() { if (mine) showTab(2); else openStory(u); } });
        return c;
    }

    void openStory(final User u) {
        final FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(0xFF000000);
        ArtView a = new ArtView(this);
        a.set(u.seed + 40, u.emoji, null);
        f.addView(a, new FrameLayout.LayoutParams(-1, -1));
        final View bar = new View(this);
        bar.setBackgroundColor(0xFFFFFFFF);
        FrameLayout track = new FrameLayout(this);
        track.setBackgroundColor(0x55FFFFFF);
        track.addView(bar, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, dp(3));
        tp.setMargins(dp(12), dp(12), dp(12), 0);
        f.addView(track, tp);
        LinearLayout top = hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(26), dp(12), 0);
        top.addView(avatar(u, 36, false));
        TextView nm = U.tv(this, "  " + u.id + "  \u00B7 " + (1 + Math.abs(u.seed) % 9) + "h", 14, 0xFFFFFFFF, true);
        top.addView(nm, U.lp(0, -2, 1f));
        ImageView x = ibtn(R.drawable.ic_close, 0xFFFFFFFF);
        U.click(x, new Do() { public void go() { onBackPressed(); } });
        top.addView(x);
        f.addView(top, new FrameLayout.LayoutParams(-1, -2));
        TextView cap = U.tv(this, Data.STORY_LINES[Math.abs(u.seed) % Data.STORY_LINES.length], 22, 0xFFFFFFFF, true);
        cap.setGravity(Gravity.CENTER);
        cap.setShadowLayer(8, 0, 2, 0x99000000);
        cap.setPadding(dp(24), 0, dp(24), dp(80));
        f.addView(cap, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        push(f);
        bar.setPivotX(0);
        stopStory();
        storyAnim = ValueAnimator.ofFloat(0f, 1f);
        storyAnim.setDuration(5000);
        storyAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator va) { bar.setScaleX((Float) va.getAnimatedValue()); }
        });
        storyAnim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;
            public void onAnimationCancel(Animator a) { cancelled = true; }
            public void onAnimationEnd(Animator a) { if (!cancelled && !stack.isEmpty() && stack.get(stack.size() - 1) == f) pop(); }
        });
        storyAnim.start();
    }

    class FeedAdapter extends BaseAdapter {
        List<Post> items = Data.all();
        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }
        public void notifyDataSetChanged() { items = Data.all(); super.notifyDataSetChanged(); }
        public View getView(int i, View cv, ViewGroup parent) {
            PostCard c;
            if (cv == null) {
                FrameLayout w = new FrameLayout(MainActivity.this);
                w.setPadding(dp(12), dp(6), dp(12), dp(6));
                c = new PostCard(MainActivity.this);
                w.addView(c, new FrameLayout.LayoutParams(-1, -2));
                w.setTag(c);
                cv = w;
            } else c = (PostCard) cv.getTag();
            c.bind(items.get(i));
            return cv;
        }
    }

    class PostCard extends LinearLayout {
        Post post;
        ArtView av, art, poster;
        LinearLayout top;
        TextView name, meta, likes, cap, vc, more;
        ImageView heart, pop, verified;

        PostCard(Context c) {
            super(c);
            setOrientation(VERTICAL);
            setBackground(U.rect(T.surface, 22));
            setClipToOutline(true);
            top = hbox();
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setPadding(dp(12), dp(12), dp(8), dp(10));
            av = new ArtView(c);
            av.circle = true;
            top.addView(av, U.lp(dp(40), dp(40)));
            LinearLayout nc = vbox();
            LinearLayout displayRow = hbox();
            displayRow.setGravity(Gravity.CENTER_VERTICAL);
            name = U.tv(c, "", 15, T.text, true);
            verified = U.icon(c, R.drawable.ic_verified, 0xFF1D9BF0, 15);
            verified.setVisibility(View.GONE);
            displayRow.addView(name);
            displayRow.addView(verified, U.lpm(dp(15), dp(15), 4, 0, 0, 0));
            nc.addView(displayRow);
            meta = U.tv(c, "", 12, T.sub, false);
            nc.addView(meta);
            top.addView(nc, U.lpm(0, -2, 10, 0, 0, 0));
            ((LinearLayout.LayoutParams) nc.getLayoutParams()).weight = 1;
            more = U.tv(c, "\u22EF", 22, T.sub, true);
            more.setPadding(dp(12), 0, dp(12), 0);
            top.addView(more);
            addView(top);
            poster = new ArtView(c);
            poster.fitCenter = true;
            addView(poster, new LinearLayout.LayoutParams(-1, -2));
            poster.setVisibility(View.GONE);
            FrameLayout frame = new FrameLayout(c);
            art = new ArtView(c);
            art.ratio = 1.12f;
            art.fitCenter = true;
            frame.addView(art, new FrameLayout.LayoutParams(-1, -2));
            pop = U.icon(c, R.drawable.ic_heart, 0xFFFFFFFF, 96);
            pop.setAlpha(0f);
            frame.addView(pop, new FrameLayout.LayoutParams(dp(96), dp(96), Gravity.CENTER));
            addView(frame, new LinearLayout.LayoutParams(-1, -2));
            final GestureDetector gd = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
                public boolean onDown(MotionEvent e) { return true; }
                public boolean onDoubleTap(MotionEvent e) {
                    if (!Data.isLiked(post)) like();
                    pop.setScaleX(0.3f); pop.setScaleY(0.3f); pop.setAlpha(1f);
                    pop.animate().scaleX(1.2f).scaleY(1.2f).alpha(0f).setDuration(650).start();
                    return true;
                }
            });
            art.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent e) { return gd.onTouchEvent(e); }
            });
            LinearLayout acts = hbox();
            acts.setGravity(Gravity.CENTER_VERTICAL);
            acts.setPadding(dp(6), dp(4), dp(6), 0);
            heart = ibtn(R.drawable.ic_heart_out, T.text);
            U.click(heart, new Do() { public void go() { like(); } });
            ImageView cm = ibtn(R.drawable.ic_comment, T.text);
            U.click(cm, new Do() { public void go() { openComments(post); } });
            ImageView sh = ibtn(R.drawable.ic_send, T.text);
            U.click(sh, new Do() { public void go() { share(post); } });
            acts.addView(heart); acts.addView(cm); acts.addView(sh);
            addView(acts);
            likes = U.tv(c, "", 14, T.text, true);
            likes.setPadding(dp(14), 0, dp(14), 0);
            addView(likes);
            cap = U.tv(c, "", 14.5f, T.text, false);
            cap.setPadding(dp(14), dp(3), dp(14), 0);
            cap.setMovementMethod(LinkMovementMethod.getInstance());
            addView(cap);
            vc = U.tv(c, "", 13, T.sub, false);
            vc.setPadding(dp(14), dp(6), dp(14), dp(14));
            addView(vc);
            View.OnClickListener prof = new View.OnClickListener() { public void onClick(View v) { openProfile(Data.user(post.user)); } };
            av.setOnClickListener(prof); name.setOnClickListener(prof);
            U.click(vc, new Do() { public void go() { openComments(post); } });
            U.click(more, new Do() { public void go() { confirmDelete(post); } });
        }

        void like() {
            Data.toggleLike(post);
            refreshLike(true);
        }

        void refreshLike(boolean anim) {
            boolean l = Data.isLiked(post);
            heart.setImageResource(l ? R.drawable.ic_heart : R.drawable.ic_heart_out);
            heart.setColorFilter(l ? T.RED : T.text);
            likes.setText(U.fmt(Data.likes(post)) + " likes");
            if (anim && l) {
                heart.setScaleX(0.6f); heart.setScaleY(0.6f);
                heart.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new OvershootInterpolator(3f)).start();
            }
        }

        void bind(Post p) {
            post = p;
            User u = Data.user(p.user);
            boolean hasPosterHeader = p.posterPath != null;
            top.setVisibility(hasPosterHeader ? View.GONE : View.VISIBLE);
            poster.setVisibility(hasPosterHeader ? View.VISIBLE : View.GONE);
            if (hasPosterHeader) {
                poster.ratio = imageRatio(p.posterPath, 0.13f);
                poster.set(p.seed, "", p.posterPath);
                poster.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openProfile(Data.user(post.user)); } });
            }
            art.ratio = imageRatio(p.imagePath, "windex".equals(p.user) ? 1.02f : ("mcdonalds".equals(p.user) ? 1.24f : 1.12f));
            av.set(u.seed, u.emoji, u.avatarPath);
            name.setText(u.name);
            verified.setVisibility(u.verified ? View.VISIBLE : View.GONE);
            meta.setText("@" + u.id + " · " + (u.verified ? "Official · " : "") + U.ago(p.ts));
            if (p.imageRes != 0) art.setResource(p.imageRes); else art.set(p.seed, p.emoji, p.imagePath);
            boolean hasMedia = p.imagePath != null || p.imageRes != 0;
            art.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
            pop.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
            more.setVisibility(p.mine ? View.VISIBLE : View.GONE);
            cap.setText(captionText(p));
            int n = p.comments.size();
            vc.setText(n == 0 ? "Add a comment\u2026" : (n == 1 ? "View 1 comment" : "View all " + n + " comments"));
            refreshLike(false);
        }
    }

    float imageRatio(String path, float fallback) {
        if (path == null) return fallback;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth > 0 && options.outHeight > 0) {
            return (float) options.outHeight / (float) options.outWidth;
        }
        return fallback;
    }

    CharSequence captionText(Post p) {
        User u = Data.user(p.user);
        SpannableStringBuilder sb = new SpannableStringBuilder(u.id + " ");
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, u.id.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = sb.length();
        sb.append(p.caption);
        Matcher m = Pattern.compile("#\\w+").matcher(p.caption);
        while (m.find()) {
            final String tag = m.group();
            sb.setSpan(new ClickableSpan() {
                public void onClick(View v) { openSearch(tag); }
                public void updateDrawState(TextPaint ds) { ds.setColor(T.PINK); ds.setUnderlineText(false); }
            }, start + m.start(), start + m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    void openSearch(String q) {
        showTab(1);
        discoverEt.setText(q);
        discoverEt.setSelection(q.length());
    }

    void share(Post p) {
        User u = Data.user(p.user);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Check out @" + u.id + "'s post on TikSnap: " + p.caption);
        startActivity(Intent.createChooser(i, "Share post"));
    }

    void confirmDelete(final Post p) {
        new AlertDialog.Builder(this, T.dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Delete post?").setMessage("This removes the post from your profile and feed.")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    Data.deletePost(p);
                    if (tab == 0 && feedAdapter != null) feedAdapter.notifyDataSetChanged();
                    else showTab(tab);
                    toast("Post deleted");
                }
            }).setNegativeButton("Cancel", null).show();
    }

    void openProfile(User u) {
        if (u == Data.me) { showTab(4); return; }
        push(buildProfile(u, false));
    }

    void openPost(Post p) {
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        if (p.posterPath == null) col.addView(header("Post", true));
        ScrollView sv = new ScrollView(this);
        FrameLayout w = new FrameLayout(this);
        w.setPadding(dp(12), dp(6), dp(12), dp(16));
        PostCard c = new PostCard(this);
        c.bind(p);
        w.addView(c, new FrameLayout.LayoutParams(-1, -2));
        sv.addView(w);
        col.addView(sv, U.lp(-1, 0, 1f));
        push(col);
    }

    // ---------- COMMENTS ----------
    void openComments(final Post p) {
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        col.addView(header("Comments", true));
        final ScrollView sv = new ScrollView(this);
        final LinearLayout list = vbox();
        list.setPadding(dp(16), dp(4), dp(16), dp(12));
        sv.addView(list);
        fillComments(list, p);
        col.addView(sv, U.lp(-1, 0, 1f));
        View line = new View(this);
        line.setBackgroundColor(T.divider);
        col.addView(line, U.lp(-1, 1));
        LinearLayout in = hbox();
        in.setGravity(Gravity.CENTER_VERTICAL);
        in.setPadding(dp(12), dp(8), dp(8), dp(8));
        in.setBackgroundColor(T.surface);
        in.addView(avatar(Data.me, 36, false));
        final EditText et = new EditText(this);
        et.setHint("Add a comment\u2026");
        et.setHintTextColor(T.sub); et.setTextColor(T.text); et.setTextSize(15);
        et.setBackground(U.rect(T.surface2, 20));
        et.setPadding(dp(16), dp(10), dp(16), dp(10));
        et.setMaxLines(3);
        in.addView(et, U.lpm(0, -2, 10, 0, 6, 0));
        ((LinearLayout.LayoutParams) et.getLayoutParams()).weight = 1;
        ImageView send = ibtn(R.drawable.ic_send, 0xFFFFFFFF);
        send.setBackground(U.brand(22));
        send.setPadding(dp(11), dp(11), dp(11), dp(11));
        U.click(send, new Do() {
            public void go() {
                String t = et.getText().toString().trim();
                if (t.length() == 0) return;
                Data.addComment(p, t);
                et.setText("");
                fillComments(list, p);
                sv.post(new Runnable() { public void run() { sv.fullScroll(View.FOCUS_DOWN); } });
            }
        });
        in.addView(send);
        col.addView(in);
        push(col);
    }

    void fillComments(LinearLayout list, Post p) {
        list.removeAllViews();
        User au = Data.user(p.user);
        list.addView(commentRow(au, p.caption, p.ts));
        for (Comment c : p.comments) list.addView(commentRow(Data.user(c.user), c.text, c.ts));
        if (p.comments.isEmpty()) {
            TextView e = U.tv(this, "No comments yet. Start the conversation!", 14, T.sub, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(30), 0, 0);
            list.addView(e);
        }
    }

    View commentRow(final User u, String text, long ts) {
        LinearLayout r = hbox();
        r.setPadding(0, dp(8), 0, dp(8));
        ArtView a = avatar(u, 36, false);
        U.click(a, new Do() { public void go() { openProfile(u); } });
        r.addView(a);
        LinearLayout c = vbox();
        SpannableStringBuilder sb = new SpannableStringBuilder(u.id + "  ");
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, u.id.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(text);
        c.addView(U.tv(this, "", 14.5f, T.text, false));
        ((TextView) c.getChildAt(0)).setText(sb);
        c.addView(U.tv(this, U.ago(ts), 12, T.sub, false));
        r.addView(c, U.lpm(0, -2, 10, 0, 0, 0));
        ((LinearLayout.LayoutParams) c.getLayoutParams()).weight = 1;
        return r;
    }

    // ---------- DISCOVER ----------
    View buildDiscover() {
        LinearLayout col = vbox();
        col.addView(header("Discover", false));
        LinearLayout bar = hbox();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(U.rect(T.surface2, 24));
        bar.setPadding(dp(14), 0, dp(6), 0);
        bar.addView(U.icon(this, R.drawable.ic_search, T.sub, 20));
        discoverEt = new EditText(this);
        discoverEt.setHint("Search people, posts, #tags");
        discoverEt.setHintTextColor(T.sub); discoverEt.setTextColor(T.text); discoverEt.setTextSize(15);
        discoverEt.setBackground(null);
        discoverEt.setSingleLine(true);
        discoverEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        discoverEt.setPadding(dp(10), dp(12), dp(10), dp(12));
        bar.addView(discoverEt, U.lp(0, -2, 1f));
        final ImageView clr = ibtn(R.drawable.ic_close, T.sub);
        clr.setVisibility(View.GONE);
        U.click(clr, new Do() { public void go() { discoverEt.setText(""); } });
        bar.addView(clr, U.lp(dp(40), dp(40)));
        col.addView(bar, U.lpm(-1, dp(46), 16, 0, 16, 8));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        final LinearLayout body = vbox();
        body.setPadding(0, 0, 0, dp(16));
        sv.addView(body);
        col.addView(sv, U.lp(-1, 0, 1f));
        renderDiscover(body, "");
        discoverEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                clr.setVisibility(e.length() > 0 ? View.VISIBLE : View.GONE);
                renderDiscover(body, e.toString().trim());
            }
        });
        discoverEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int a, android.view.KeyEvent e) { hideKb(v); return true; }
        });
        return col;
    }

    TextView sectionTitle(String s) {
        TextView t = U.tv(this, s, 17, T.text, true);
        t.setPadding(dp(16), dp(16), dp(16), dp(8));
        return t;
    }

    void renderDiscover(LinearLayout body, final String q) {
        body.removeAllViews();
        if (q.length() == 0) {
            body.addView(sectionTitle("Trending hashtags"));
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = hbox();
            row.setPadding(dp(12), 0, dp(12), 0);
            List<String> tg = Data.tags();
            for (int i = 0; i < Math.min(9, tg.size()); i++) {
                final String tag = tg.get(i);
                LinearLayout chip = vbox();
                chip.setBackground(U.rect(T.surface, 16));
                chip.setPadding(dp(14), dp(8), dp(14), dp(8));
                chip.addView(U.tv(this, "#" + tag, 15, T.PINK, true));
                chip.addView(U.tv(this, U.fmt(Data.tagCount(tag)) + " posts", 11.5f, T.sub, false));
                U.click(chip, new Do() { public void go() { discoverEt.setText("#" + tag); discoverEt.setSelection(discoverEt.length()); } });
                row.addView(chip, U.lpm(-2, -2, 4, 0, 4, 0));
            }
            hs.addView(row);
            body.addView(hs);
            body.addView(sectionTitle("Trending topics"));
            LinearLayout card = vbox();
            card.setBackground(U.rect(T.surface, 18));
            for (int i = 0; i < Data.TOPICS.length; i++) {
                final String[] tp = Data.TOPICS[i];
                LinearLayout r = hbox();
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPadding(dp(16), dp(12), dp(16), dp(12));
                TextView rank = U.tv(this, String.valueOf(i + 1), 18, T.PINK, true);
                rank.setMinWidth(dp(28));
                r.addView(rank);
                LinearLayout c = vbox();
                c.addView(U.tv(this, tp[0], 15, T.text, true));
                c.addView(U.tv(this, tp[1] + " \u00B7 " + tp[2], 12.5f, T.sub, false));
                r.addView(c, U.lp(0, -2, 1f));
                r.addView(U.icon(this, R.drawable.ic_search, T.sub, 18));
                U.ripple(r);
                U.click(r, new Do() { public void go() { discoverEt.setText(tp[1]); discoverEt.setSelection(discoverEt.length()); } });
                card.addView(r);
            }
            body.addView(card, U.lpm(-1, -2, 16, 0, 16, 0));
            body.addView(sectionTitle("Suggested accounts"));
            HorizontalScrollView hs2 = new HorizontalScrollView(this);
            hs2.setHorizontalScrollBarEnabled(false);
            LinearLayout row2 = hbox();
            row2.setPadding(dp(12), 0, dp(12), 0);
            List<User> sug = new ArrayList<User>();
            for (User u : Data.users) if (!Data.isFollowing(u.id)) sug.add(u);
            for (User u : Data.users) if (Data.isFollowing(u.id) && sug.size() < 7) sug.add(u);
            for (int i = 0; i < Math.min(7, sug.size()); i++) row2.addView(suggestCard(sug.get(i)), U.lpm(dp(140), -2, 4, 0, 4, 0));
            hs2.addView(row2);
            body.addView(hs2);
            body.addView(sectionTitle("Popular posts"));
            List<Post> pop = Data.all();
            java.util.Collections.sort(pop, new java.util.Comparator<Post>() { public int compare(Post a, Post b) { return Data.likes(b) - Data.likes(a); } });
            body.addView(postGrid(pop.subList(0, Math.min(6, pop.size())), 2, 1.25f, 16, 8));
        } else {
            List<User> pu = Data.searchUsers(q);
            List<String> th = Data.searchTags(q);
            List<Post> pp = Data.searchPosts(q);
            if (pu.isEmpty() && th.isEmpty() && pp.isEmpty()) {
                TextView e = U.tv(this, "No results for \u201C" + q + "\u201D\nTry a different name or hashtag.", 15, T.sub, false);
                e.setGravity(Gravity.CENTER);
                e.setPadding(dp(24), dp(60), dp(24), 0);
                body.addView(e);
                return;
            }
            if (!pu.isEmpty()) {
                body.addView(sectionTitle("People"));
                for (int i = 0; i < Math.min(6, pu.size()); i++) body.addView(personRow(pu.get(i)));
            }
            if (!th.isEmpty()) {
                body.addView(sectionTitle("Hashtags"));
                for (int i = 0; i < Math.min(4, th.size()); i++) {
                    final String tag = th.get(i);
                    LinearLayout r = hbox();
                    r.setGravity(Gravity.CENTER_VERTICAL);
                    r.setPadding(dp(16), dp(10), dp(16), dp(10));
                    TextView hash = U.tv(this, "#", 20, 0xFFFFFFFF, true);
                    hash.setGravity(Gravity.CENTER);
                    hash.setBackground(U.brand(24));
                    r.addView(hash, U.lp(dp(48), dp(48)));
                    LinearLayout c = vbox();
                    c.addView(U.tv(this, "#" + tag, 15, T.text, true));
                    c.addView(U.tv(this, U.fmt(Data.tagCount(tag)) + " posts", 12.5f, T.sub, false));
                    r.addView(c, U.lpm(0, -2, 12, 0, 0, 0));
                    U.ripple(r);
                    U.click(r, new Do() { public void go() { discoverEt.setText("#" + tag); discoverEt.setSelection(discoverEt.length()); } });
                    body.addView(r);
                }
            }
            if (!pp.isEmpty()) {
                body.addView(sectionTitle("Posts"));
                body.addView(postGrid(pp, 2, 1.25f, 16, 8));
            }
        }
    }

    View suggestCard(final User u) {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setBackground(U.rect(T.surface, 20));
        c.setPadding(dp(12), dp(16), dp(12), dp(14));
        c.addView(avatar(u, 68, true), U.lp(dp(68), dp(68)));
        TextView n = U.tv(this, u.name, 14.5f, T.text, true);
        n.setSingleLine(true); n.setGravity(Gravity.CENTER);
        c.addView(n, U.lpm(-1, -2, 0, 8, 0, 0));
        TextView h = U.tv(this, "@" + u.id, 12, T.sub, false);
        h.setSingleLine(true); h.setGravity(Gravity.CENTER);
        c.addView(h);
        final TextView b = pillBtn("Follow", true);
        b.setPadding(dp(18), dp(7), dp(18), dp(7));
        styleFollow(b, Data.isFollowing(u.id));
        U.click(b, new Do() { public void go() { Data.toggleFollow(u.id); styleFollow(b, Data.isFollowing(u.id)); } });
        c.addView(b, U.lpm(-1, -2, 0, 10, 0, 0));
        U.click(c, new Do() { public void go() { openProfile(u); } });
        return c;
    }

    View personRow(final User u) {
        LinearLayout r = hbox();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), dp(8));
        r.addView(avatar(u, 48, false));
        LinearLayout c = vbox();
        c.addView(U.tv(this, u.name, 15, T.text, true));
        c.addView(U.tv(this, "@" + u.id + " \u00B7 " + U.fmt(u.followers) + " followers", 12.5f, T.sub, false));
        r.addView(c, U.lpm(0, -2, 12, 0, 8, 0));
        ((LinearLayout.LayoutParams) c.getLayoutParams()).weight = 1;
        final TextView b = pillBtn("Follow", true);
        b.setPadding(dp(16), dp(7), dp(16), dp(7));
        styleFollow(b, Data.isFollowing(u.id));
        U.click(b, new Do() { public void go() { Data.toggleFollow(u.id); styleFollow(b, Data.isFollowing(u.id)); } });
        r.addView(b);
        U.ripple(r);
        U.click(r, new Do() { public void go() { openProfile(u); } });
        return r;
    }

    View postGrid(List<Post> posts, int cols, float ratio, float padDp, float gapDp) {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int gap = dp(gapDp);
        int tile = (sw - 2 * dp(padDp) - gap * (cols - 1)) / cols;
        LinearLayout g = vbox();
        g.setPadding(dp(padDp), 0, dp(padDp), 0);
        for (int i = 0; i < posts.size(); i += cols) {
            LinearLayout row = hbox();
            for (int j = 0; j < cols; j++) {
                if (i + j >= posts.size()) { row.addView(new View(this), U.lp(tile, 1)); continue; }
                final Post p = posts.get(i + j);
                FrameLayout f = new FrameLayout(this);
                ArtView a = new ArtView(this);
                a.corner = cols > 2 ? 4 : 16;
                a.set(p.seed, p.emoji, p.imagePath);
                f.addView(a, new FrameLayout.LayoutParams(tile, (int) (tile * ratio)));
                if (cols <= 2) {
                    LinearLayout lk = hbox();
                    lk.setGravity(Gravity.CENTER_VERTICAL);
                    lk.setPadding(dp(10), 0, 0, dp(8));
                    lk.addView(U.icon(this, R.drawable.ic_heart, 0xFFFFFFFF, 14));
                    TextView t = U.tv(this, " " + U.fmt(Data.likes(p)), 12.5f, 0xFFFFFFFF, true);
                    t.setShadowLayer(6, 0, 1, 0x99000000);
                    lk.addView(t);
                    f.addView(lk, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START));
                }
                U.click(f, new Do() { public void go() { openPost(p); } });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tile, (int) (tile * ratio));
                lp.rightMargin = j < cols - 1 ? gap : 0;
                row.addView(f, lp);
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = gap;
            g.addView(row, rp);
        }
        return g;
    }

    // ---------- CREATE ----------
    View buildCreate() {
        LinearLayout col = vbox();
        col.addView(header("New post", false));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = vbox();
        body.setPadding(dp(16), dp(4), dp(16), dp(24));
        int sw = getResources().getDisplayMetrics().widthPixels - dp(32);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(U.rect(T.surface2, 22));
        frame.setClipToOutline(true);
        createPrev = new ArtView(this);
        createPrev.corner = 22;
        frame.addView(createPrev, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout hint = vbox();
        hint.setGravity(Gravity.CENTER);
        ImageView hi = U.icon(this, R.drawable.ic_image, T.PINK, 44);
        hint.addView(hi, U.lp(dp(44), dp(44)));
        TextView ht = U.tv(this, "Tap to choose a photo", 16, T.text, true);
        hint.addView(ht, U.lpm(-2, -2, 0, 10, 0, 0));
        hint.addView(U.tv(this, "Pick an image from your gallery", 13, T.sub, false));
        createHint = hint;
        frame.addView(hint, new FrameLayout.LayoutParams(-1, -1));
        U.click(frame, new Do() { public void go() { pickImage(REQ_POST); } });
        body.addView(frame, U.lp(sw, sw));
        updateCreatePreview();
        LinearLayout btns = hbox();
        TextView choose = pillBtn("Choose from gallery", false);
        U.click(choose, new Do() { public void go() { pickImage(REQ_POST); } });
        btns.addView(choose);
        TextView rm = pillBtn("Remove", false);
        rm.setTextColor(T.sub);
        U.click(rm, new Do() { public void go() { pendingPath = null; updateCreatePreview(); } });
        btns.addView(rm, U.lpm(-2, -2, 8, 0, 0, 0));
        body.addView(btns, U.lpm(-2, -2, 0, 12, 0, 0));
        body.addView(U.tv(this, "Caption", 14, T.sub, true), U.lpm(-2, -2, 0, 18, 0, 6));
        captionEt = new EditText(this);
        captionEt.setHint("Write a caption\u2026 add #hashtags");
        captionEt.setHintTextColor(T.sub); captionEt.setTextColor(T.text); captionEt.setTextSize(15);
        captionEt.setBackground(U.rect(T.surface, 16));
        captionEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        captionEt.setMinLines(4);
        captionEt.setGravity(Gravity.TOP);
        captionEt.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        body.addView(captionEt, U.lp(-1, -2));
        final TextView cnt = U.tv(this, "0/300", 12, T.sub, false);
        cnt.setGravity(Gravity.END);
        captionEt.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { cnt.setText(s.length() + "/300"); }
            public void afterTextChanged(Editable e) { }
        });
        body.addView(cnt, U.lpm(-1, -2, 0, 4, 0, 0));
        TextView pub = U.tv(this, "Publish", 16, 0xFFFFFFFF, true);
        pub.setGravity(Gravity.CENTER);
        pub.setBackground(U.brand(26));
        U.ripple(pub);
        U.click(pub, new Do() {
            public void go() {
                if (pendingPath == null) { toast("Choose a photo first"); return; }
                Data.addPost(captionEt.getText().toString().trim(), pendingPath);
                pendingPath = null;
                hideKb(captionEt);
                toast("Posted to TikSnap \u2728");
                showTab(0);
            }
        });
        body.addView(pub, U.lpm(-1, dp(52), 0, 16, 0, 0));
        sv.addView(body);
        col.addView(sv, U.lp(-1, 0, 1f));
        return col;
    }

    void updateCreatePreview() {
        if (createPrev == null) return;
        boolean has = pendingPath != null;
        createPrev.setVisibility(has ? View.VISIBLE : View.GONE);
        createHint.setVisibility(has ? View.GONE : View.VISIBLE);
        if (has) createPrev.set(0, "", pendingPath);
    }

    void pickImage(int req) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(Intent.createChooser(i, "Choose a photo"), req); }
        catch (Exception e) { toast("No gallery app found"); }
    }

    protected void onActivityResult(final int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (res != RESULT_OK || d == null || d.getData() == null) return;
        final Uri u = d.getData();
        toast("Loading photo\u2026");
        new Thread(new Runnable() {
            public void run() {
                final String path = saveImage(u, req == REQ_AVATAR ? 512 : 1080, req == REQ_AVATAR, req == REQ_AVATAR ? "avatar_" : "post_");
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (path == null) { toast("Couldn't open that image"); return; }
                        if (req == REQ_POST) { pendingPath = path; if (tab == 2) updateCreatePreview(); }
                        else { editAvatarPath = path; if (editAvatarView != null) editAvatarView.set(Data.me.seed, Data.me.emoji, path); }
                    }
                });
            }
        }).start();
    }

    String saveImage(Uri u, int max, boolean square, String prefix) {
        try {
            InputStream in = getContentResolver().openInputStream(u);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (o.outWidth / (s * 2) >= max && o.outHeight / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            in = getContentResolver().openInputStream(u);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            int rot = 0;
            try {
                in = getContentResolver().openInputStream(u);
                int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                in.close();
                rot = ori == 6 ? 90 : ori == 3 ? 180 : ori == 8 ? 270 : 0;
            } catch (Exception ignore) { }
            int x = 0, y = 0, w = b.getWidth(), h = b.getHeight();
            if (square) { int side = Math.min(w, h); x = (w - side) / 2; y = (h - side) / 2; w = side; h = side; }
            Matrix m = new Matrix();
            if (rot != 0) m.postRotate(rot);
            float sc = Math.min(1f, (float) max / Math.max(w, h));
            m.postScale(sc, sc);
            Bitmap r = Bitmap.createBitmap(b, x, y, w, h, m, true);
            File f = new File(getFilesDir(), prefix + System.currentTimeMillis() + ".jpg");
            FileOutputStream fo = new FileOutputStream(f);
            r.compress(Bitmap.CompressFormat.JPEG, 88, fo);
            fo.close();
            return f.getAbsolutePath();
        } catch (Throwable e) { return null; }
    }

    // ---------- NOTIFICATIONS ----------
    View buildNotifs() {
        LinearLayout col = vbox();
        col.addView(header("Notifications", false));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout list = vbox();
        list.setPadding(0, 0, 0, dp(16));
        List<Notif> all = Data.notifs();
        long seen = Data.seen();
        boolean newHdr = false, oldHdr = false;
        if (all.isEmpty()) {
            TextView e = U.tv(this, "You're all caught up.\nTurn notification types back on in Settings.", 15, T.sub, false);
            e.setGravity(Gravity.CENTER);
            e.setPadding(dp(24), dp(80), dp(24), 0);
            list.addView(e);
        }
        for (Notif n : all) {
            if (n.ts > seen && !newHdr) { list.addView(sectionTitle("New")); newHdr = true; }
            if (n.ts <= seen && !oldHdr) { list.addView(sectionTitle("Earlier")); oldHdr = true; }
            list.addView(notifRow(n));
        }
        sv.addView(list);
        col.addView(sv, U.lp(-1, 0, 1f));
        return col;
    }

    View notifRow(final Notif n) {
        final User u = Data.user(n.user);
        LinearLayout r = hbox();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(9), dp(16), dp(9));
        FrameLayout f = new FrameLayout(this);
        f.addView(avatar(u, 50, false), new FrameLayout.LayoutParams(dp(50), dp(50)));
        int bc = n.type.equals("like") ? T.RED : n.type.equals("comment") ? T.PURPLE : n.type.equals("follow") ? T.ORANGE : T.PINK;
        int bi = n.type.equals("like") ? R.drawable.ic_heart : n.type.equals("comment") ? R.drawable.ic_comment : n.type.equals("follow") ? R.drawable.ic_person : R.drawable.ic_bell;
        ImageView b = U.icon(this, bi, 0xFFFFFFFF, 12);
        b.setPadding(dp(4), dp(4), dp(4), dp(4));
        b.setBackground(U.rect(bc, 10));
        f.addView(b, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.BOTTOM | Gravity.END));
        r.addView(f, U.lp(dp(52), dp(52)));
        SpannableStringBuilder sb = new SpannableStringBuilder(u.id + " ");
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, u.id.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(n.text).append("  ");
        int st = sb.length();
        sb.append(U.ago(n.ts));
        sb.setSpan(new android.text.style.ForegroundColorSpan(T.sub), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView t = U.tv(this, "", 14.5f, T.text, false);
        t.setText(sb);
        r.addView(t, U.lpm(0, -2, 12, 0, 8, 0));
        ((LinearLayout.LayoutParams) t.getLayoutParams()).weight = 1;
        if (n.type.equals("follow")) {
            final TextView fb = pillBtn("Follow back", true);
            fb.setPadding(dp(14), dp(7), dp(14), dp(7));
            fb.setTextSize(13);
            styleFollow(fb, Data.isFollowing(u.id));
            if (!Data.isFollowing(u.id)) fb.setText("Follow back");
            U.click(fb, new Do() {
                public void go() {
                    Data.toggleFollow(u.id);
                    styleFollow(fb, Data.isFollowing(u.id));
                    if (!Data.isFollowing(u.id)) fb.setText("Follow back");
                }
            });
            r.addView(fb);
        } else if (n.type.equals("like") || n.type.equals("comment")) {
            ArtView a = new ArtView(this);
            a.corner = 10;
            a.set(n.seed, n.emoji, null);
            r.addView(a, U.lp(dp(46), dp(46)));
        }
        U.ripple(r);
        U.click(r, new Do() { public void go() { openProfile(u); } });
        return r;
    }

    // ---------- PROFILE ----------
    View buildProfile(final User u, final boolean own) {
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        if (own) {
            ImageView gear = ibtn(R.drawable.ic_gear, T.text);
            U.click(gear, new Do() { public void go() { push(buildSettings()); } });
            col.addView(header("@" + u.id, false, gear));
        } else col.addView(header("@" + u.id, true));
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout body = vbox();
        FrameLayout top = new FrameLayout(this);
        ArtView banner = new ArtView(this);
        banner.set(u.seed + 7, "", null);
        top.addView(banner, new FrameLayout.LayoutParams(-1, dp(120)));
        FrameLayout ringW = new FrameLayout(this);
        ringW.setBackground(U.rect(T.bg, 56));
        ringW.setPadding(dp(5), dp(5), dp(5), dp(5));
        ringW.addView(avatar(u, 96, false), new FrameLayout.LayoutParams(dp(96), dp(96)));
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(dp(106), dp(106), Gravity.TOP | Gravity.START);
        rp.leftMargin = dp(16); rp.topMargin = dp(68);
        top.addView(ringW, rp);
        LinearLayout btns = hbox();
        if (own) {
            TextView ed = pillBtn("Edit profile", false);
            U.click(ed, new Do() { public void go() { openEdit(); } });
            btns.addView(ed);
            TextView sh = pillBtn("Share", false);
            U.click(sh, new Do() {
                public void go() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, "Follow @" + u.id + " on TikSnap!");
                    startActivity(Intent.createChooser(i, "Share profile"));
                }
            });
            btns.addView(sh, U.lpm(-2, -2, 8, 0, 0, 0));
        }
        final TextView followersTv = U.tv(this, "", 18, T.text, true);
        if (!own) {
            final TextView fb = pillBtn("Follow", true);
            styleFollow(fb, Data.isFollowing(u.id));
            U.click(fb, new Do() {
                public void go() {
                    Data.toggleFollow(u.id);
                    styleFollow(fb, Data.isFollowing(u.id));
                    followersTv.setText(U.fmt(u.followers + (Data.isFollowing(u.id) ? 1 : 0)));
                }
            });
            btns.addView(fb);
        }
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        bp.rightMargin = dp(16); bp.bottomMargin = dp(4);
        top.addView(btns, bp);
        body.addView(top, U.lp(-1, dp(176)));
        LinearLayout info = vbox();
        info.setPadding(dp(18), dp(4), dp(18), dp(8));
        info.addView(U.tv(this, u.name, 22, T.text, true));
        info.addView(U.tv(this, "@" + u.id, 14, T.sub, false));
        TextView bio = U.tv(this, u.bio, 15, T.text, false);
        bio.setPadding(0, dp(10), 0, 0);
        info.addView(bio);
        body.addView(info);
        List<Post> posts = Data.by(own ? Data.ME : u.id);
        LinearLayout stats = hbox();
        stats.setBackground(U.rect(T.surface, 18));
        stats.setPadding(0, dp(14), 0, dp(14));
        followersTv.setText(java.text.NumberFormat.getIntegerInstance().format(own ? u.followers : u.followers + (Data.isFollowing(u.id) ? 1 : 0)));
        stats.addView(stat(U.tv(this, String.valueOf(posts.size()), 18, T.text, true), "Posts"), U.lp(0, -2, 1f));
        stats.addView(stat(followersTv, "Followers"), U.lp(0, -2, 1f));
        stats.addView(stat(U.tv(this, java.text.NumberFormat.getIntegerInstance().format(u.following), 18, T.text, true), "Following"), U.lp(0, -2, 1f));
        body.addView(stats, U.lpm(-1, -2, 16, 8, 16, 4));
        body.addView(sectionTitle("Posts"));
        if (posts.isEmpty()) {
            LinearLayout e = vbox();
            e.setGravity(Gravity.CENTER);
            e.setPadding(dp(24), dp(24), dp(24), dp(24));
            e.addView(U.tv(this, own ? "No posts yet" : "Nothing posted yet", 17, T.text, true));
            TextView s = U.tv(this, own ? "Share your first moment with TikSnap." : "Check back soon.", 14, T.sub, false);
            s.setGravity(Gravity.CENTER);
            e.addView(s, U.lpm(-2, -2, 0, 4, 0, 14));
            if (own) {
                TextView c = pillBtn("Create a post", true);
                U.click(c, new Do() { public void go() { showTab(2); } });
                e.addView(c);
            }
            body.addView(e);
        } else body.addView(postGrid(posts, 3, 1f, 16, 3));
        View sp = new View(this);
        body.addView(sp, U.lp(1, dp(20)));
        sv.addView(body);
        col.addView(sv, U.lp(-1, 0, 1f));
        return col;
    }

    View stat(TextView num, String label) {
        LinearLayout c = vbox();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        num.setGravity(Gravity.CENTER);
        c.addView(num);
        c.addView(U.tv(this, label, 12.5f, T.sub, false));
        return c;
    }

    void openEdit() {
        editAvatarPath = null;
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        TextView save = pillBtn("Save", true);
        save.setPadding(dp(18), dp(7), dp(18), dp(7));
        col.addView(header("Edit profile", true, save));
        ScrollView sv = new ScrollView(this);
        LinearLayout body = vbox();
        body.setPadding(dp(16), dp(8), dp(16), dp(30));
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        editAvatarView = avatar(Data.me, 104, true);
        U.click(editAvatarView, new Do() { public void go() { pickImage(REQ_AVATAR); } });
        body.addView(editAvatarView, U.lp(dp(104), dp(104)));
        TextView ch = U.tv(this, "Change photo", 14, T.PINK, true);
        ch.setPadding(0, dp(10), 0, dp(14));
        U.click(ch, new Do() { public void go() { pickImage(REQ_AVATAR); } });
        body.addView(ch);
        final EditText name = field(body, "Name", Data.me.name, 1, 40);
        final EditText user = field(body, "Username", Data.me.id, 1, 24);
        final EditText bio = field(body, "Bio", Data.me.bio, 4, 120);
        final EditText mail = field(body, "Email", Data.me.email, 1, 60);
        U.click(save, new Do() {
            public void go() {
                String n = name.getText().toString().trim();
                String un = user.getText().toString().trim().toLowerCase();
                if (n.length() == 0) { toast("Name can't be empty"); return; }
                if (!un.matches("[a-z0-9._]{3,24}")) { toast("Username: 3-24 letters, numbers, . or _"); return; }
                if (editAvatarPath != null && Data.me.avatarPath != null && !Data.me.avatarPath.equals(editAvatarPath)) new File(Data.me.avatarPath).delete();
                Data.saveMe(n, un, bio.getText().toString().trim(), mail.getText().toString().trim(), editAvatarPath);
                hideKb(name);
                toast("Profile updated");
                showTab(tab == 4 ? 4 : tab);
            }
        });
        sv.addView(body);
        col.addView(sv, U.lp(-1, 0, 1f));
        push(col);
    }

    EditText field(LinearLayout parent, String label, String val, int lines, int max) {
        TextView l = U.tv(this, label, 13, T.sub, true);
        parent.addView(l, U.lpm(-1, -2, 4, 8, 0, 6));
        EditText e = new EditText(this);
        e.setText(val);
        e.setTextColor(T.text); e.setTextSize(15.5f);
        e.setBackground(U.rect(T.surface, 14));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (lines > 1) { e.setMinLines(lines); e.setGravity(Gravity.TOP); } else e.setSingleLine(true);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max)});
        parent.addView(e, U.lp(-1, -2));
        return e;
    }

    // ---------- SETTINGS ----------
    TextView sec(String s) {
        TextView t = U.tv(this, s.toUpperCase(), 12, T.sub, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(22), dp(20), dp(22), dp(8));
        return t;
    }

    LinearLayout card() {
        LinearLayout c = vbox();
        c.setBackground(U.rect(T.surface, 18));
        c.setClipToOutline(true);
        return c;
    }

    View srow(String title, String sub, View right, final Do click) {
        LinearLayout r = hbox();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout c = vbox();
        c.addView(U.tv(this, title, 15.5f, T.text, false));
        if (sub != null) c.addView(U.tv(this, sub, 12.5f, T.sub, false));
        r.addView(c, U.lp(0, -2, 1f));
        if (right != null) r.addView(right);
        if (click != null) { U.ripple(r); U.click(r, click); }
        return r;
    }

    Switch sw(final String key, boolean def) {
        Switch s = new Switch(this);
        s.setChecked(Data.sp.getBoolean(key, def));
        int[][] st = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(st, new int[]{0xFFFFFFFF, 0xFFFFFFFF}));
        s.setTrackTintList(new ColorStateList(st, new int[]{T.PINK, 0xFF8E8499}));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Data.sp.edit().putBoolean(key, on).commit(); refreshBadge(); }
        });
        return s;
    }

    String version() {
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName + " (" + pi.versionCode + ")";
        } catch (Exception e) { return "1.1.0"; }
    }

    EditText profileCountInput(int value) {
        EditText field = new EditText(this);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setSingleLine(true);
        field.setText(String.valueOf(value));
        field.setTextColor(T.text);
        field.setTextSize(15);
        field.setSelectAllOnFocus(true);
        field.setBackground(U.rect(T.surface2, 12));
        field.setPadding(dp(12), dp(8), dp(12), dp(8));
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(10)});
        field.setGravity(Gravity.CENTER);
        field.setHintTextColor(T.sub);
        field.setLayoutParams(U.lp(dp(112), -2));
        return field;
    }

    View buildSettings() {
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        col.addView(header("Settings", true));
        ScrollView sv = new ScrollView(this);
        LinearLayout b = vbox();
        b.setPadding(dp(16), 0, dp(16), dp(30));
        b.addView(sec("Appearance"), U.lpm(-1, -2, -16, 0, -16, 0));
        LinearLayout chips = hbox();
        chips.setBackground(U.rect(T.surface, 18));
        chips.setPadding(dp(8), dp(8), dp(8), dp(8));
        String[] names = {"System", "Light", "Dark"};
        int cur = Data.sp.getInt("theme", 0);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = U.tv(this, names[i], 14.5f, i == cur ? 0xFFFFFFFF : T.text, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(11), 0, dp(11));
            t.setBackground(i == cur ? U.brand(14) : U.rect(0, 14));
            U.click(t, new Do() { public void go() { Data.sp.edit().putInt("theme", idx).commit(); rebuild(true); } });
            chips.addView(t, U.lp(0, -2, 1f));
        }
        b.addView(chips);
        b.addView(sec("Notifications"), U.lpm(-1, -2, -16, 0, -16, 0));
        LinearLayout n = card();
        n.addView(srow("Likes", "When someone likes your posts", sw("n_likes", true), null));
        n.addView(srow("Comments", "Replies and comments on your posts", sw("n_comments", true), null));
        n.addView(srow("New followers", "When someone follows you", sw("n_follow", true), null));
        n.addView(srow("Mentions & activity", "Story views, shares and mentions", sw("n_other", true), null));
        b.addView(n);
        b.addView(sec("Profile stats"), U.lpm(-1, -2, -16, 0, -16, 0));
        LinearLayout profileStatsCard = card();
        final EditText followersInput = profileCountInput(Data.me.followers);
        final EditText followingInput = profileCountInput(Data.me.following);
        profileStatsCard.addView(srow("Followers", "Displayed on your profile", followersInput, null));
        profileStatsCard.addView(srow("Following", "Displayed on your profile", followingInput, null));
        LinearLayout statsSaveRow = hbox();
        statsSaveRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        statsSaveRow.setPadding(dp(16), dp(8), dp(16), dp(14));
        TextView saveStatsButton = pillBtn("Save changes", true);
        saveStatsButton.setPadding(dp(18), dp(10), dp(18), dp(10));
        U.click(saveStatsButton, new Do() {
            public void go() {
                String followersText = followersInput.getText().toString().trim();
                String followingText = followingInput.getText().toString().trim();
                boolean followersValid = Data.validProfileCount(followersText);
                boolean followingValid = Data.validProfileCount(followingText);
                Data.saveProfileStats(followersText, followingText);
                followersInput.setText(String.valueOf(Data.me.followers));
                followingInput.setText(String.valueOf(Data.me.following));
                rebuild(true);
                if (followersValid && followingValid) toast("Profile stats saved");
                else if (followersValid || followingValid) toast("Valid value saved; invalid field left unchanged");
                else toast("No changes saved. Enter whole numbers 0 or higher.");
            }
        });
        statsSaveRow.addView(saveStatsButton);
        profileStatsCard.addView(statsSaveRow);
        b.addView(profileStatsCard);
        b.addView(sec("Account"), U.lpm(-1, -2, -16, 0, -16, 0));
        LinearLayout a = card();
        a.addView(srow("Edit profile", "@" + Data.me.id, null, new Do() { public void go() { openEdit(); } }));
        a.addView(srow("Email", Data.me.email.length() == 0 ? "Not set" : Data.me.email, null, new Do() { public void go() { openEdit(); } }));
        a.addView(srow("Private account", "Only approved followers see your posts", sw("private", false), null));
        View clear = srow("Clear local data", "Delete posts, likes and profile changes", null, new Do() {
            public void go() {
                new AlertDialog.Builder(MainActivity.this, T.dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Clear all local data?")
                    .setMessage("This deletes your posts, likes, comments, profile changes and settings on this device.")
                    .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) { Data.wipe(); tab = 0; rebuild(false); toast("Local data cleared"); }
                    }).setNegativeButton("Cancel", null).show();
            }
        });
        a.addView(clear);
        b.addView(a);
        b.addView(sec("About"), U.lpm(-1, -2, -16, 0, -16, 0));
        LinearLayout ab = card();
        ab.addView(srow("About TikSnap", null, U.tv(this, "\u203A", 22, T.sub, false), new Do() { public void go() { push(buildAbout()); } }));
        ab.addView(srow("App version", null, U.tv(this, version(), 14, T.sub, false), null));
        ab.addView(srow("Developer", null, U.tv(this, "ByteSnap Inc.", 14, T.sub, false), null));
        b.addView(ab);
        TextView foot = U.tv(this, "TikSnap \u00B7 Developed by ByteSnap Inc.", 12, T.sub, false);
        foot.setGravity(Gravity.CENTER);
        b.addView(foot, U.lpm(-1, -2, 0, 24, 0, 0));
        sv.addView(b);
        col.addView(sv, U.lp(-1, 0, 1f));
        return col;
    }

    View buildAbout() {
        LinearLayout col = vbox();
        col.setBackgroundColor(T.bg);
        col.addView(header("About TikSnap", true));
        ScrollView sv = new ScrollView(this);
        LinearLayout b = vbox();
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setPadding(dp(20), dp(16), dp(20), dp(30));
        ImageView lg = new ImageView(this);
        lg.setImageResource(R.drawable.logo);
        b.addView(lg, U.lp(dp(104), dp(104)));
        TextView t = U.tv(this, "TikSnap", 30, T.text, true);
        U.gradText(t);
        b.addView(t, U.lpm(-2, -2, 0, 12, 0, 0));
        b.addView(U.tv(this, "Version " + version(), 14, T.sub, false));
        TextView d = U.tv(this, "Snap it. Tik it. Share it.\nShort moments, big vibes \u2014 a social app that works fully on your device, no account and no internet needed.", 15, T.text, false);
        d.setGravity(Gravity.CENTER);
        b.addView(d, U.lpm(-2, -2, 0, 14, 0, 18));
        LinearLayout c = card();
        c.addView(srow("Developer", null, U.tv(this, "ByteSnap Inc.", 14, T.text, true), null));
        c.addView(srow("Publisher", null, U.tv(this, "ByteSnap Inc.", 14, T.text, true), null));
        c.addView(srow("Package", null, U.tv(this, "com.bytesnap.tiksnap", 13, T.sub, false), null));
        c.addView(srow("Your data", null, U.tv(this, "Stored on this device", 13, T.sub, false), null));
        b.addView(c, U.lp(-1, -2));
        TextView legal = U.tv(this, "\u00A9 2026 ByteSnap Inc. All rights reserved.\n\nTikSnap is a fictional demonstration app. All people, posts and activity shown are fictional, and TikSnap is not affiliated with or endorsed by any other social platform.", 12, T.sub, false);
        legal.setGravity(Gravity.CENTER);
        b.addView(legal, U.lpm(-1, -2, 0, 22, 0, 0));
        sv.addView(b);
        col.addView(sv, U.lp(-1, 0, 1f));
        return col;
    }
}
