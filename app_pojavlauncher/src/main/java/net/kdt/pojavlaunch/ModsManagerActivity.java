package net.kdt.pojavlaunch;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;

public class ModsManagerActivity extends Activity {
    private static final String[] TYPE_LABELS = {"Mods", "Resource packs", "Shaders", "Data packs"};
    private static final String[] TYPE_DIRS = {"mods", "resourcepacks", "shaderpacks", "datapacks"};
    private static final String[] TYPE_EXT = {".jar", ".zip", ".zip", ".zip"};

    private final List<File> items = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Spinner typeSp;
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
        title.setText("Installed");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        typeSp = new Spinner(this);
        typeSp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TYPE_LABELS));
        typeSp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p2, android.view.View v, int pos, long id) { reload(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p2) {}
        });
        root.addView(typeSp);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> confirmDelete(items.get(pos)));
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        reload();
    }

    private void reload() {
        items.clear();
        labels.clear();
        int idx = typeSp.getSelectedItemPosition();
        String dir = TYPE_DIRS[idx];
        String ext = TYPE_EXT[idx];
        File versionsDir = new File(gameDir, "versions");
        File[] versions = versionsDir.listFiles(File::isDirectory);
        if (versions != null) {
            for (File v : versions) {
                File[] files = new File(v, dir).listFiles((d, n) -> n.endsWith(ext));
                if (files == null) continue;
                for (File f : files) {
                    items.add(f);
                    labels.add(f.getName() + "  \u00b7  " + v.getName());
                }
            }
        }
        if (items.isEmpty()) labels.add("No " + TYPE_LABELS[idx].toLowerCase() + " installed");
        adapter.notifyDataSetChanged();
    }

    private void confirmDelete(final File f) {
        new AlertDialog.Builder(this)
            .setTitle("Delete " + f.getName() + "?")
            .setPositiveButton("Delete", (d, w) -> { f.delete(); reload(); })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
