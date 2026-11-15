package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

/** يطبّق اللغة المختارة من الإعدادات، والافتراضي هو لغة الجهاز. */
public final class LocaleHelper {
    public static final String KEY = "app_language";
    public static String applied = null;

    public static String chosen(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c).getString(KEY, "system");
    }

    private LocaleHelper() {}

    public static Context wrap(Context base) {
        String lang = PreferenceManager.getDefaultSharedPreferences(base).getString(KEY, "system");
        applied = lang;
        if (lang == null || "system".equals(lang)) return base;
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }
}
