package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.gui.SharedHomesGui;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class SharedHomesCommand {
    private SharedHomesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sharedhomes")
            .requires(src -> Perms.check(src, "quickhearth.command.sharedhomes", true))
            .executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                SharedHomesGui.open(p);
                return 1;
            })
        );
    }
}
