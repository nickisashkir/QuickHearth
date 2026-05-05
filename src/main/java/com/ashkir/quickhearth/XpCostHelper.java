package com.ashkir.quickhearth;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class XpCostHelper {
    private XpCostHelper() {}

    public static int calculate(ServerPlayer mover, ServerLevel destLevel,
                                double destX, double destZ,
                                int baseCost, String homeNameOrNull) {
        ModConfig.TeleportXpCost cfg = QuickHearth.get().configManager().get().teleportXpCost;
        if (!cfg.enabled) return 0;
        if (Perms.check(mover, "quickhearth.bypass.cost", false)) return 0;

        int cost;
        boolean crossDim = !mover.level().dimension().equals(destLevel.dimension());
        if (crossDim) {
            cost = baseCost + cfg.crossDimensionFlat;
        } else if (cfg.blocksPerLevel > 0) {
            double dx = mover.getX() - destX;
            double dz = mover.getZ() - destZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            cost = baseCost + (int) Math.floor(dist / cfg.blocksPerLevel);
        } else {
            cost = baseCost;
        }

        if (homeNameOrNull != null && cfg.primaryHomeName != null
            && homeNameOrNull.equalsIgnoreCase(cfg.primaryHomeName)) {
            cost = (int) Math.floor(cost * cfg.primaryHomeMultiplier);
        }

        if (cost < 0) cost = 0;
        if (cfg.maxLevels > 0 && cost > cfg.maxLevels) cost = cfg.maxLevels;
        return cost;
    }

    public static boolean tryDeductOrFail(ServerPlayer player, int cost) {
        if (cost <= 0) return true;
        if (player.experienceLevel < cost) {
            player.sendSystemMessage(Component.literal("\u00a7cYou need \u00a7e" + cost
                + "\u00a7c levels to teleport. You have \u00a7e" + player.experienceLevel + "\u00a7c."));
            return false;
        }
        player.giveExperienceLevels(-cost);
        if (cost > 0) {
            player.sendSystemMessage(Component.literal("\u00a77Charged \u00a7e" + cost + "\u00a77 levels."));
        }
        return true;
    }
}
