package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.teleport.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class TpaHereCommand {
    private TpaHereCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("tpahere")
            .requires(src -> Perms.check(src, "quickhearth.command.tpahere", true))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(TpaCommand.ONLINE_PLAYERS)
                .executes(ctx -> TpaCommand.run(ctx, TpaManager.Direction.FROM_TARGET, "tpahere")))
        );
        dispatcher.register(Commands.literal("tpr")
            .requires(src -> Perms.check(src, "quickhearth.command.tpahere", true))
            .redirect(root));
    }
}
