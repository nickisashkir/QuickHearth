package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.teleport.TpaManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TpaCommand {
    private TpaCommand() {}

    public static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, b) -> {
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            b.suggest(p.getGameProfile().name());
        }
        return b.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
            .requires(src -> Perms.check(src, "quickhearth.command.tpa", true))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(ONLINE_PLAYERS)
                .executes(ctx -> run(ctx, TpaManager.Direction.TO_TARGET, "tpa")))
        );
    }

    public static int run(CommandContext<CommandSourceStack> ctx, TpaManager.Direction dir, String label) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer requester)) return 0;
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            requester.sendSystemMessage(Component.literal("\u00a7cPlayer \u00a7e" + targetName + "\u00a7c isn't online."));
            return 0;
        }
        if (target.getUUID().equals(requester.getUUID())) {
            requester.sendSystemMessage(Component.literal("\u00a7cYou can't request a teleport to yourself."));
            return 0;
        }
        if (QuickHearth.get().toggles().isTpaDisabled(target.getUUID())) {
            requester.sendSystemMessage(Component.literal("\u00a7e" + target.getGameProfile().name() + " \u00a7cisn't accepting teleport requests."));
            return 0;
        }
        if (!QuickHearth.get().tpa().send(requester, target, dir)) return 0;
        requester.sendSystemMessage(Component.literal("\u00a77Sent " + label + " request to \u00a7f"
            + target.getGameProfile().name() + "\u00a77. Expires in 60s."));
        if (dir == TpaManager.Direction.TO_TARGET) {
            target.sendSystemMessage(Component.literal("\u00a7e" + requester.getGameProfile().name()
                + " \u00a77wants to teleport \u00a7fto you\u00a77. \u00a7a/tpaccept \u00a77or \u00a7c/tpdeny\u00a77."));
        } else {
            target.sendSystemMessage(Component.literal("\u00a7e" + requester.getGameProfile().name()
                + " \u00a77wants you to teleport \u00a7fto them\u00a77. \u00a7a/tpaccept \u00a77or \u00a7c/tpdeny\u00a77."));
        }
        return 1;
    }
}
