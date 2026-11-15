package net.kdt.pojavlaunch.multirt;

import android.content.Context;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;

import java.io.InputStream;

/**
 * يثبّت بيئات Java المدمجة في التطبيق عند أول تشغيل.
 * Java 8 مقسّمة إلى ملف مشترك وملف للمعمارية (binpack).
 * Java 17 ملف واحد. البيئة الغائبة تُتخطى بدون خطأ.
 */
public final class InternalJreInstaller {
    private static final String TAG = "InternalJreInstaller";

    private InternalJreInstaller() {}

    public static void installIfMissing(final Context context) {
        new Thread(() -> {
            installBinpack(context, "Internal-8", "jre8");
            installSingle(context, "Internal-17", "jre17");
            installSingle(context, "Internal-21", "jre21");
        }, "jre-install").start();
    }

    private static void installBinpack(Context context, String name, String folder) {
        try {

            String version;
            try (InputStream v = context.getAssets().open("components/" + folder + "/version")) {
                version = new String(readAll(v), "UTF-8").trim();
            }
            try (InputStream universal = context.getAssets().open("components/" + folder + "/universal.tar.xz");
                 InputStream bins = context.getAssets().open("components/" + folder + "/bin-arm64.tar.xz")) {
                MultiRTUtils.installRuntimeNamedBinpack(universal, bins, name, version);
                Log.i(TAG, "Installed " + name);
            }
        } catch (Exception e) {
            android.util.Log.e("JRE21_ERR", "Skipping " + name, e);
        }
    }

    private static void installSingle(Context context, String name, String folder) {
        try {

            try (InputStream in = context.getAssets().open("components/" + folder + "/" + folder + ".tar.xz")) {
                MultiRTUtils.installRuntimeNamed(Tools.NATIVE_LIB_DIR, in, name);
                Log.i(TAG, "Installed " + name);
            }
        } catch (Exception e) {
            Log.w(TAG, "Skipping " + name + ": " + e.getMessage());
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
