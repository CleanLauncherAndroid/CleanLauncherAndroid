package net.kdt.pojavlaunch.value.launcherprofiles;
import androidx.annotation.Keep;

import java.util.*;
import net.kdt.pojavlaunch.*;

@Keep
public class MinecraftLauncherProfiles {
	public Map<String, MinecraftProfile> profiles = new HashMap<>();
	public boolean profilesWereMigrated;
	public String clientToken;
	public Map<String, MinecraftAuthenticationDatabase> authenticationDatabase;
	// public Map launcherVersion;
	public MinecraftLauncherSettings settings;
	// public Map analyticsToken;
	public int analyticsFailcount;
	public MinecraftSelectedUser selectedUser;
    
    public String toJson() {
        return Tools.GLOBAL_GSON.toJson(this);
    }

    public String currentKey() {
        try {
            MinecraftProfile cur = net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.getCurrentProfile();
            if (cur == null || profiles == null) return "default";
            for (Map.Entry<String, MinecraftProfile> e : profiles.entrySet()) {
                MinecraftProfile v = e.getValue();
                if (v == cur || (v != null && java.util.Objects.equals(v.name, cur.name) && java.util.Objects.equals(v.lastVersionId, cur.lastVersionId))) {
                    return e.getKey();
                }
            }
        } catch (Exception ignored) {
        }
        return "default";
    }
}
