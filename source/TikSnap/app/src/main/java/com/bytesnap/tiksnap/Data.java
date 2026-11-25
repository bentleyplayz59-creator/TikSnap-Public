package com.bytesnap.tiksnap;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

class User {
    String id, name, bio, emoji, avatarPath, email = "";
    boolean verified;
    int followers, following, seed;
    User(String id, String name, String emoji, int seed, String bio, int fo, int fg) {
        this.id = id; this.name = name; this.emoji = emoji; this.seed = seed; this.bio = bio; this.followers = fo; this.following = fg;
    }
}

class Comment {
    String user, text; long ts;
    Comment(String u, String t, long ts) { user = u; text = t; this.ts = ts; }
}

class Post {
    long id; String user, caption, imagePath, posterPath, emoji; long ts; int seed, baseLikes, imageRes;
    List<String> tags = new ArrayList<String>();
    List<Comment> comments = new ArrayList<Comment>();
    boolean mine;
}

class Notif {
    String type, user, text, emoji; long ts; int seed;
    Notif(String type, String user, String text, long ts, int seed, String emoji) {
        this.type = type; this.user = user; this.text = text; this.ts = ts; this.seed = seed; this.emoji = emoji;
    }
}

class Data {
    static final String ME = "__me__";
    static SharedPreferences sp;
    static Context ctx;
    static List<User> users = new ArrayList<User>();
    static Map<String, User> byId = new HashMap<String, User>();
    static List<Post> sample = new ArrayList<Post>();
    static List<Post> mine = new ArrayList<Post>();
    static Set<String> liked = new HashSet<String>();
    static Set<String> followed = new LinkedHashSet<String>();
    static User me;
    static final String[][] TOPICS = {
        {"Night owl photography", "#nightvibes", "82.4K posts"},
        {"Pocket-size ramen shops", "#foodtok", "61.9K posts"},
        {"Pets doing the most", "#pets", "140K posts"},
        {"City lights after rain", "#citylights", "47.3K posts"},
        {"Shipped it! Indie dev wins", "#devlife", "23.8K posts"}
    };
    static final String[] STORY_LINES = {"Golden hour is calling", "Behind the scenes", "Quick snap from today", "Not sorry for the spam", "Vibes only", "Look what I found", "Weekend mode: on", "Say cheese"};

    static void init(Context c) {
        ctx = c.getApplicationContext();
        sp = ctx.getSharedPreferences("tiksnap", 0);
        long now = System.currentTimeMillis();
        if (!sp.contains("seen")) sp.edit().putLong("seen", now - 45 * 60000L).commit();
        users.clear(); byId.clear(); sample.clear(); mine.clear(); liked.clear(); followed.clear();
        u("luna.vibes", "Luna Park", "LP", 1, "Night owl. Moon photographer. DMs open for collabs \u2728", 128400, 312);
        u("neon.nomad", "Kai Mercer", "KM", 3, "Chasing city lights across 30 countries.", 86200, 540);
        u("pixelpaws", "Mina Okafor", "MO", 4, "Mom to Biscuit the corgi and Pepper the fox. Pet content only.", 312000, 98);
        u("chef.zane", "Zane Rivera", "ZR", 2, "Line cook by day, broth obsessive by night.", 54300, 221);
        u("skyline_sam", "Sam Whitfield", "SW", 12, "Architecture nerd. Blue hour addict.", 41800, 402);
        u("glitchgarden", "Ivy Chen", "IC", 5, "Balcony jungle. 74 plants and counting.", 22900, 310);
        u("retro.rae", "Rae Donovan", "RD", 8, "Analog soul in a digital world.", 17600, 188);
        u("drift.dax", "Dax Moreno", "DM", 6, "Mountain roads and manual gearboxes.", 66100, 77);
        u("byte.bella", "Bella Novak", "BN", 10, "Indie dev. Shipping apps and snacks.", 29400, 265);
        u("tidal.theo", "Theo Park", "TP", 9, "Dawn patrol surfer. Water is life.", 38800, 150);
        u("cosmicjo", "Jo Alvarez", "JA", 7, "Astrophotography from my backyard.", 71500, 64);
        u("sunny.sol", "Sol Hart", "SH", 11, "Soft mornings, strong coffee.", 12300, 410);

        u("avery.afterdark", "Avery Cole", "AC", 31, "Collecting screenshots and moments worth keeping.", 28400, 312);
        u("jordanmakes", "Jordan Ellis", "JE", 32, "Here for the unexpected plot twists.", 19200, 418);
        u("maya.inmotion", "Maya Bennett", "MB", 33, "A little humor for the daily scroll.", 58300, 206);
        u("theo.frames", "Theo Grant", "TG", 34, "Finding the funny in ordinary days.", 31700, 291);
        u("nina.notes", "Nina Brooks", "NB", 35, "Curated chaos, one post at a time.", 42800, 184);
        u("eli.jpg", "Eli Carter", "EC", 36, "Internet culture enthusiast.", 15600, 533);
        u("sofia.says", "Sofia Reyes", "SR", 37, "Good timing. Better captions.", 67100, 352);
        u("marcusoffline", "Marcus Lee", "ML", 38, "Posting what everyone was thinking.", 23900, 144);
        u("riley.reacts", "Riley Morgan", "RM", 40, "The group chat made me post this.", 49200, 391);
        u("mcdonalds", "McDonald's", "M", 41, "Official McDonald's account in TikSnap.", 125000, 12);
        u("windex", "Windex", "W", 42, "Official Windex account in TikSnap.", 18600, 8);
        byId.get("mcdonalds").verified = true;
        byId.get("windex").verified = true;

        pImage(101, "avery.afterdark", 31, 1840, 7, "This is the exact energy I brought into Monday.", "screenshot_01.jpg", "jordanmakes|Monday understood the assignment;maya.inmotion|Too accurate;theo.frames|Saving this for later");
        pImage(102, "jordanmakes", 32, 5260, 19, "Some days the group chat deserves an award.", "screenshot_02.jpg", "avery.afterdark|The group chat never misses;eli.jpg|I know this feeling;sofia.says|Absolutely");
        pImage(103, "maya.inmotion", 33, 9380, 34, "I was going to explain, but the picture says enough.", "screenshot_03.jpg", "nina.notes|No explanation needed;riley.reacts|This is painfully relatable;marcusoffline|Facts");
        pImage(104, "theo.frames", 34, 2710, 51, "A perfectly normal reaction. Nothing to see here.", "screenshot_04.jpg", "riley.reacts|That reaction is everything;maya.inmotion|Same here;eli.jpg|Exactly");
        pImage(105, "nina.notes", 35, 12400, 83, "When the plan changes but somehow gets worse.", "screenshot_05.jpg", "sofia.says|Every single time;avery.afterdark|Why is this so accurate;theo.frames|I felt that");
        pImage(106, "eli.jpg", 36, 4160, 126, "The confidence was there. The results were not.", "screenshot_06.jpg", "jordanmakes|Confidence is key;marcusoffline|The results part got me;riley.reacts|Too real");
        pImage(107, "sofia.says", 37, 7850, 184, "Trying to act surprised like I did not see this coming.", "screenshot_07.jpg", "maya.inmotion|Not even a little surprised;nina.notes|We all knew;riley.reacts|Called it");
        pImage(108, "marcusoffline", 38, 3190, 265, "A reminder that the smallest tasks can become a whole event.", "screenshot_08.jpg", "theo.frames|A full production;eli.jpg|Why is it always like this;avery.afterdark|Too familiar");
        pImage(110, "windex", 42, 6420, 615, "New game loading. Get your setup ready. Windex Gaming Wipes keep your screens and gear looking fresh.", "Windex.jpg", "nina.notes|That setup is ready;marcusoffline|Clean setup;eli.jpg|The gaming wipes ad is back");
        pImage(111, "mcdonalds", 41, 24100, 2, "A little extra joy can turn an ordinary day around. Thanks for making us part of yours.", "McDonalds.jpg", "avery.afterdark|The fries-and-friends kind of day;jordanmakes|Classic stop;maya.inmotion|Always a good idea");


        String me_id = sp.getString("me_id", "alex.tiksnap");
        me = new User(ME, sp.getString("me_name", "Alex Rivera"), "AR", 20, sp.getString("me_bio", "Just getting started on TikSnap \u2728\nSnap it. Tik it. Share it."), sp.getInt("profile_followers", 1284), sp.getInt("profile_following", 7));
        me.id = me_id;
        me.email = sp.getString("me_email", "alex@example.com");
        String ap = sp.getString("me_avatar", null);
        if (ap != null && new File(ap).exists()) me.avatarPath = ap;

        if (!sp.contains("followed")) {
            Set<String> d = new LinkedHashSet<String>();
            for (int i = 0; i < 6; i++) d.add(users.get(i).id);
            sp.edit().putStringSet("followed", d).commit();
        }
        followed.addAll(sp.getStringSet("followed", new HashSet<String>()));
        liked.addAll(sp.getStringSet("liked", new HashSet<String>()));

        try {
            JSONArray a = new JSONArray(sp.getString("myposts", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Post q = new Post();
                q.id = o.getLong("id"); q.user = ME; q.mine = true; q.caption = o.getString("caption");
                q.ts = o.getLong("ts"); q.imagePath = o.optString("img", null); q.seed = (int) (q.id % 97); q.emoji = "\uD83D\uDCF8";
                q.tags = tagsOf(q.caption);
                if (q.imagePath == null || new File(q.imagePath).exists()) mine.add(q);
            }
            JSONArray cm = new JSONArray(sp.getString("mycomments", "[]"));
            for (int i = 0; i < cm.length(); i++) {
                JSONObject o = cm.getJSONObject(i);
                Post t = find(o.getLong("pid"));
                if (t != null) t.comments.add(new Comment(ME, o.getString("text"), o.getLong("ts")));
            }
        } catch (Exception e) { }
        Collections.sort(mine, new Comparator<Post>() { public int compare(Post a, Post b) { return Long.compare(b.ts, a.ts); } });
    }

    static void u(String id, String name, String emoji, int seed, String bio, int fo, int fg) {
        User x = new User(id, name, emoji, seed, bio, fo, fg);
        users.add(x); byId.put(id, x);
    }

    static String copyFeedImage(String name) {
        File target = new File(ctx.getFilesDir(), "feed_" + name);
        File temp = new File(ctx.getFilesDir(), "feed_" + name + ".tmp");
        try {
            InputStream in = ctx.getAssets().open("feed/" + name);
            FileOutputStream out = new FileOutputStream(temp, false);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();
            in.close();
            out.close();
            if (target.exists() && !target.delete()) {
                temp.delete();
                return null;
            }
            if (!temp.renameTo(target)) {
                temp.delete();
                return null;
            }
            return target.getAbsolutePath();
        } catch (Exception e) {
            temp.delete();
            return null;
        }
    }

    static void pImage(long id, String user, int seed, int likes, int ageMin, String cap, String imageName, String cm) {
        p(id, user, "", seed, likes, ageMin, cap, cm);
        Post post = sample.get(sample.size() - 1);
        post.imagePath = copyFeedImage(imageName);
        if ("windex".equals(user)) post.posterPath = copyFeedImage("Windex_header.jpg");
        if ("mcdonalds".equals(user)) post.posterPath = copyFeedImage("McDonalds_header.jpg");
    }

    static void p(long id, String user, String emoji, int seed, int likes, int ageMin, String cap, String cm) {
        Post q = new Post();
        long now = System.currentTimeMillis();
        q.id = id; q.user = user; q.emoji = emoji; q.seed = seed; q.baseLikes = likes; q.ts = now - ageMin * 60000L; q.caption = cap;
        q.tags = tagsOf(cap);
        int k = 0;
        for (String s : cm.split(";")) {
            String[] pr = s.split("\\|");
            q.comments.add(new Comment(pr[0], pr[1], q.ts + (++k) * 7 * 60000L));
        }
        sample.add(q);
    }

    static List<String> tagsOf(String cap) {
        List<String> r = new ArrayList<String>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("#(\\w+)").matcher(cap);
        while (m.find()) r.add(m.group(1).toLowerCase());
        return r;
    }

    static User user(String id) {
        if (ME.equals(id)) return me;
        User x = byId.get(id);
        return x != null ? x : users.get(0);
    }

    static Post find(long id) {
        for (Post q : mine) if (q.id == id) return q;
        for (Post q : sample) if (q.id == id) return q;
        return null;
    }

    static List<Post> all() {
        List<Post> r = new ArrayList<Post>(mine);
        r.addAll(sample);
        Collections.sort(r, new Comparator<Post>() { public int compare(Post a, Post b) { return Long.compare(b.ts, a.ts); } });
        return r;
    }

    static List<Post> by(String userId) {
        List<Post> r = new ArrayList<Post>();
        if (ME.equals(userId)) { r.addAll(mine); return r; }
        for (Post q : sample) if (q.user.equals(userId)) r.add(q);
        return r;
    }

    static int likes(Post q) { return q.baseLikes + (liked.contains(String.valueOf(q.id)) ? 1 : 0); }

    static boolean isLiked(Post q) { return liked.contains(String.valueOf(q.id)); }

    static void toggleLike(Post q) {
        String k = String.valueOf(q.id);
        if (!liked.remove(k)) liked.add(k);
        sp.edit().putStringSet("liked", new HashSet<String>(liked)).commit();
    }

    static boolean isFollowing(String id) { return followed.contains(id); }

    static void toggleFollow(String id) {
        if (!followed.remove(id)) followed.add(id);
        sp.edit().putStringSet("followed", new LinkedHashSet<String>(followed)).commit();
    }

    static void addPost(String caption, String img) {
        Post q = new Post();
        q.id = System.currentTimeMillis(); q.user = ME; q.mine = true; q.caption = caption; q.ts = q.id; q.imagePath = img;
        q.seed = (int) (q.id % 97); q.emoji = "\uD83D\uDCF8"; q.tags = tagsOf(caption);
        mine.add(0, q);
        saveMine();
        try {
            JSONArray a = new JSONArray(sp.getString("notifx", "[]"));
            String[][] src = {{"like", "luna.vibes", "liked your post"}, {"comment", "neon.nomad", "commented: Love this! \uD83D\uDD25"}, {"like", "pixelpaws", "liked your post"}, {"follow", "retro.rae", "started following you"}};
            long[] dl = {25000, 70000, 140000, 200000};
            for (int i = 0; i < src.length; i++) {
                JSONObject o = new JSONObject();
                o.put("type", src[i][0]); o.put("user", src[i][1]); o.put("text", src[i][2]); o.put("ts", q.ts + dl[i]); o.put("seed", q.seed);
                a.put(o);
            }
            sp.edit().putString("notifx", a.toString()).commit();
        } catch (Exception e) { }
    }

    static void deletePost(Post q) {
        mine.remove(q);
        if (q.imagePath != null) new File(q.imagePath).delete();
        saveMine();
    }

    static void saveMine() {
        try {
            JSONArray a = new JSONArray();
            for (Post q : mine) {
                JSONObject o = new JSONObject();
                o.put("id", q.id); o.put("caption", q.caption); o.put("ts", q.ts); o.put("img", q.imagePath == null ? JSONObject.NULL : q.imagePath);
                a.put(o);
            }
            sp.edit().putString("myposts", a.toString()).commit();
        } catch (Exception e) { }
    }

    static void addComment(Post q, String text) {
        Comment c = new Comment(ME, text, System.currentTimeMillis());
        q.comments.add(c);
        try {
            JSONArray a = new JSONArray(sp.getString("mycomments", "[]"));
            JSONObject o = new JSONObject();
            o.put("pid", q.id); o.put("text", text); o.put("ts", c.ts);
            a.put(o);
            sp.edit().putString("mycomments", a.toString()).commit();
        } catch (Exception e) { }
    }

    static void saveMe(String name, String id, String bio, String email, String avatar) {
        me.name = name; me.id = id; me.bio = bio; me.email = email;
        if (avatar != null) me.avatarPath = avatar;
        sp.edit().putString("me_name", name).putString("me_id", id).putString("me_bio", bio).putString("me_email", email)
          .putString("me_avatar", me.avatarPath).commit();
    }

    static boolean validProfileCount(String value) {
        if (value == null || !value.matches("[0-9]+")) return false;
        try { return Long.parseLong(value) <= Integer.MAX_VALUE; }
        catch (Exception e) { return false; }
    }

    static void saveProfileStats(String followersValue, String followingValue) {
        SharedPreferences.Editor edit = sp.edit();
        if (validProfileCount(followersValue)) {
            me.followers = Integer.parseInt(followersValue);
            edit.putInt("profile_followers", me.followers);
        }
        if (validProfileCount(followingValue)) {
            me.following = Integer.parseInt(followingValue);
            edit.putInt("profile_following", me.following);
        }
        edit.commit();
    }

    static boolean pref(String k) { return sp.getBoolean(k, true); }

    static List<Notif> notifs() {
        long now = System.currentTimeMillis();
        List<Notif> r = new ArrayList<Notif>();
        r.add(new Notif("follow", "luna.vibes", "started following you", now - 12 * 60000L, 1, "\uD83C\uDF19"));
        r.add(new Notif("like", "neon.nomad", "liked your photo", now - 26 * 60000L, 3, "\uD83C\uDF06"));
        r.add(new Notif("comment", "byte.bella", "commented: This is so good \uD83D\uDC9C", now - 38 * 60000L, 10, "\uD83D\uDC9C"));
        r.add(new Notif("other", "chef.zane", "mentioned you in a comment", now - 70 * 60000L, 2, "\uD83C\uDF5C"));
        r.add(new Notif("like", "pixelpaws", "and 24 others liked your post", now - 3 * 3600000L, 4, "\uD83D\uDC3E"));
        r.add(new Notif("follow", "cosmicjo", "started following you", now - 5 * 3600000L, 7, "\uD83E\uDE90"));
        r.add(new Notif("other", "tidal.theo", "viewed your story", now - 9 * 3600000L, 9, "\uD83C\uDF0A"));
        r.add(new Notif("comment", "sunny.sol", "commented: Where is this?? \uD83D\uDE0D", now - 26 * 3600000L, 11, "\u2600\uFE0F"));
        r.add(new Notif("like", "drift.dax", "liked your photo", now - 30 * 3600000L, 6, "\uD83D\uDE97"));
        r.add(new Notif("follow", "skyline_sam", "started following you", now - 50 * 3600000L, 12, "\uD83C\uDFD9"));
        r.add(new Notif("other", "glitchgarden", "shared your post to their story", now - 75 * 3600000L, 5, "\uD83C\uDF3F"));
        try {
            JSONArray a = new JSONArray(sp.getString("notifx", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (o.getLong("ts") <= now) r.add(new Notif(o.getString("type"), o.getString("user"), o.getString("text"), o.getLong("ts"), o.getInt("seed"), "\uD83D\uDCF8"));
            }
        } catch (Exception e) { }
        List<Notif> f = new ArrayList<Notif>();
        for (Notif n : r) {
            String k = n.type.equals("like") ? "n_likes" : n.type.equals("comment") ? "n_comments" : n.type.equals("follow") ? "n_follow" : "n_other";
            if (pref(k)) f.add(n);
        }
        Collections.sort(f, new Comparator<Notif>() { public int compare(Notif a, Notif b) { return Long.compare(b.ts, a.ts); } });
        return f;
    }

    static long seen() { return sp.getLong("seen", 0); }

    static int unseen() {
        int c = 0;
        for (Notif n : notifs()) if (n.ts > seen()) c++;
        return c;
    }

    static void markSeen() { sp.edit().putLong("seen", System.currentTimeMillis()).commit(); }

    static List<String> tags() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (Post q : all()) for (String t : q.tags) m.put(t, (m.containsKey(t) ? m.get(t) : 0) + 1);
        List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(m.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int c = b.getValue() - a.getValue();
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        List<String> r = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : l) r.add(e.getKey());
        return r;
    }

    static List<User> searchUsers(String q) {
        q = q.toLowerCase().replace("@", "").trim();
        List<User> r = new ArrayList<User>();
        for (User x : users) if (x.id.contains(q) || x.name.toLowerCase().contains(q) || x.bio.toLowerCase().contains(q)) r.add(x);
        return r;
    }

    static List<Post> searchPosts(String q) {
        q = q.toLowerCase().trim();
        boolean tag = q.startsWith("#");
        String t = q.replace("#", "");
        List<Post> r = new ArrayList<Post>();
        for (Post p : all()) {
            if (tag ? p.tags.contains(t) : (p.caption.toLowerCase().contains(q) || p.tags.contains(t) || user(p.user).id.contains(q))) r.add(p);
        }
        return r;
    }

    static List<String> searchTags(String q) {
        String t = q.toLowerCase().replace("#", "").trim();
        List<String> r = new ArrayList<String>();
        for (String s : tags()) if (s.contains(t)) r.add(s);
        return r;
    }

    static int tagCount(String t) {
        int c = 0;
        for (Post p : all()) if (p.tags.contains(t)) c++;
        return c * 1800 + Math.abs(t.hashCode()) % 900;
    }

    static void wipe() {
        for (Post q : new ArrayList<Post>(mine)) if (q.imagePath != null) new File(q.imagePath).delete();
        if (me != null && me.avatarPath != null) new File(me.avatarPath).delete();
        sp.edit().clear().commit();
        init(ctx);
    }
}
