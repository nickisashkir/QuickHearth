package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.data.SharedHome;
import com.ashkir.quickhearth.teleport.ShareManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ShareAcceptCommand {
    private ShareAcceptCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shareaccept")
            .requires(src -> Perms.check(src, "quickhearth.command.shareaccept", true))
            .executes(ShareAcceptCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) return 0;
        Optional<ShareManager.Pending> opt = QuickHearth.get().sharePending().consume(self.getUUID());
        if (opt.isEmpty()) {
            self.sendSystemMessage(Component.literal("\u00a7cNo pending share request."));
            return 0;
        }
        ShareManager.Pending p = opt.get();
        SharedHome share = new SharedHome(p.owner(), p.ownerName(), p.homeName(),
            self.getUUID(), System.currentTimeMillis());
        QuickHearth.get().shares().share(share);

        self.sendSystemMessage(Component.literal("\u00a7aAdded \u00a7f" + p.homeName()
            + "\u00a7a from \u00a7e" + p.ownerName() + "\u00a7a to your shared homes."));
        ServerPlayer owner = self.level().getServer().getPlayerList().getPlayer(p.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("\u00a7e" + self.getGameProfile().name()
                + " \u00a7aaccepted your home share."));
        }
        return 1;
    }
}
