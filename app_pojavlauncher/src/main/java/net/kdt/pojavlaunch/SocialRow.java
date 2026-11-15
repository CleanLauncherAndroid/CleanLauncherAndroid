package net.kdt.pojavlaunch;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

public final class SocialRow {
    private static final String YT_URL = "https://www.youtube.com/@Cleannyfr";
    private static final String YT_PKG = "com.google.android.youtube";

    private SocialRow() {}

    public static void bind(View root, final Context ctx) {
        View yt = root.findViewById(R.id.btn_youtube);
        View up = root.findViewById(R.id.btn_update);
        if (yt != null) yt.setOnClickListener(v -> openYoutube(ctx));
        if (up != null) up.setOnClickListener(v ->
                Toast.makeText(ctx, ctx.getString(R.string.social_update_none), Toast.LENGTH_SHORT).show());
    }

    private static void openYoutube(Context ctx) {
        Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(YT_URL));
        app.setPackage(YT_PKG);
        app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(app);
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(YT_URL));
        web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(web);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, ctx.getString(R.string.social_no_browser), Toast.LENGTH_SHORT).show();
        }
    }
}
