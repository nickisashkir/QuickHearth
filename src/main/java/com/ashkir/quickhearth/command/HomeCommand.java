package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.ModConfig;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.XpCostHelper;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.gui.HomesGui;
import com.ashkir.quickhearth.teleport.TeleportService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ashkir.quickhearth.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.Optional;

public final class HomeCommand {
    private HomeCommand() {}

    public static final SuggestionProvider<CommandSourceStack> HOME_NAMES = (ctx, builder) -> {
        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
            String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (String name : QuickHearth.get().homes().sortedNames(p.getUUID())) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) builder.suggest(name);
            }
            if ("help".startsWith(prefix)) builder.suggest("help");
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .requires(src -> Perms.check(src, "quickhearth.command.home", true))
            .executes(HomeCommand::openGui)
            .then(Commands.literal("help").executes(HomeCommand::showHelp))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(HOME_NAMES)
                .executes(HomeCommand::teleportToNamed))
        );
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        HomesGui.open(p);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.literal("\u00a76=== QuickHearth ==="));
        src.sendSystemMessage(Component.literal("\u00a7e/sethome [name] \u00a77- save your current spot (default 'home')"));
        src.sendSystemMessage(Component.literal("\u00a7e/home \u00a77- open the home picker"));
        src.sendSystemMessage(Component.literal("\u00a7e/home <name> \u00a77- teleport to a home"));
        src.sendSystemMessage(Component.literal("\u00a7e/homes \u00a77- same as /home"));
        src.sendSystemMessage(Component.literal("\u00a7e/delhome <name> \u00a77- delete a home"));
        src.sendSystemMessage(Component.literal("\u00a7e/spawn \u00a77- teleport to world spawn"));
        src.sendSystemMessage(Component.literal("\u00a7e/tpa <player> \u00a77- request to teleport to them"));
        src.sendSystemMessage(Component.literal("\u00a7e/tpahere <player> \u00a77- request them to teleport to you (alias /tpr)"));
        src.sendSystemMessage(Component.literal("\u00a7e/tpaccept \u00a77/ \u00a7e/tpdeny \u00a77- respond to a request"));
        src.sendSystemMessage(Component.literal("\u00a7e/tpatoggle \u00a77- block incoming teleport requests"));
        return 1;
    }

    private static int teleportToNamed(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String name = StringArgumentType.getString(ctx, "name").trim();
        Optional<Home> home = QuickHearth.get().homes().get(p.getUUID(), name);
        if (home.isEmpty()) {
            p.sendSystemMessage(Component.literal("\u00a7cNo home named '" + name + "'. Try \u00a7e/home help\u00a7c."));
            return 0;
        }
        return teleport(p, home.get()) ? 1 : 0;
    }

    public static boolean teleport(ServerPlayer p, Home home) {
        TeleportService ts = QuickHearth.get().teleport();
        if (ts.isOnCooldown(p)) {
            p.sendSystemMessage(Component.literal("\u00a7cYou must wait \u00a7e" + ts.cooldownRemaining(p) + "s\u00a7c before teleporting again."));
            return false;
        }
        MinecraftServer server = p.level().getServer();
        Identifier dimId = Identifier.parse(home.dimension());
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) {
            p.sendSystemMessage(Component.literal("\u00a7cThat home's dimension isn't loaded."));
            return false;
        }
        ModConfig.TeleportXpCost xpCfg = QuickHearth.get().configManager().get().teleportXpCost;
        int cost = XpCostHelper.calculate(p, level, home.x(), home.z(), xpCfg.homeBase, home.name());
        if (!XpCostHelper.tryDeductOrFail(p, cost)) return false;
        TeleportService.Destination dest = new TeleportService.Destination(
            level, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        return ts.queue(p, dest, "home '" + home.name() + "'", Config.HOME_COOLDOWN_TICKS);
    }
}
