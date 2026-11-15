package net.kdt.pojavlaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.kdt.pojavlaunch.skin.SkinPackManager;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** شاشة السكنات: رفع سكن، تحديد نوعه، معاينته أمام/خلف، وحفظ عدة سكنات لكل بروفايل. */
public class SkinManagerActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    private static final int REQ_PICK = 4721;
    private static final int REQ_LIB = 4722;
    private String[] modelLabels() { return new String[]{"Slim (Alex)", "Wide (Steve)", getString(R.string.skin_model_auto)}; }

    private File gameDir;
    private File skinsDir() {
        File d = new File(net.kdt.pojavlaunch.Tools.getGameDirPath(net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.getCurrentProfile()),
                SkinPackManager.skinSubdir());
        d.mkdirs();
        return d;
    }
    private final List<String> names = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView list;
    private SkinPreviewView preview;
    private TextView status;
    private String current;
    private boolean backView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameDir = Tools.getGameDirPath(LauncherProfiles.getCurrentProfile());
        SkinPackManager.setActiveKey(SkinPackManager.accountKey(this));
        SkinPackManager.ensureMigrated(gameDir);
        
        skinsDir().mkdirs();
        setContentView(buildUi());
        refresh();
    }

    public static String pickedKeyGlobal = null;
    private Spinner profileSpinner;
    private String pickedKey = null;

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackgroundColor(0xFF181818);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton home = new ImageButton(this);
        home.setImageResource(R.drawable.ic_home_small);
        home.setBackgroundColor(0x00000000);
        home.setContentDescription(getString(R.string.skin_back_menu));
        home.setOnClickListener(v -> finish());
        top.addView(home, new LinearLayout.LayoutParams(dp(44), dp(44)));
        top.addView(text(getString(R.string.skins_title), 20, true), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(top);

        Button upload = button(getString(R.string.skin_upload));
        upload.setOnClickListener(v -> pickFile());
        Button library = button(getString(R.string.skin_library));
        library.setOnClickListener(v -> startActivityForResult(new Intent(this, SkinLibraryActivity.class), REQ_LIB));
        LinearLayout uploadRow = new LinearLayout(this);
        uploadRow.setOrientation(LinearLayout.HORIZONTAL);
        uploadRow.addView(upload, weight());
        uploadRow.addView(library, weight());
        root.addView(uploadRow);

        list = new ListView(this);
        list.setBackgroundColor(0xFF232323);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, names);
        list.setAdapter(adapter);
        // قائمة السكنات مخفية

        preview = new SkinPreviewView(this);
        root.addView(preview, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230)));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button front = button(getString(R.string.skin_front));
        Button back = button(getString(R.string.skin_back));
        Button view3d = button(getString(R.string.skin_3d));
        front.setOnClickListener(v -> { backView = false; showPreview(); });
        back.setOnClickListener(v -> { backView = true; showPreview(); });
        view3d.setOnClickListener(v -> open3d());
        row1.addView(front, weight());
        row1.addView(back, weight());
        row1.addView(view3d, weight());
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button confirm = button(getString(R.string.skin_confirm));
        Button delete = button(getString(R.string.skin_delete));
        confirm.setOnClickListener(v -> confirmSelected());
        delete.setOnClickListener(v -> deleteSelected());
        row2.addView(confirm, weight());
        row2.addView(delete, weight());
        root.addView(row2);

        list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, names);
        list.setAdapter(adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            current = names.get(pos);
            showPreview();
        });
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160)));
        status = text(getString(R.string.skin_hint), 12, false);
        root.addView(status);
        profileSpinner = new Spinner(this);
        final java.util.List<String> accKeys = new java.util.ArrayList<>();
        final java.util.List<String> accLabels = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        try {
            java.io.File accDir = new java.io.File(net.kdt.pojavlaunch.Tools.DIR_ACCOUNT_NEW);
            java.io.File[] listed = accDir.listFiles();
            if (listed != null) {
                for (java.io.File af : listed) {
                    String nm = af.getName();
                    if (nm.endsWith(".json")) nm = nm.substring(0, nm.length() - 5);
                    if (seen.add(nm)) { accKeys.add(SkinPackManager.sanitize(nm)); accLabels.add(nm); }
                }
            }
        } catch (Exception ignored) {
        }
        if (accKeys.isEmpty()) { accKeys.add("default"); accLabels.add(getString(R.string.skin_profile_label)); }
        ArrayAdapter<String> pa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, accLabels);
        profileSpinner.setAdapter(pa);
        String curAcc = SkinPackManager.accountKey(this);
        int sel = accKeys.indexOf(curAcc);
        if (sel >= 0) profileSpinner.setSelection(sel);
        pickedKey = accKeys.get(Math.max(sel, 0));
        pickedKeyGlobal = pickedKey;
        SkinPackManager.setActiveKey(pickedKey);

        return root;
    }

    private void open3d() {
        if (current == null) {
            toast(getString(R.string.skin_pick_first));
            return;
        }
        Intent i = new Intent(this, SkinViewer3DActivity.class);
        i.putExtra("path", new File(skinsDir(), current + ".png").getAbsolutePath());
        i.putExtra("slim", "SLIM".equals(readModel(current)));
        startActivity(i);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/png");
        startActivityForResult(i, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LIB && resultCode == RESULT_OK) { refresh(); return; }
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            handleUpload(data.getData());
        }
    }

    private void applyNow(String name) {
        current = name;
        try {
            SkinPackManager.select(skinsDir(), name);
            Toast.makeText(this, getString(R.string.skin_applied, name), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.skin_apply_error), Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }

    private void handleUpload(Uri uri) {
        try {
            byte[] bytes = readBytes(uri);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp == null || bmp.getWidth() != 64 || (bmp.getHeight() != 64 && bmp.getHeight() != 32)) {
                toast(getString(R.string.skin_bad_size));
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.skin_type_title))
                    .setItems(modelLabels(), (d, which) -> askName(bytes, bmp, which, displayName(uri)))
                    .show();
        } catch (IOException e) {
            toast(getString(R.string.skin_read_error));
        }
    }

    private void saveSkin(byte[] bytes, Bitmap bmp, int which, String rawName) {
        String name = uniqueName(sanitize(rawName));
        String model = which == 0 ? "SLIM" : which == 1 ? "WIDE" : detect(bmp);
        try {
            SkinPackManager.addSkin(gameDir, new ByteArrayInputStream(bytes), name);
            writeText(new File(skinsDir(), name + ".model"), model);
        } catch (IOException e) {
            toast(getString(R.string.skin_save_error));
            return;
        }
        refresh();
        current = name;
        selectCurrent();
        showPreview();
        status.setText(getString(R.string.skin_uploaded));
    }

    private void askName(byte[] bytes, Bitmap bmp, int which, String suggested) {
        EditText input = new EditText(this);
        input.setText(suggested);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.skin_name_title))
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = input.getText().toString();
                    saveSkin(bytes, bmp, which, n.trim().isEmpty() ? suggested : n);
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> saveSkin(bytes, bmp, which, suggested))
                .show();
    }

    private String displayName(Uri uri) {
        String n = null;
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) n = c.getString(idx);
            }
        } catch (Exception ignored) {
        }
        if (n == null || n.trim().isEmpty()) n = "skin";
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        return n;
    }

    private static String sanitize(String raw) {
        String s = raw.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return s.isEmpty() ? "skin" : s;
    }

    private String uniqueName(String base) {
        String name = base;
        int k = 2;
        while (new File(skinsDir(), name + ".png").exists()) {
            name = base + "_" + k++;
        }
        return name;
    }

    private void refresh() {
        names.clear();
        File[] files = skinsDir().listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                if (n.endsWith(".png")) names.add(n.substring(0, n.length() - 4));
            }
        }
        Collections.sort(names);
        adapter.notifyDataSetChanged();
        if (current == null && !names.isEmpty()) current = names.get(0);
        selectCurrent();
        showPreview();
    }

    private void selectCurrent() {
        if (current != null) { try { SkinPackManager.select(skinsDir().getParentFile().getParentFile(), current); } catch (Exception ignored) {} }
        String saved = SkinPackManager.selectedName(skinsDir().getParentFile().getParentFile());
        if (saved != null && names.contains(saved)) current = saved;
        if (current != null) { try { SkinPackManager.select(skinsDir(), current); } catch (Exception ignored) {} }
        // الاختيار الحالي يُعتمد مباشرة في ملف selected.txt للحساب
        int idx = names.indexOf(current);
        if (idx >= 0) list.setItemChecked(idx, true);
    }

    private void showPreview() {
        if (current == null) {
            preview.setSkin(null, false, backView);
            return;
        }
        Bitmap bmp = BitmapFactory.decodeFile(new File(skinsDir(), current + ".png").getAbsolutePath());
        preview.setSkin(bmp, "SLIM".equals(readModel(current)), backView);
    }

    private void confirmSelected() {
        if (current == null) {
            status.setText(getString(R.string.skin_pick_first));
            return;
        }
        try {
            SkinPackManager.select(gameDir, current);
            SkinPackManager.apply(gameDir, null);
            Toast.makeText(this, getString(R.string.skin_applied, current), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (IOException e) {
            status.setText(getString(R.string.skin_apply_error));
        }
    }

    private void deleteSelected() {
        if (current == null) return;
        new File(skinsDir(), current + ".png").delete();
        new File(skinsDir(), current + ".model").delete();
        try {
            if (current.equals(SkinPackManager.selectedName(gameDir))) {
                SkinPackManager.select(gameDir, "");
                SkinPackManager.apply(gameDir, null);
            }
        } catch (IOException ignored) {
        }
        current = null;
        refresh();
        status.setText(getString(R.string.skin_deleted));
    }

    private String readModel(String name) {
        File f = new File(skinsDir(), name + ".model");
        if (!f.exists()) return "WIDE";
        try {
            return new String(readBytes(f), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "WIDE";
        }
    }

    static String detect(Bitmap bmp) {
        if (bmp.getHeight() != 64) return "WIDE";
        int alpha = (bmp.getPixel(54, 20) >>> 24) & 0xFF;
        return alpha == 0 ? "SLIM" : "WIDE";
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("no stream");
            return drain(in);
        }
    }

    private static byte[] readBytes(File f) throws IOException {
        try (InputStream in = new java.io.FileInputStream(f)) {
            return drain(in);
        }
    }

    private static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static void writeText(File f, String text) throws IOException {
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(8), 0, dp(8));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setSingleLine(true);
        b.setMaxLines(1);
        b.setTextSize(13);
        b.setMinHeight(0);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(0xFF2E7FA0);
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    /** معاينة السكن بوضع أمام أو خلف (2D)، مع تقريب للأطراف الخلفية. */
    public static class SkinPreviewView extends View {
        private Bitmap skin;
        private boolean slim;
        private boolean back;
        private final Paint paint = new Paint();

        public SkinPreviewView(android.content.Context c) {
            super(c);
            paint.setFilterBitmap(false);
        }

        public void setSkin(Bitmap bmp, boolean slim, boolean back) {
            this.skin = bmp;
            this.slim = slim;
            this.back = back;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (skin == null) return;
            float s = Math.min(getWidth() / 16f, getHeight() / 32f);
            float ox = (getWidth() - 16 * s) / 2f;
            float aw = slim ? 3 : 4;
            float ax = slim ? 0.5f : 0f;
            boolean legacy = skin.getHeight() == 32;
            if (!back) {
                draw(c, 8, 8, 8, 8, ox + 4 * s, 0, 8 * s, 8 * s);
                if (!legacy) draw(c, 40, 8, 8, 8, ox + 4 * s, 0, 8 * s, 8 * s);
                draw(c, 20, 20, 8, 12, ox + 4 * s, 8 * s, 8 * s, 12 * s);
                if (!legacy) draw(c, 20, 36, 8, 12, ox + 4 * s, 8 * s, 8 * s, 12 * s);
                int[] ra = legacy ? new int[]{40, 16} : new int[]{44, 20};
                int[] la = legacy ? new int[]{40, 16} : new int[]{36, 52};
                int[] rl = legacy ? new int[]{0, 16} : new int[]{4, 20};
                int[] ll = legacy ? new int[]{0, 16} : new int[]{20, 52};
                draw(c, ra[0], ra[1], (int) aw, 12, ox + ax * s, 8 * s, aw * s, 12 * s);
                draw(c, la[0], la[1], (int) aw, 12, ox + (12 + ax) * s, 8 * s, aw * s, 12 * s);
                draw(c, rl[0], rl[1], 4, 12, ox + 4 * s, 20 * s, 4 * s, 12 * s);
                draw(c, ll[0], ll[1], 4, 12, ox + 8 * s, 20 * s, 4 * s, 12 * s);
            } else {
                draw(c, 24, 8, 8, 8, ox + 4 * s, 0, 8 * s, 8 * s);
                draw(c, 32, 20, 8, 12, ox + 4 * s, 8 * s, 8 * s, 12 * s);
                draw(c, 52, 20, (int) aw, 12, ox + (12 + ax) * s, 8 * s, aw * s, 12 * s);
                draw(c, 44, 20, (int) aw, 12, ox + ax * s, 8 * s, aw * s, 12 * s);
                draw(c, 12, 20, 4, 12, ox + 8 * s, 20 * s, 4 * s, 12 * s);
                draw(c, 4, 20, 4, 12, ox + 4 * s, 20 * s, 4 * s, 12 * s);
            }
        }

        private void draw(Canvas c, int sx, int sy, int sw, int sh, float dx, float dy, float dw, float dh) {
            Rect src = new Rect(sx, sy, sx + sw, sy + sh);
            RectF dst = new RectF(dx, dy, dx + dw, dy + dh);
            c.drawBitmap(skin, src, dst, paint);
        }
    }
}
