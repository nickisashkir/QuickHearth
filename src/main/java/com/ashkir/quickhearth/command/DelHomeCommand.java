package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.QuickHearth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DelHomeCommand {
    private DelHomeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("delhome")
            .requires(src -> Perms.check(src, "quickhearth.command.delhome", true))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(HomeCommand.HOME_NAMES)
                .executes(DelHomeCommand::run))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String name = StringArgumentType.getString(ctx, "name").trim();
        if (QuickHearth.get().homes().delete(p.getUUID(), name)) {
            p.sendSystemMessage(Component.literal("\u00a77Removed home \u00a7f" + name));
            return 1;
        }
        p.sendSystemMessage(Component.literal("\u00a7cNo home named '" + name + "'."));
        return 0;
    }
}
