package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.data.ItemSerde;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class HomeIconCommand {
    private HomeIconCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homeicon")
            .requires(src -> Perms.check(src, "quickhearth.command.homeicon", true))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(HomeCommand.HOME_NAMES)
                .executes(HomeIconCommand::run))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String name = StringArgumentType.getString(ctx, "name").trim();
        Optional<Home> existing = QuickHearth.get().homes().get(p.getUUID(), name);
        if (existing.isEmpty()) {
            p.sendSystemMessage(Component.literal("\u00a7cYou don't have a home named '" + name + "'."));
            return 0;
        }
        ItemStack mainHand = p.getMainHandItem();
        String iconJson = null;
        Component iconLabel = null;
        if (!mainHand.isEmpty()) {
            iconJson = ItemSerde.serialize(mainHand, p.level().getServer().registryAccess());
            iconLabel = mainHand.getHoverName();
        }
        Home old = existing.get();
        Home updated = new Home(old.name(), old.dimension(),
            old.x(), old.y(), old.z(), old.yaw(), old.pitch(), iconJson);
        QuickHearth.get().homes().set(p.getUUID(), updated);
        Component msg = (iconLabel != null)
            ? Component.literal("\u00a7aIcon for \u00a7f" + old.name() + "\u00a7a updated to: ").append(iconLabel)
            : Component.literal("\u00a7aIcon for \u00a7f" + old.name() + "\u00a7a cleared (using default banner).");
        p.sendSystemMessage(msg);
        return 1;
    }
}
