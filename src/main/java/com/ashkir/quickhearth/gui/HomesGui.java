package com.ashkir.quickhearth.gui;

import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.command.HomeCommand;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.data.HomeStorage;
import com.ashkir.quickhearth.data.ItemSerde;
import com.ashkir.quickhearth.data.SharedHome;
import com.ashkir.quickhearth.limits.HomeLimitProvider;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public final class HomesGui {
    private HomesGui() {}

    private static final int OWNED_SLOTS = 18;
    private static final int SHARED_SLOTS = 9;
    private static final int SHARED_START = 18;

    public static void open(ServerPlayer player) {
        HomeStorage storage = QuickHearth.get().homes();
        HomeLimitProvider limits = QuickHearth.get().limits();
        List<String> names = storage.sortedNames(player.getUUID());
        List<SharedHome> shared = QuickHearth.get().shares().sharesFor(player.getUUID());
        int max = limits.max(player);
        int used = names.size();

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.literal("Homes (" + used + "/" + max + ")"));

        if (names.isEmpty() && shared.isEmpty()) {
            gui.setSlot(13, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("\u00a7eNo homes yet"))
                .addLoreLine(Component.literal("\u00a77Use \u00a7f/sethome <name>\u00a77 to save your spot."))
                .addLoreLine(Component.literal("\u00a77Hold any item first to set a custom icon."))
                .build());
            gui.open();
            return;
        }

        int slot = 0;
        for (String name : names) {
            if (slot >= OWNED_SLOTS) break;
            Home home = storage.get(player.getUUID(), name).orElse(null);
            if (home == null) continue;
            final Home homeRef = home;
            gui.setSlot(slot, builderForHome(homeRef, player)
                .setCallback((index, type, input, slotGui) -> {
                    gui.close();
                    if (type.shift) {
                        storage.delete(player.getUUID(), homeRef.name());
                        QuickHearth.get().shares().unshareAllForHome(player.getUUID(), homeRef.name());
                        player.sendSystemMessage(Component.literal("\u00a77Removed home \u00a7f" + homeRef.name()));
                    } else {
                        HomeCommand.teleport(player, homeRef);
                    }
                })
                .build());
            slot++;
        }

        int sharedShown = 0;
        boolean overflow = shared.size() > SHARED_SLOTS;
        int displayLimit = overflow ? SHARED_SLOTS - 1 : SHARED_SLOTS;
        for (SharedHome share : shared) {
            if (sharedShown >= displayLimit) break;
            Optional<Home> ownerHome = QuickHearth.get().homes().get(share.owner(), share.homeName());
            if (ownerHome.isEmpty()) continue;
            final Home homeRef = ownerHome.get();
            final SharedHome shareRef = share;
            gui.setSlot(SHARED_START + sharedShown, SharedHomesGui.sharedBuilder(homeRef, shareRef, player)
                .setCallback((index, type, input, slotGui) -> {
                    gui.close();
                    if (type.shift) {
                        QuickHearth.get().shares().unshare(shareRef.owner(), shareRef.homeName(), player.getUUID());
                        player.sendSystemMessage(Component.literal("\u00a77Removed shared home \u00a7f"
                            + shareRef.homeName() + "\u00a77 from \u00a7e" + shareRef.ownerName()));
                    } else {
                        HomeCommand.teleport(player, homeRef);
                    }
                })
                .build());
            sharedShown++;
        }

        if (overflow) {
            gui.setSlot(SHARED_START + SHARED_SLOTS - 1, new GuiElementBuilder(Items.WRITABLE_BOOK)
                .setName(Component.literal("\u00a7eMore shared homes..."))
                .addLoreLine(Component.literal("\u00a77You have " + shared.size() + " shared homes."))
                .addLoreLine(Component.literal("\u00a77Click to open the full list."))
                .setCallback((index, type, input, slotGui) -> {
                    gui.close();
                    SharedHomesGui.open(player);
                })
                .build());
        }

        gui.open();
    }

    private static GuiElementBuilder builderForHome(Home home, ServerPlayer player) {
        GuiElementBuilder builder;
        Optional<ItemStack> custom = ItemSerde.deserialize(home.icon(), player.level().getServer().registryAccess());
        if (custom.isPresent() && !custom.get().isEmpty()) {
            builder = GuiElementBuilder.from(custom.get());
            builder.setName(Component.literal("\u00a7f" + home.name()));
        } else {
            builder = new GuiElementBuilder(Items.WHITE_BANNER)
                .setName(Component.literal("\u00a7f" + home.name()));
        }
        return builder
            .addLoreLine(Component.literal("\u00a77" + home.dimension()))
            .addLoreLine(Component.literal("\u00a78" + (int) home.x() + ", " + (int) home.y() + ", " + (int) home.z()))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("\u00a7eClick to teleport"))
            .addLoreLine(Component.literal("\u00a7cShift-click to delete"));
    }
}
