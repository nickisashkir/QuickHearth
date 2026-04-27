package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.data.HomeStorage;
import com.ashkir.quickhearth.data.ItemSerde;
import com.ashkir.quickhearth.limits.HomeLimitProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SetHomeCommand {
    private SetHomeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
            .requires(src -> Perms.check(src, "quickhearth.command.sethome", true))
            .executes(ctx -> setHome(ctx, "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name"))))
        );
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx, String rawName) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String name = rawName.trim();
        if (name.isEmpty() || !name.matches("[A-Za-z0-9_-]{1,24}")) {
            p.sendSystemMessage(Component.literal("\u00a7cInvalid name. Use letters, numbers, _, or - (max 24 chars)."));
            return 0;
        }
        if (name.equalsIgnoreCase("help")) {
            p.sendSystemMessage(Component.literal("\u00a7c'help' is reserved. Pick a different name."));
            return 0;
        }
        HomeStorage storage = QuickHearth.get().homes();
        HomeLimitProvider limits = QuickHearth.get().limits();
        boolean overwrite = storage.get(p.getUUID(), name).isPresent();
        if (!overwrite) {
            int max = limits.max(p);
            int current = storage.homes(p.getUUID()).size();
            if (current >= max) {
                p.sendSystemMessage(Component.literal("\u00a7cYou've reached your home limit (" + max + "). Delete one first."));
                return 0;
            }
        }

        ItemStack mainHand = p.getMainHandItem();
        String iconJson = null;
        Component iconLabel = null;
        if (!mainHand.isEmpty()) {
            iconJson = ItemSerde.serialize(mainHand, p.level().getServer().registryAccess());
            iconLabel = mainHand.getHoverName();
        }

        Home home = new Home(name,
            p.level().dimension().identifier().toString(),
            p.getX(), p.getY(), p.getZ(),
            p.getYRot(), p.getXRot(),
            iconJson);
        storage.set(p.getUUID(), home);

        Component msg;
        String prefix = (overwrite ? "\u00a7eUpdated" : "\u00a7aSet") + " home \u00a7f" + name;
        if (iconLabel != null) {
            msg = Component.literal(prefix + "\u00a77 with icon: ").append(iconLabel);
        } else {
            msg = Component.literal(prefix);
        }
        p.sendSystemMessage(msg);
        return 1;
    }
}
