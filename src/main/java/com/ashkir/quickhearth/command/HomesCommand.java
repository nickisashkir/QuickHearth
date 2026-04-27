package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.gui.HomesGui;
import com.mojang.brigadier.CommandDispatcher;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class HomesCommand {
    private HomesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homes")
            .requires(src -> Perms.check(src, "quickhearth.command.homes", true))
            .executes(ctx -> {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
                HomesGui.open(p);
                return 1;
            })
        );
    }
}
