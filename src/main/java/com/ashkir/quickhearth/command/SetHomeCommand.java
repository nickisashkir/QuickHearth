package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.ModConfig;
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

    private static final java.util.regex.Pattern NAME_PATTERN =
        java.util.regex.Pattern.compile("^[A-Za-z0-9_'\\- ]{1,32}$");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
            .requires(src -> Perms.check(src, "quickhearth.command.sethome", true))
            .executes(ctx -> setHome(ctx, "home"))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "name"))))
        );
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx, String rawName) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String name = rawName.trim();
        if (name.isEmpty() || !NAME_PATTERN.matcher(name).matches()) {
            p.sendSystemMessage(Component.literal("\u00a7cInvalid name. Use letters, numbers, spaces, apostrophes, underscores, or hyphens (max 32 chars)."));
            return 0;
        }
        if (name.equalsIgnoreCase("help")) {
            p.sendSystemMessage(Component.literal("\u00a7c'help' is reserved. Pick a different name."));
            return 0;
        }

        ModConfig cfg = QuickHearth.get().configManager().get();
        String currentDim = p.level().dimension().identifier().toString();
        if (cfg.blockedHomeDimensions != null && cfg.blockedHomeDimensions.contains(currentDim)) {
            p.sendSystemMessage(Component.literal("\u00a7cHomes cannot be set in this dimension."));
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
            Integer dimCap = cfg.perDimensionPlayerCap == null ? null : cfg.perDimensionPlayerCap.get(currentDim);
            if (dimCap != null) {
                long currentInDim = storage.homes(p.getUUID()).values().stream()
                    .filter(h -> currentDim.equals(h.dimension()))
                    .count();
                if (currentInDim >= dimCap) {
                    p.sendSystemMessage(Component.literal("\u00a7cYou've reached the per-dimension home cap for this dimension (" + dimCap + ")."));
                    return 0;
                }
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
            currentDim,
            p.getX(), p.getY(), p.getZ(),
            p.getYRot(), p.getXRot(),
            iconJson);
        storage.set(p.getUUID(), home);

        String prefix = (overwrite ? "\u00a7eUpdated" : "\u00a7aSet") + " home \u00a7f" + name;
        Component msg = (iconLabel != null)
            ? Component.literal(prefix + "\u00a77 with icon: ").append(iconLabel)
            : Component.literal(prefix);
        p.sendSystemMessage(msg);
        return 1;
    }
}
