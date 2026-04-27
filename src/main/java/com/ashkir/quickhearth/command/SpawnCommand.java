package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.teleport.TeleportService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;

public final class SpawnCommand {
    private SpawnCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
            .requires(src -> Perms.check(src, "quickhearth.command.spawn", true))
            .executes(SpawnCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        ServerLevel overworld = p.level().getServer().overworld();
        LevelData.RespawnData respawn = overworld.getRespawnData();
        if (respawn == null) respawn = LevelData.RespawnData.DEFAULT;
        BlockPos spawn = respawn.pos();
        float yaw = respawn.yaw();
        float pitch = respawn.pitch();
        TeleportService ts = QuickHearth.get().teleport();
        if (ts.isOnCooldown(p)) {
            p.sendSystemMessage(Component.literal("\u00a7cYou must wait \u00a7e" + ts.cooldownRemaining(p) + "s\u00a7c before teleporting again."));
            return 0;
        }
        TeleportService.Destination dest = new TeleportService.Destination(
            overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, yaw, pitch);
        return ts.queue(p, dest, "spawn", Config.SPAWN_COOLDOWN_TICKS) ? 1 : 0;
    }
}
