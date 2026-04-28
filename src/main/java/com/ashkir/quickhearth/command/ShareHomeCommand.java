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

public final class ShareHomeCommand {
    private ShareHomeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sharehome")
            .requires(src -> Perms.check(src, "quickhearth.command.sharehome", true))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(TpaCommand.ONLINE_PLAYERS)
                .then(Commands.argument("home", StringArgumentType.greedyString())
                    .suggests(HomeCommand.HOME_NAMES)
                    .executes(ShareHomeCommand::run)))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer owner)) return 0;
        String targetName = StringArgumentType.getString(ctx, "player");
        String homeName = StringArgumentType.getString(ctx, "home").trim();

        ServerPlayer recipient = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (recipient == null) {
            owner.sendSystemMessage(Component.literal("\u00a7cPlayer \u00a7e" + targetName + "\u00a7c isn't online."));
            return 0;
        }
        if (recipient.getUUID().equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.literal("\u00a7cYou can't share a home with yourself."));
            return 0;
        }
        var home = QuickHearth.get().homes().get(owner.getUUID(), homeName);
        if (home.isEmpty()) {
            owner.sendSystemMessage(Component.literal("\u00a7cYou don't have a home named '" + homeName + "'."));
            return 0;
        }

        if (!QuickHearth.get().sharePending().propose(owner, recipient, home.get().name())) {
            owner.sendSystemMessage(Component.literal("\u00a7c" + recipient.getGameProfile().name() + " already has a pending share request."));
            return 0;
        }

        owner.sendSystemMessage(Component.literal("\u00a77Share request sent to \u00a7f"
            + recipient.getGameProfile().name() + "\u00a77 for home \u00a7f" + home.get().name() + "\u00a77. Expires in 60s."));
        recipient.sendSystemMessage(Component.literal("\u00a7e" + owner.getGameProfile().name()
            + " \u00a77wants to share their home \u00a7f" + home.get().name()
            + "\u00a77 with you. \u00a7a/shareaccept \u00a77or \u00a7c/sharedeny\u00a77."));
        return 1;
    }
}
