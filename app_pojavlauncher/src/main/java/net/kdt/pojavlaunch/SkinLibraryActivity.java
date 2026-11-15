package net.kdt.pojavlaunch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import net.kdt.pojavlaunch.skin.SkinPackManager;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** مكتبة السكنات: تعرض كل ملفات PNG الموجودة في مجلد المكتبة بمعاينة أمامية 2D. */
public class SkinLibraryActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    private File gameDir, skinsDir, libDir;
    private final List<File> items = new ArrayList<>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameDir = Tools.getGameDirPath(LauncherProfiles.getCurrentProfile());
        SkinPackManager.setActiveKey(LauncherProfiles.mainProfileJson.currentKey());
        SkinPackManager.ensureMigrated(gameDir);
        skinsDir = new File(gameDir, SkinPackManager.skinSubdir());
        skinsDir.mkdirs();
        libDir = new File(getExternalFilesDir(null), "skin_library");
        libDir.mkdirs();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF181818);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText(getString(R.string.skin_library_title));
        title.setTextSize(20);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setBackgroundColor(0xFF232323);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView empty = new TextView(this);
        empty.setTextColor(0xFFAAAAAA);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(8), dp(12), dp(8), dp(12));
        empty.setText(getString(R.string.skin_library_empty, libDir.getAbsolutePath()));
        root.addView(empty);
        list.setEmptyView(empty);
        setContentView(root);

        adapter = new BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public Object getItem(int i) { return items.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override
            public View getView(int i, View convert, ViewGroup parent) {
                LinearLayout row = new LinearLayout(SkinLibraryActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(8), dp(8), dp(8));
                File f = items.get(i);
                SkinManagerActivity.SkinPreviewView pv = new SkinManagerActivity.SkinPreviewView(SkinLibraryActivity.this);
                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                String mdl = SkinManagerActivity.detect(bm);
                pv.setSkin(bm, "SLIM".equals(mdl), false);
                row.addView(pv, new LinearLayout.LayoutParams(dp(70), dp(110)));
                TextView name = new TextView(SkinLibraryActivity.this);
                name.setText(f.getName().replace(".png", "") + "  \u00b7  " + ("SLIM".equals(SkinManagerActivity.detect(BitmapFactory.decodeFile(f.getAbsolutePath()))) ? "Slim" : "Wide"));
                name.setTextColor(0xFFFFFFFF);
                name.setTextSize(15);
                name.setPadding(dp(12), 0, 0, 0);
                row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> importSkin(items.get(pos)));
        reload();
    }

    private void reload() {
        items.clear();
        File dir = new File(getCacheDir(), "skin_library");
        dir.mkdirs();
        try {
            String[] names = getAssets().list("skin_library");
            if (names != null) {
                for (String n : names) {
                    if (!n.toLowerCase().endsWith(".png")) continue;
                    File out = new File(dir, n);
                    if (!out.exists()) copyAsset("skin_library/" + n, out);
                    items.add(out);
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(items);
        adapter.notifyDataSetChanged();
    }

    private void copyAsset(String path, File out) throws java.io.IOException {
        try (InputStream in = getAssets().open(path);
             java.io.FileOutputStream os = new java.io.FileOutputStream(out)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) != -1) os.write(b, 0, n);
        }
    }

    private void importSkin(File f) {
        String base = f.getName().replace(".png", "").replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.isEmpty()) base = "skin";
        String name = base;
        int k = 2;
        while (new File(skinsDir, name + ".png").exists()) name = base + "_" + k++;
        try (InputStream in = new FileInputStream(f)) {
            SkinPackManager.addSkin(gameDir, in, name);
            Bitmap bmpM = BitmapFactory.decodeFile(f.getAbsolutePath());
            String model = SkinManagerActivity.detect(bmpM);
            try (java.io.FileOutputStream mo = new java.io.FileOutputStream(new File(skinsDir, name + ".model"))) {
                mo.write(model.getBytes("UTF-8"));
            }
            Toast.makeText(this, getString(R.string.skin_library_added), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "error" : e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
