package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class QuickHearthCommand {
    private QuickHearthCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quickhearth")
            .requires(src -> Perms.check(src, "quickhearth.command.admin", false))
            .then(Commands.literal("reload").executes(QuickHearthCommand::reload))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        QuickHearth.get().configManager().reload();
        ctx.getSource().sendSystemMessage(Component.literal("\u00a7aQuickHearth config reloaded."));
        return 1;
    }
}
