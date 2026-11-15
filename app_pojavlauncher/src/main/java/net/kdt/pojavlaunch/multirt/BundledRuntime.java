package net.kdt.pojavlaunch.multirt;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.InputStream;

/**
 * يثبّت بيئة Java 17 المرفقة داخل التطبيق (assets/jre/jre17-arm64.tar.xz)
 * في مجلد الـ runtimes، فتظهر تلقائياً لاختيارها عند تشغيل اللعبة.
 */
public class BundledRuntime {
    public static final String NAME = "Bundled-JRE17";
    private static final String ASSET = "jre/jre17-arm64.tar.xz";

    public static void ensureInstalled(Context context) {
        try {
            if (!isArm64()) {
                Log.i("BundledRuntime", "Device is not arm64, skipping bundled JRE");
                return;
            }
            File release = new File(Tools.MULTIRT_HOME, NAME + "/release");
            if (release.exists()) return;

            try (InputStream in = context.getAssets().open(ASSET)) {
                MultiRTUtils.installRuntimeNamed(Tools.NATIVE_LIB_DIR, in, NAME);
            }
            Log.i("BundledRuntime", "Installed " + NAME);
        } catch (Throwable t) {
            Log.e("BundledRuntime", "Failed to install bundled JRE", t);
        }
    }

    private static boolean isArm64() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }
}
