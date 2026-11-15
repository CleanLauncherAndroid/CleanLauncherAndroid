package net.kdt.pojavlaunch.skin;

import android.util.Log;

import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** سكنات كل بروفايل محفوظة في مجلد خاص به، وحزمة الموارد لكل بروفايل باسم مختلف. */
public final class SkinPackManager {
    private static final String TAG = "SkinPackManager";
    private static final String PACK_PREFIX = "CleanLauncher_skin_";
    private static final String[] DEFAULT_NAMES = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};
    private static String activeKey = "default";

    private SkinPackManager() {}

    public static void setActiveProfile(MinecraftProfile p) {
        activeKey = keyFor(p);
    }


    public static String keyOf(MinecraftProfile p) {
        String n = (p == null || p.name == null) ? "" : p.name.trim();
        return n.isEmpty() ? "default" : n.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    public static void ensureMigratedFor(File gameDir, String key) {
        try {
            File newDir = new File(gameDir, "cleanlauncher/skins/" + key);
            if (new File(newDir, "selected.txt").exists()) return;
            File legacy = new File(gameDir, "cleanlauncher/skins");
            File[] files = legacy.listFiles();
            if (files == null) return;
            newDir.mkdirs();
            for (File f : files) {
                if (!f.isFile()) continue;
                File dst = new File(newDir, f.getName());
                if (dst.exists()) continue;
                try (InputStream in = new FileInputStream(f); OutputStream os = new FileOutputStream(dst)) {
                    copy(in, os);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "migration failed", e);
        }
    }

    public static String keyFor(MinecraftProfile p) {
        try {
            for (java.util.Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                if (e.getValue() == p) return e.getKey().replaceAll("[^A-Za-z0-9_\\-]", "_");
            }
        } catch (Exception ignored) {
        }
        return "default";
    }

    public static void setActiveKey(String k) { activeKey = sanitize(k); }

    public static String sanitize(String k) {
        if (k == null || k.trim().isEmpty()) return "default";
        return k.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    public static String accountKey(android.content.Context ctx) {
        try {
            net.kdt.pojavlaunch.value.MinecraftAccount a = net.kdt.pojavlaunch.PojavProfile.getCurrentProfileContent(ctx, null);
            if (a != null && a.username != null && !a.username.trim().isEmpty()) return sanitize(a.username);
        } catch (Exception ignored) {
        }
        return "default";
    }

    public static String skinSubdir() { return "cleanlauncher/skins"; }
    private static String packFile() { return PACK_PREFIX + activeKey + ".zip"; }
    private static String selectedPath() { return skinSubdir() + "/selected.txt"; }

    public static File skinFile(File gameDir, String name) {
        return new File(gameDir, skinSubdir() + "/" + name + ".png");
    }

    public static File addSkin(File gameDir, InputStream png, String name) throws IOException {
        File out = skinFile(gameDir, name);
        out.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(out)) {
            copy(png, os);
        }
        return out;
    }

    public static void select(File gameDir, String name) throws IOException {
        File f = new File(gameDir, selectedPath());
        f.getParentFile().mkdirs();
        writeText(f, name == null ? "" : name);
    }

    public static String selectedName(File gameDir) {
        File sel = new File(gameDir, selectedPath());
        if (!sel.exists()) return null;
        try {
            String n = readText(sel).trim();
            return n.isEmpty() ? null : n;
        } catch (IOException e) {
            return null;
        }
    }

    public static File selected(File gameDir) {
        String n = selectedName(gameDir);
        if (n == null) return null;
        File skin = skinFile(gameDir, n);
        return skin.exists() ? skin : null;
    }

    /** ينسخ السكنات القديمة (المشتركة) إلى مجلد هذا البروفايل مرة واحدة فقط. */
    public static void ensureMigrated(File gameDir) {
        try {
            File newDir = new File(gameDir, skinSubdir());
            if (new File(newDir, "selected.txt").exists()) return;
            File legacy = new File(gameDir, "cleanlauncher/skins");
            File[] files = legacy.listFiles();
            if (files == null) return;
            newDir.mkdirs();
            for (File f : files) {
                if (!f.isFile()) continue;
                File dst = new File(newDir, f.getName());
                if (dst.exists()) continue;
                try (InputStream in = new FileInputStream(f); OutputStream os = new FileOutputStream(dst)) {
                    copy(in, os);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "migration failed", e);
        }
    }

    /** يكتب السكن مباشرة فوق ملفات الخامة الأصلية داخل إصدار اللعبة، فيظهر دون تفعيل حزمة الموارد. */
    public static void applyDirect(File gameDir, String versionId) {
        try {
            File skin = selected(gameDir);
            if (skin == null || versionId == null) return;
            File vdir = new File(gameDir, "versions/" + versionId);
            File jar = new File(vdir, versionId + ".jar");
            if (!jar.exists()) return;
            byte[] png = readAll(skin);
            java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(new FileInputStream(jar));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zout = new java.util.zip.ZipOutputStream(bos);
            java.util.zip.ZipEntry e;
            java.util.Set<String> names = new java.util.HashSet<>();
            for (String n : DEFAULT_NAMES) names.add("assets/minecraft/textures/entity/" + n + ".png");
            while ((e = zin.getNextEntry()) != null) {
                if (names.contains(e.getName())) { zin.closeEntry(); continue; }
                zout.putNextEntry(new java.util.zip.ZipEntry(e.getName()));
                copy(zin, zout);
                zout.closeEntry();
                zin.closeEntry();
            }
            zin.close();
            for (String n : names) {
                zout.putNextEntry(new java.util.zip.ZipEntry(n));
                zout.write(png);
                zout.closeEntry();
            }
            zout.close();
            File tmp = new File(vdir, versionId + ".jar.tmp");
            try (FileOutputStream fo = new FileOutputStream(tmp)) { fo.write(bos.toByteArray()); }
            tmp.renameTo(jar);
            Log.i(TAG, "Skin written directly into " + versionId);
        } catch (Exception ex) {
            Log.e(TAG, "applyDirect failed", ex);
        }
    }

    public static void apply(File gameDir, String versionId) {
        applyDirect(gameDir, versionId);
        try {
            File packsDir = new File(gameDir, "resourcepacks");
            packsDir.mkdirs();
            File pack = new File(packsDir, packFile());
            File skin = selected(gameDir);
            if (skin == null) {
                pack.delete();
                setEnabled(gameDir, false);
                return;
            }
            buildPack(readAll(skin), pack, formatFor(versionId));
            setEnabled(gameDir, true);
            Log.i(TAG, "Skin applied for " + activeKey + ": " + skin.getName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply skin", e);
        }
    }

    public static int formatFor(String v) {
        if (v == null) return 15;
        String[] p = v.split("\\.");
        if (p.length < 2) return 15;
        int minor;
        int patch = 0;
        try {
            minor = Integer.parseInt(p[1].replaceAll("[^0-9].*", ""));
        } catch (Exception e) {
            return 15;
        }
        if (p.length > 2) {
            try { patch = Integer.parseInt(p[2].replaceAll("[^0-9].*", "")); } catch (Exception ignored) {}
        }
        if (minor >= 21) return patch >= 4 ? 46 : 34;
        if (minor == 20) {
            if (patch >= 5) return 32;
            if (patch >= 3) return 22;
            if (patch >= 2) return 18;
            return 15;
        }
        if (minor == 19) {
            if (patch >= 4) return 13;
            if (patch >= 3) return 12;
            return 9;
        }
        if (minor == 18) return 8;
        if (minor == 17) return 7;
        if (minor >= 15) return 6;
        if (minor >= 13) return 4;
        return 3;
    }

    private static void buildPack(byte[] skinBytes, File pack, int format) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(pack))) {
            zos.putNextEntry(new ZipEntry("pack.png"));
            zos.write(makePackIcon(skinBytes));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            String meta = "{\"pack\":{\"pack_format\":" + format + ",\"description\":\"CleanLauncher skin\"}}";
            zos.write(meta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String name : DEFAULT_NAMES) {
                zos.putNextEntry(new ZipEntry("assets/minecraft/textures/entity/" + name + ".png"));
                zos.write(skinBytes);
                zos.closeEntry();
                for (String model : new String[]{"wide", "slim"}) {
                    zos.putNextEntry(new ZipEntry("assets/minecraft/textures/entity/player/" + model + "/" + name + ".png"));
                    zos.write(skinBytes);
                    zos.closeEntry();
                }
            }
        }
    }

    /** يفعّل حزمة هذا البروفايل فقط، ويزيل حزم بقية البروفايلات من قائمة options.txt. */
    private static void setEnabled(File gameDir, boolean on) throws IOException {
        File opts = new File(gameDir, "options.txt");
        List<String> lines = opts.exists() ? readLines(opts) : new ArrayList<String>();
        String marker = "\"file/" + packFile() + "\"";
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("resourcePacks:")) continue;
            found = true;
            String body = line.substring("resourcePacks:".length()).trim();
            List<String> items = new ArrayList<>();
            if (body.startsWith("[") && body.endsWith("]")) {
                for (String it : body.substring(1, body.length() - 1).split(",")) {
                    if (!it.trim().isEmpty()) items.add(it.trim());
                }
            }
            items.removeIf(x -> x.startsWith("\"file/" + PACK_PREFIX));
            if (on) items.add(marker);
            lines.set(i, "resourcePacks:[" + join(items) + "]");
        }
        if (!found && on) lines.add("resourcePacks:[" + marker + "]");
        writeLines(opts, lines);
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }

    public static byte[] readAll(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(in, bos);
            return bos.toByteArray();
        }
    }

    private static String readText(File f) throws IOException {
        return new String(readAll(f), StandardCharsets.UTF_8);
    }

    private static void writeText(File f, String text) throws IOException {
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<String> readLines(File f) throws IOException {
        List<String> out = new ArrayList<>();
        for (String l : readText(f).split("\n")) {
            if (!l.isEmpty()) out.add(l);
        }
        return out;
    }

    private static void writeLines(File f, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        writeText(f, sb.toString());
    }

    private static byte[] makePackIcon(byte[] skinBytes) {
        try {
            android.graphics.Bitmap skin = android.graphics.BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.length);
            if (skin == null) return new byte[0];
            android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            c.drawColor(0xFFDCDCDC);
            android.graphics.Bitmap front = android.graphics.Bitmap.createBitmap(skin, 8, 8, 8, 8);
            android.graphics.Bitmap hat = android.graphics.Bitmap.createBitmap(skin, 40, 8, 8, 8);
            android.graphics.Bitmap body = android.graphics.Bitmap.createBitmap(skin, 20, 20, 8, 12);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(body, null, new android.graphics.RectF(44, 56, 84, 116), p);
            c.drawBitmap(front, null, new android.graphics.RectF(44, 10, 84, 50), p);
            c.drawBitmap(hat, null, new android.graphics.RectF(42, 8, 86, 52), p);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
