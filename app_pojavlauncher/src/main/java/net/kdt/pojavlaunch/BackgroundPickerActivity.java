package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.preference.PreferenceManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BackgroundPickerActivity extends Activity {
    public static final String KEY = "launcher_background";
    private final List<String> names = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF181818);
        int p = dp(8);
        root.setPadding(p, p, p, p);
        TextView title = new TextView(this);
        title.setText("Background");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        try {
            for (String n : getAssets().list("backgrounds")) if (n.endsWith(".jpg") || n.endsWith(".png")) names.add(n);
        } catch (Exception ignored) {}
        android.widget.Button noneBtn = new android.widget.Button(this);
        noneBtn.setText("None");
        noneBtn.setAllCaps(false);
        noneBtn.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(BackgroundPickerActivity.this).edit().putString(KEY, "none").apply();
            setResult(RESULT_OK);
            finish();
        });
        root.addView(noneBtn);
        GridView grid = new GridView(this);
        grid.setNumColumns(3);
        grid.setVerticalSpacing(dp(6));
        grid.setHorizontalSpacing(dp(6));
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return names.size(); }
            @Override public Object getItem(int i) { return names.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View convert, ViewGroup parent) {
                ImageView iv = convert instanceof ImageView ? (ImageView) convert : new ImageView(BackgroundPickerActivity.this);
                iv.setLayoutParams(new GridView.LayoutParams(dp(110), dp(190)));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                try (InputStream in = getAssets().open("backgrounds/" + names.get(i))) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inSampleSize = 2;
                    Bitmap bm = BitmapFactory.decodeStream(in, null, o);
                    iv.setImageBitmap(bm);
                } catch (Exception ignored) {}
                if (names.get(i).equals("none")) {
                    iv.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xFF181818));
                }
                iv.setOnClickListener(v -> {
                    PreferenceManager.getDefaultSharedPreferences(BackgroundPickerActivity.this).edit().putString(KEY, names.get(i)).apply();
                    setResult(RESULT_OK);
                    finish();
                });
                return iv;
            }
        });
        android.widget.Button imp = new android.widget.Button(this);
        imp.setText("Import from device");
        imp.setAllCaps(false);
        imp.setOnClickListener(v -> pickFromDevice());
        root.addView(imp);
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private static final int REQ_IMPORT = 6101;

    private void pickFromDevice() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMPORT || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            java.io.File dir = new java.io.File(getFilesDir(), "custom_backgrounds");
            dir.mkdirs();
            java.io.File out = new java.io.File(dir, "custom_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = getContentResolver().openInputStream(data.getData());
                 java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(KEY, "file:" + out.getAbsolutePath()).apply();
            setResult(RESULT_OK);
            finish();
        } catch (Exception ignored) {
        }
    }
}
