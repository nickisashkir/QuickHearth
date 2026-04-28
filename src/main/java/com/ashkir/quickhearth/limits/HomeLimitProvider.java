package com.ashkir.quickhearth.limits;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

public class HomeLimitProvider {
    private final boolean luckPermsLoaded;

    public HomeLimitProvider() {
        boolean lp;
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            lp = true;
        } catch (ClassNotFoundException e) {
            lp = false;
        }
        this.luckPermsLoaded = lp;
        QuickHearth.LOGGER.info("LuckPerms detected: {} (default home limit when absent: {})",
            luckPermsLoaded, Config.DEFAULT_MAX_HOMES);
    }

    public int rankBase(ServerPlayer player) {
        if (luckPermsLoaded) {
            try {
                return LuckPermsBridge.lookupRankBase(player.getUUID());
            } catch (Throwable t) {
                QuickHearth.LOGGER.debug("LuckPerms lookup failed for {}: {}", player.getGameProfile().name(), t.getMessage());
            }
        }
        return Config.DEFAULT_MAX_HOMES;
    }

    public int bonus(ServerPlayer player) {
        Scoreboard sb = player.level().getServer().getScoreboard();
        Objective obj = sb.getObjective(Config.BONUS_OBJECTIVE_NAME);
        if (obj == null) return 0;
        ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(player, obj);
        if (info == null) return 0;
        return info.value();
    }

    public int max(ServerPlayer player) {
        return Math.max(0, rankBase(player) + bonus(player));
    }
}
