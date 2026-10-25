package com.fort.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static class AppEntry {
        String label;
        Drawable icon;
        Intent intent;
    }

    /** Friendly name + package candidates for the Watch row. Only installed ones show. */
    private static final String[][] FAVORITES = {
        {"Netflix", "com.netflix.ninja"},
        {"Disney+", "com.disney.disneyplus"},
        {"YouTube", "com.google.android.youtube.tv", "com.google.android.youtube"},
        {"Hulu", "com.hulu.plus"},
        {"Prime Video", "com.amazon.avod.thirdpartyclient"},
        {"Max", "com.wbd.max", "com.hbo.hbomax"},
        {"Paramount+", "com.paramount.plus", "com.cbs.video"},
        {"Peacock", "com.peacocktv.peacock.android", "com.peacocktv.peacock"},
        {"PBS Kids", "org.pbskids.video"},
        {"Spotify", "com.spotify.tv", "com.spotify.music"},
        {"Pluto TV", "tv.pluto.android"},
        {"Apple TV", "com.apple.atve.androidtv", "com.apple.atve"},
    };

    private static final int[] TILE_BG = {
        R.drawable.tile_pink, R.drawable.tile_mint, R.drawable.tile_blue
    };

    private TextView clockView;
    private TextView ampmView;
    private TextView dateView;
    private TextView greetingView;
    private GridView grid;
    private LinearLayout favRow;
    private LinearLayout inputRow;
    private AppAdapter adapter;
    private final Handler handler = new Handler();
    private final ImageView[] stars = new ImageView[5];
    private final float[] starPhase = {0f, 1.3f, 2.7f, 4.1f, 5.5f};

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm", Locale.getDefault());
    private final SimpleDateFormat ampmFmt = new SimpleDateFormat("a", Locale.getDefault());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            twinkleStars();
            handler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clockView = (TextView) findViewById(R.id.clock);
        ampmView = (TextView) findViewById(R.id.ampm);
        dateView = (TextView) findViewById(R.id.date);
        greetingView = (TextView) findViewById(R.id.greeting);
        grid = (GridView) findViewById(R.id.grid);
        favRow = (LinearLayout) findViewById(R.id.fav_row);
        inputRow = (LinearLayout) findViewById(R.id.input_row);

        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};
        for (int i = 0; i < starIds.length; i++) {
            stars[i] = (ImageView) findViewById(starIds[i]);
        }

        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    startActivity(adapter.getItem(position).intent);
                } catch (Exception ignored) {
                }
            }
        });

        buildFavoritesRow();
        buildInputsRow();
        updateClock();
    }

    // ---------- Watch row ----------

    private void buildFavoritesRow() {
        favRow.removeAllViews();
        PackageManager pm = getPackageManager();
        int tile = 0;
        for (String[] fav : FAVORITES) {
            for (int i = 1; i < fav.length; i++) {
                String pkg = fav[i];
                if (pkg.indexOf('.') < 0) continue; // friendly name slot
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch == null) continue;
                String label;
                Drawable icon;
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    CharSequence l = pm.getApplicationLabel(info);
                    label = (l == null) ? fav[0] : l.toString();
                    icon = pm.getApplicationIcon(pkg);
                } catch (Exception e) {
                    label = fav[0];
                    icon = null;
                }
                Intent intent = new Intent(launch);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                addTile(favRow, label, icon, TILE_BG[tile % TILE_BG.length], intent);
                tile++;
                break; // first matching candidate wins
            }
        }
        if (favRow.getChildCount() == 0) {
            // nothing pinned installed; leave row empty
        }
    }

    // ---------- TV & HDMI inputs ----------

    private void buildInputsRow() {
        TextView label = (TextView) findViewById(R.id.input_label);
        View scroll = findViewById(R.id.input_scroll);
        try {
            TvInputManager tv = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tv != null) {
                List<TvInputInfo> inputs = tv.getTvInputList();
                if (inputs != null) {
                    int index = 1;
                    for (TvInputInfo info : inputs) {
                        try {
                            if (!info.isPassthroughInput()) continue;
                            String id = info.getId();
                            CharSequence l = info.loadLabel(getApplicationContext());
                            String name = (l != null) ? l.toString() : ("Input " + index);
                            Intent intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("content://android.media.tv/passthrough/" + id));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            addTile(inputRow, name,
                                    getResources().getDrawable(R.drawable.ic_hdmi),
                                    R.drawable.tile_steel, intent);
                            index++;
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // TV input service unavailable on this device
        }
        if (inputRow.getChildCount() == 0) {
            label.setVisibility(View.GONE);
            scroll.setVisibility(View.GONE);
        }
    }

    /** Inflates one colorful tile into a row. */
    private void addTile(LinearLayout row, String labelText, Drawable icon, int tileBg, final Intent launch) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_fav, row, false);
        FrameLayout tile = (FrameLayout) v.findViewById(R.id.tile);
        ImageView iv = (ImageView) v.findViewById(R.id.icon);
        TextView tv = (TextView) v.findViewById(R.id.label);
        tile.setBackgroundResource(tileBg);
        if (icon != null) iv.setImageDrawable(icon);
        tv.setText(labelText);
        v.setContentDescription(labelText);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startActivity(launch);
                } catch (Exception ignored) {
                }
            }
        });
        popOnFocus(v);
        row.addView(v);
    }

    // ---------- whimsy ----------

    /** Tiles and grid items gently grow when focused. */
    private void popOnFocus(View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                float s = hasFocus ? 1.08f : 1f;
                view.animate().scaleX(s).scaleY(s).setDuration(140).start();
            }
        });
    }

    private void twinkleStars() {
        long t = System.currentTimeMillis() / 600;
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            float a = 0.30f + 0.40f * (float) Math.sin(t / 6.0 + starPhase[i]);
            stars[i].setAlpha(a);
        }
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(timeFmt.format(now));
        ampmView.setText(ampmFmt.format(now));
        dateView.setText(dateFmt.format(now).toUpperCase(Locale.getDefault()));

        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.US).format(now));
        String greeting;
        if (hour < 5) greeting = "Up late, Whitingers?";
        else if (hour < 12) greeting = "Good morning, Whitingers!";
        else if (hour < 17) greeting = "Good afternoon, Whitingers!";
        else greeting = "Good evening, Whitingers!";
        greetingView.setText(greeting);
    }

    // ---------- all apps grid ----------

    private class AppAdapter extends BaseAdapter {
        private final List<AppEntry> apps = new ArrayList<AppEntry>();

        void reload() {
            apps.clear();
            PackageManager pm = getPackageManager();
            Set<String> seen = new HashSet<String>();

            addFromCategory(pm, seen, Intent.CATEGORY_LEANBACK_LAUNCHER);
            addFromCategory(pm, seen, Intent.CATEGORY_LAUNCHER);

            Collections.sort(apps, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
            notifyDataSetChanged();
        }

        private void addFromCategory(PackageManager pm, Set<String> seen, String category) {
            Intent probe = new Intent(Intent.ACTION_MAIN);
            probe.addCategory(category);
            List<ResolveInfo> found = pm.queryIntentActivities(probe, 0);
            if (found == null) return;
            for (ResolveInfo ri : found) {
                String pkg = ri.activityInfo.packageName;
                if (pkg == null || pkg.equals(getPackageName())) continue;
                String key = pkg + "/" + ri.activityInfo.name;
                if (!seen.add(key)) continue;
                AppEntry e = new AppEntry();
                CharSequence l = ri.loadLabel(pm);
                e.label = (l == null ? pkg : l.toString());
                e.icon = ri.loadIcon(pm);
                e.intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(pkg, ri.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                apps.add(e);
            }
        }

        @Override
        public int getCount() { return apps.size(); }
        @Override
        public AppEntry getItem(int position) { return apps.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_app, parent, false);
                popOnFocus(convertView);
            }
            AppEntry app = getItem(position);
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            TextView label = (TextView) convertView.findViewById(R.id.label);
            icon.setImageDrawable(app.icon);
            label.setText(app.label);
            convertView.setContentDescription(app.label);
            return convertView;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.reload();
        buildFavoritesRow(); // picks up newly installed channels
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }
}
