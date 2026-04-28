package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.teleport.ShareManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ShareDenyCommand {
    private ShareDenyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sharedeny")
            .requires(src -> Perms.check(src, "quickhearth.command.sharedeny", true))
            .executes(ShareDenyCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer self)) return 0;
        Optional<ShareManager.Pending> opt = QuickHearth.get().sharePending().consume(self.getUUID());
        if (opt.isEmpty()) {
            self.sendSystemMessage(Component.literal("\u00a7cNo pending share request."));
            return 0;
        }
        ShareManager.Pending p = opt.get();
        ServerPlayer owner = self.level().getServer().getPlayerList().getPlayer(p.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("\u00a7e" + self.getGameProfile().name()
                + " \u00a7cdeclined your home share."));
        }
        self.sendSystemMessage(Component.literal("\u00a77Share request declined."));
        return 1;
    }
}
