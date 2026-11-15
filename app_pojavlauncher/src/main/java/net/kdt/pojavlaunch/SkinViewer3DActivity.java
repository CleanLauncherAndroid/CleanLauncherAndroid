package net.kdt.pojavlaunch;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SkinViewer3DActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getStringExtra("path");
        final boolean slim = getIntent().getBooleanExtra("slim", false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Button back = new Button(this);
        back.setAllCaps(false);
        back.setText(getString(R.string.skin_back_menu));
        back.setOnClickListener(v -> finish());
        root.addView(back);
        WebView web = new WebView(this);
        web.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        web.setBackgroundColor(0xFF181818);
        web.getSettings().setJavaScriptEnabled(true);
        root.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        String data = "";
        boolean legacy = false;
        try {
            byte[] bytes = readAll(new File(path));
            data = "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
            legacy = o.outHeight == 32;
        } catch (Exception ignored) {
        }
        final String js = "setSkin('" + data + "'," + slim + "," + legacy + ")";
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(js, null);
            }
        });
        web.loadUrl("file:///android_asset/skin3d.html");
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }
}
