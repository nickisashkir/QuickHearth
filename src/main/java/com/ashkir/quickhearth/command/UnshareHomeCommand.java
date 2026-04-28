package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class UnshareHomeCommand {
    private UnshareHomeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unsharehome")
            .requires(src -> Perms.check(src, "quickhearth.command.unsharehome", true))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(TpaCommand.ONLINE_PLAYERS)
                .then(Commands.argument("home", StringArgumentType.greedyString())
                    .suggests(HomeCommand.HOME_NAMES)
                    .executes(UnshareHomeCommand::run)))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer owner)) return 0;
        String targetName = StringArgumentType.getString(ctx, "player");
        String homeName = StringArgumentType.getString(ctx, "home").trim();

        ServerPlayer recipient = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (recipient == null) {
            owner.sendSystemMessage(Component.literal("\u00a7c" + targetName + " must be online to unshare. (For offline unshare, edit shares.json directly.)"));
            return 0;
        }
        boolean removed = QuickHearth.get().shares().unshare(owner.getUUID(), homeName, recipient.getUUID());
        owner.sendSystemMessage(Component.literal(removed
            ? "\u00a77Stopped sharing \u00a7f" + homeName + "\u00a77 with \u00a7f" + targetName
            : "\u00a7cNo such active share."));
        return removed ? 1 : 0;
    }
}
