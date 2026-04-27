package com.ashkir.quickhearth;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public final class Perms {
    private Perms() {}

    private static final Permission OP_LEVEL = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private static final boolean LP_LOADED;

    static {
        boolean lp;
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            lp = true;
        } catch (ClassNotFoundException e) {
            lp = false;
        }
        LP_LOADED = lp;
    }

    public static boolean check(CommandSourceStack src, String permission, boolean defaultAllow) {
        if (src.getEntity() instanceof ServerPlayer p) return check(p, permission, defaultAllow);
        return defaultAllow || src.permissions().hasPermission(OP_LEVEL);
    }

    public static boolean check(ServerPlayer player, String permission, boolean defaultAllow) {
        if (LP_LOADED) {
            try {
                Boolean lp = LuckPermsCheck.check(player.getUUID(), permission);
                if (lp != null) return lp;
            } catch (Throwable t) {
                QuickHearth.LOGGER.debug("LuckPerms perm check failed: {}", t.getMessage());
            }
        }
        return defaultAllow || player.permissions().hasPermission(OP_LEVEL);
    }
}
