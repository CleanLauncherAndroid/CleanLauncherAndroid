package net.kdt.pojavlaunch.fragments;
import net.kdt.pojavlaunch.SocialRow;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        SocialRow.bind(view, view.getContext());
        applySavedBackground(view);
        boolean kids = androidx.preference.PreferenceManager.getDefaultSharedPreferences(view.getContext()).getBoolean("kids_mode", false);
        View ytGroup = view.findViewById(R.id.btn_youtube).getParent() instanceof android.view.View ? (View) view.findViewById(R.id.btn_youtube).getParent() : null;
        View divider = view.findViewById(R.id.social_row) instanceof android.view.ViewGroup ? findKidsDivider((android.view.ViewGroup) view.findViewById(R.id.social_row)) : null;
        if (ytGroup != null) ytGroup.setVisibility(kids ? View.GONE : View.VISIBLE);
        if (divider != null) divider.setVisibility(kids ? View.GONE : View.VISIBLE);
        View modsGroup = view.findViewById(R.id.mods_button) != null ? (View) view.findViewById(R.id.mods_button).getParent() : null;
        if (modsGroup != null && kids) {
            android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) modsGroup.getLayoutParams();
            modsGroup.setLayoutParams(lp);
        }
        view.findViewById(R.id.mods_button).setOnClickListener(v -> startActivity(new Intent(requireContext(), net.kdt.pojavlaunch.ModsActivity.class)));
        view.findViewById(R.id.skins_button).setOnClickListener(v -> startActivity(new Intent(requireContext(), net.kdt.pojavlaunch.SkinManagerActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
        mInstallJarButton.setOnLongClickListener(v->{
            runInstallerWithConfirmation(true);
            return true;
        });
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> {
            Tools.switchDemo(Tools.isDemoProfile(v.getContext())); // avoid switching accounts being able to access
            if(Tools.isDemoProfile(v.getContext())){
                Toast.makeText(v.getContext(), R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
                return;
            }

            openPath(v.getContext(), getCurrentProfileDirectory(), false);
        });
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if(!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if(profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        // avoid using custom installers to install a version
        if(Tools.isLocalProfile(requireContext()) || Tools.isDemoProfile(requireContext())){
            Toast.makeText(requireContext(), R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
            return;
        }

        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    private static View findKidsDivider(android.view.ViewGroup row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View c = row.getChildAt(i);
            if (c.getClass() == View.class) return c;
        }
        return null;
    }

    private void applySavedBackground(View root) {
        try {
            String name = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(net.kdt.pojavlaunch.BackgroundPickerActivity.KEY, null);
            if (name == null || name.equals("none")) { root.setBackgroundColor(0xFF181818); return; }
            java.io.InputStream in = name.startsWith("file:")
                ? new java.io.FileInputStream(name.substring(5))
                : requireContext().getAssets().open("backgrounds/" + name);
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            root.setBackground(new android.graphics.drawable.BitmapDrawable(getResources(), bm));
        } catch (Exception ignored) {
        }
    }
}
