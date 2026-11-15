package net.kdt.pojavlaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModsActivity extends Activity {
    private static final String API = "https://api.modrinth.com/v2";
    private static final String UA = "CleanLauncher/1.0";
    private static final String[] TYPE_KEYS = {"mod", "resourcepack", "shader", "datapack"};
    private static final String[] TYPE_DIRS = {"mods", "resourcepacks", "shaderpacks", "datapacks"};
    private static final String[] LOADERS = {"fabric", "forge", "neoforge", "quilt"};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<JSONObject> hits = new ArrayList<>();
    private final Set<String> installedIds = new HashSet<>();
    private final List<String> versionList = new ArrayList<>();
    private EditText queryBox;
    private Spinner typeSp, verSp, loaderSp;
    private ArrayAdapter<String> verAdapter;
    private TextView status;
    private HitAdapter adapter;
    private File gameDir;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gameDir = Tools.getGameDirPath(LauncherProfiles.getCurrentProfile());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF181818);
        int p = dp(12);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Mods");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        queryBox = new EditText(this);
        queryBox.setHint("Search mods");
        queryBox.setSingleLine(true);
        queryBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) {
                ui.removeCallbacksAndMessages(null);
                ui.postDelayed(ModsActivity.this::search, 400);
            }
        });
        root.addView(queryBox);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        typeSp = spinner(new String[]{"Mods", "Resource packs", "Shaders", "Data packs"});
        verAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, versionList);
        verSp = new Spinner(this);
        verSp.setAdapter(verAdapter);
        loaderSp = spinner(LOADERS);
        filters.addView(typeSp, weight());
        filters.addView(verSp, weight());
        filters.addView(loaderSp, weight());
        root.addView(filters);

        status = new TextView(this);
        status.setTextColor(0xFFAAAAAA);
        status.setTextSize(12);
        root.addView(status);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new HitAdapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button manage = new Button(this);
        manage.setText("Installed mods");
        manage.setAllCaps(false);
        manage.setOnClickListener(v -> startActivity(new android.content.Intent(this, ModsManagerActivity.class)));
        root.addView(manage, 1);

        setContentView(root);

        AdapterView.OnItemSelectedListener change = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { search(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        typeSp.setOnItemSelectedListener(change);
        verSp.setOnItemSelectedListener(change);
        loaderSp.setOnItemSelectedListener(change);

        loadVersions();
        scanInstalled();
        search();
    }

    private void loadVersions() {
        new Thread(() -> {
            try {
                JSONArray arr = new JSONArray(get(API + "/tag/game_version"));
                final List<String> out = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    if ("release".equals(o.optString("version_type"))) out.add(o.getString("version"));
                }
                ui.post(() -> {
                    versionList.clear();
                    versionList.addAll(out);
                    verAdapter.notifyDataSetChanged();
                    if (!versionList.isEmpty()) verSp.setSelection(0);
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private String currentType() { return TYPE_KEYS[typeSp.getSelectedItemPosition()]; }
    private String currentVersion() {
        int i = verSp.getSelectedItemPosition();
        return (i >= 0 && i < versionList.size()) ? versionList.get(i) : "1.21.11";
    }
    private String currentLoader() { return LOADERS[loaderSp.getSelectedItemPosition()]; }

    // يحفظ معرّفات المشاريع المثبّتة بالاسم (slug) من ملف معرّف بجانب كل ملف
    private void scanInstalled() {
        new Thread(() -> {
            Set<String> found = new HashSet<>();
            File dir = new File(gameDir, TYPE_DIRS[typeIndex(currentType())]);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".modid")) {
                        found.add(f.getName().replace(".modid", ""));
                    }
                }
            }
            ui.post(() -> {
                installedIds.clear();
                installedIds.addAll(found);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void search() {
        final String q = queryBox.getText().toString().trim();
        final String type = currentType();
        final String ver = currentVersion();
        final String loader = currentLoader();
        status.setText("Loading...");
        new Thread(() -> {
            try {
                StringBuilder f = new StringBuilder("[[\"project_type:" + type + "\"],[\"versions:" + ver + "\"]");
                if (type.equals("mod")) f.append(",[\"categories:").append(loader).append("\"]");
                f.append("]");
                String url = API + "/search?query=" + enc(q) + "&limit=30&facets=" + enc(f.toString());
                JSONArray arr = new JSONObject(get(url)).getJSONArray("hits");
                final List<JSONObject> got = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) got.add(arr.getJSONObject(i));
                ui.post(() -> {
                    hits.clear();
                    hits.addAll(got);
                    adapter.notifyDataSetChanged();
                    status.setText(got.isEmpty() ? "No results" : got.size() + " results");
                    scanInstalled();
                });
            } catch (Exception e) {
                ui.post(() -> status.setText("Network: " + e.getMessage()));
            }
        }).start();
    }

    // نافذة اختيار الإصدار تظهر دائماً، حتى لو لم توجد إصدارات محمّلة
    private void onDownloadClick(final JSONObject hit) {
        final File versionsDir = new File(gameDir, "versions");
        final File[] dirs = versionsDir.listFiles(File::isDirectory);
        final String[] names;
        if (dirs == null || dirs.length == 0) {
            names = new String[]{"No downloaded versions"};
        } else {
            names = new String[dirs.length];
            for (int i = 0; i < dirs.length; i++) names[i] = dirs[i].getName();
        }
        new AlertDialog.Builder(this)
            .setTitle("Choose a version")
            .setItems(names, (d, which) -> {
                if (dirs == null || dirs.length == 0) {
                    Toast.makeText(this, "No downloaded versions. Launch a version first.", Toast.LENGTH_LONG).show();
                    return;
                }
                installTo(hit, names[which]);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void installTo(final JSONObject hit, final String versionName) {
        final String type = currentType();
        final String ver = gameVersionOf(versionName);
        final String loader = currentLoader();
        final String pid = hit.optString("project_id");
        final String name = hit.optString("title");
        status.setText("Installing " + name + "...");
        new Thread(() -> {
            try {
                List<String> done = new ArrayList<>();
                int count = installProject(pid, type, ver, loader, 0, done, versionName);
                ui.post(() -> {
                    installedIds.add(hit.optString("slug"));
                    scanInstalled();
                    status.setText("Installed " + count + " file(s)");
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                ui.post(() -> status.setText("Failed: " + e.getMessage()));
            }
        }).start();
    }

    // يحمّل المشروع واعتمادياته المطلوبة تلقائياً، ويتخطى ما هو موجود
    private int installProject(String pid, String type, String ver, String loader, int depth, List<String> done, String versionName) throws Exception {
        if (depth > 4 || done.contains(pid)) return 0;
        done.add(pid);
        String loaders = type.equals("mod") ? "&loaders=" + enc("[\"" + loader + "\"]") : "";
        JSONArray vs = new JSONArray(get(API + "/project/" + pid + "/version?game_versions=" + enc("[\"" + ver + "\"]") + loaders));
        if (vs.length() == 0) throw new Exception("no compatible version for " + ver);
        JSONObject v = vs.getJSONObject(0);
        JSONArray files = v.getJSONArray("files");
        JSONObject file = files.getJSONObject(0);
        for (int i = 0; i < files.length(); i++) {
            if (files.getJSONObject(i).optBoolean("primary")) { file = files.getJSONObject(i); break; }
        }
        File dir = new File(new File(gameDir, "versions/" + versionName), TYPE_DIRS[typeIndex(type)]);
        dir.mkdirs();
        File out = new File(dir, file.getString("filename"));
        int count = 0;
        if (!out.exists()) {
            download(file.getString("url"), out);
            count++;
        }
        new File(dir, out.getName() + ".modid").createNewFile();
        JSONArray deps = v.optJSONArray("dependencies");
        if (deps != null) {
            for (int i = 0; i < deps.length(); i++) {
                JSONObject d = deps.getJSONObject(i);
                if ("required".equals(d.optString("dependency_type")) && !d.isNull("project_id")) {
                    count += installProject(d.getString("project_id"), type, ver, loader, depth + 1, done, versionName);
                }
            }
        }
        return count;
    }

    private static int typeIndex(String t) {
        for (int i = 0; i < TYPE_KEYS.length; i++) if (TYPE_KEYS[i].equals(t)) return i;
        return 0;
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    private static void download(String url, File out) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(15000);
        c.setReadTimeout(60000);
        try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) fo.write(buf, 0, n);
        } finally {
            c.disconnect();
        }
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8");
    }

    private void loadIcon(final ImageView iv, final String url) {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setRequestProperty("User-Agent", UA);
                try (InputStream in = c.getInputStream()) {
                    final Bitmap bm = BitmapFactory.decodeStream(in);
                    if (bm != null) ui.post(() -> iv.setImageBitmap(bm));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private Spinner spinner(String[] items) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        return s;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private class HitAdapter extends BaseAdapter {
        @Override public int getCount() { return hits.size(); }
        @Override public Object getItem(int i) { return hits.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
            final JSONObject h = hits.get(i);
            LinearLayout row = new LinearLayout(ModsActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));

            ImageView icon = new ImageView(ModsActivity.this);
            row.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            String iconUrl = h.optString("icon_url", "");
            if (!iconUrl.isEmpty()) loadIcon(icon, iconUrl);

            LinearLayout texts = new LinearLayout(ModsActivity.this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(10), 0, dp(6), 0);
            TextView t1 = new TextView(ModsActivity.this);
            t1.setText(h.optString("title"));
            t1.setTextColor(0xFFFFFFFF);
            t1.setTextSize(15);
            TextView t2 = new TextView(ModsActivity.this);
            t2.setText("by " + h.optString("author"));
            t2.setTextColor(0xFFAAAAAA);
            t2.setTextSize(12);
            texts.addView(t1);
            texts.addView(t2);
            row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            boolean done = installedIds.contains(h.optString("slug", ""));
            Button btn = new Button(ModsActivity.this);
            btn.setAllCaps(false);
            btn.setText(done ? "Downloaded" : "Download");
            btn.setEnabled(!done);
            btn.setOnClickListener(v -> onDownloadClick(h));
            row.addView(btn);
            return row;
        }
    }

    // يستخرج إصدار اللعبة من اسم مجلد الإصدار المحمّل (مثل 1.7.10)
    private String gameVersionOf(String versionName) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)").matcher(versionName);
        return m.find() ? m.group(1) : currentVersion();
    }
}
