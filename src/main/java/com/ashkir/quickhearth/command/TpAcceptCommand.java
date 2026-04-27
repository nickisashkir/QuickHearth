package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.teleport.TeleportService;
import com.ashkir.quickhearth.teleport.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class TpAcceptCommand {
    private TpAcceptCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpaccept")
            .requires(src -> Perms.check(src, "quickhearth.command.tpaccept", true))
            .executes(TpAcceptCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) return 0;
        Optional<TpaManager.Request> opt = QuickHearth.get().tpa().consume(self.getUUID());
        if (opt.isEmpty()) {
            self.sendSystemMessage(Component.literal("\u00a7cNo pending teleport request."));
            return 0;
        }
        TpaManager.Request r = opt.get();
        ServerPlayer requester = self.level().getServer().getPlayerList().getPlayer(r.requester());
        if (requester == null) {
            self.sendSystemMessage(Component.literal("\u00a7cThe requester is no longer online."));
            return 0;
        }
        ServerPlayer mover = (r.direction() == TpaManager.Direction.TO_TARGET) ? requester : self;
        ServerPlayer destinationPlayer = (mover == requester) ? self : requester;
        TeleportService.Destination dest = new TeleportService.Destination(
            destinationPlayer.level(),
            destinationPlayer.getX(), destinationPlayer.getY(), destinationPlayer.getZ(),
            destinationPlayer.getYRot(), destinationPlayer.getXRot());
        QuickHearth.get().teleport().queue(mover, dest,
            "request to " + destinationPlayer.getGameProfile().name(),
            Config.HOME_COOLDOWN_TICKS);
        self.sendSystemMessage(Component.literal("\u00a7aAccepted teleport request."));
        if (mover != self) {
            requester.sendSystemMessage(Component.literal("\u00a7e" + self.getGameProfile().name()
                + " \u00a7aaccepted your request."));
        } else {
            requester.sendSystemMessage(Component.literal("\u00a7e" + self.getGameProfile().name()
                + " \u00a7aaccepted. They are coming to you."));
        }
        return 1;
    }
}
