package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.teleport.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class TpDenyCommand {
    private TpDenyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpdeny")
            .requires(src -> Perms.check(src, "quickhearth.command.tpdeny", true))
            .executes(TpDenyCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) return 0;
        Optional<TpaManager.Request> opt = QuickHearth.get().tpa().consume(self.getUUID());
        if (opt.isEmpty()) {
            self.sendSystemMessage(Component.literal("\u00a7cNo pending teleport request."));
            return 0;
        }
        ServerPlayer requester = self.level().getServer().getPlayerList().getPlayer(opt.get().requester());
        if (requester != null) {
            requester.sendSystemMessage(Component.literal("\u00a7e" + self.getGameProfile().name()
                + " \u00a7cdenied your request."));
        }
        self.sendSystemMessage(Component.literal("\u00a77Request denied."));
        return 1;
    }
}
