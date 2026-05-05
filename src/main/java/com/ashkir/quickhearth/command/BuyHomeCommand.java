package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.ModConfig;
import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public final class BuyHomeCommand {
    private BuyHomeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buyhome")
            .requires(src -> Perms.check(src, "quickhearth.command.buyhome", true))
            .executes(BuyHomeCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        ModConfig.BuyHome cfg = QuickHearth.get().configManager().get().buyHome;
        if (!cfg.enabled) {
            p.sendSystemMessage(Component.literal("\u00a7cBuying home slots is disabled on this server."));
            return 0;
        }

        int currentBonus = QuickHearth.get().limits().bonus(p);
        if (cfg.maxBonus > 0 && currentBonus >= cfg.maxBonus) {
            p.sendSystemMessage(Component.literal("\u00a7cYou've reached the maximum of \u00a7e"
                + cfg.maxBonus + "\u00a7c bought slots."));
            return 0;
        }

        int cost = (int) Math.floor(cfg.baseCost * Math.pow(cfg.costMultiplier, currentBonus));
        if (cost < 0) cost = 0;

        if (p.experienceLevel < cost) {
            p.sendSystemMessage(Component.literal("\u00a7cYou need \u00a7e" + cost
                + "\u00a7c levels to buy a slot. You have \u00a7e" + p.experienceLevel + "\u00a7c."));
            return 0;
        }

        Scoreboard sb = p.level().getServer().getScoreboard();
        Objective obj = sb.getObjective(Config.BONUS_OBJECTIVE_NAME);
        if (obj == null) {
            p.sendSystemMessage(Component.literal("\u00a7cBonus scoreboard objective missing. Contact an admin."));
            return 0;
        }

        p.giveExperienceLevels(-cost);
        sb.getOrCreatePlayerScore(p, obj).add(1);

        int newMax = QuickHearth.get().limits().max(p);
        p.sendSystemMessage(Component.literal("\u00a7aBought a home slot for \u00a7e" + cost
            + "\u00a7a levels. You now have \u00a7e" + newMax + "\u00a7a total slots."));
        return 1;
    }
}
