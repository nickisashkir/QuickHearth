package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.QuickHearth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TpaToggleCommand {
    private TpaToggleCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpatoggle")
            .requires(src -> Perms.check(src, "quickhearth.command.tpatoggle", true))
            .executes(TpaToggleCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        boolean nowDisabled = QuickHearth.get().toggles().toggleTpa(p.getUUID());
        p.sendSystemMessage(Component.literal(nowDisabled
            ? "\u00a77Teleport requests will be \u00a7cblocked\u00a77."
            : "\u00a77Teleport requests will be \u00a7aaccepted\u00a77."));
        return 1;
    }
}
