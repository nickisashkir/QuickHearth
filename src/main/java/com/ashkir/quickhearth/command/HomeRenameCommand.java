package com.ashkir.quickhearth.command;

import com.ashkir.quickhearth.Perms;
import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.data.Home;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.regex.Pattern;

public final class HomeRenameCommand {
    private HomeRenameCommand() {}

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_'\\- ]{1,32}$");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homerename")
            .requires(src -> Perms.check(src, "quickhearth.command.homerename", true))
            .then(Commands.argument("old", StringArgumentType.string())
                .suggests(HomeCommand.HOME_NAMES)
                .then(Commands.argument("new", StringArgumentType.greedyString())
                    .executes(HomeRenameCommand::run)))
        );
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 0;
        String oldName = StringArgumentType.getString(ctx, "old").trim();
        String newName = StringArgumentType.getString(ctx, "new").trim();

        if (newName.isEmpty() || !NAME_PATTERN.matcher(newName).matches()) {
            p.sendSystemMessage(Component.literal("\u00a7cInvalid new name. Use letters, numbers, spaces, apostrophes, _, or - (max 32 chars)."));
            return 0;
        }
        if (newName.equalsIgnoreCase("help")) {
            p.sendSystemMessage(Component.literal("\u00a7c'help' is reserved."));
            return 0;
        }
        if (oldName.equalsIgnoreCase(newName)) {
            p.sendSystemMessage(Component.literal("\u00a7cNew name is the same as the old name."));
            return 0;
        }

        Optional<Home> existing = QuickHearth.get().homes().get(p.getUUID(), oldName);
        if (existing.isEmpty()) {
            p.sendSystemMessage(Component.literal("\u00a7cYou don't have a home named '" + oldName + "'."));
            return 0;
        }
        if (QuickHearth.get().homes().get(p.getUUID(), newName).isPresent()) {
            p.sendSystemMessage(Component.literal("\u00a7cYou already have a home named '" + newName + "'."));
            return 0;
        }

        Home old = existing.get();
        Home renamed = new Home(newName, old.dimension(),
            old.x(), old.y(), old.z(), old.yaw(), old.pitch(), old.icon());
        QuickHearth.get().homes().set(p.getUUID(), renamed);
        QuickHearth.get().homes().delete(p.getUUID(), oldName);
        QuickHearth.get().shares().unshareAllForHome(p.getUUID(), oldName);
        p.sendSystemMessage(Component.literal("\u00a77Renamed home \u00a7f" + oldName + "\u00a77 to \u00a7f" + newName));
        return 1;
    }
}
