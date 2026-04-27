package com.ashkir.quickhearth.gui;

import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.command.HomeCommand;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.data.HomeStorage;
import com.ashkir.quickhearth.limits.HomeLimitProvider;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;

public final class HomesGui {
    private HomesGui() {}

    private static final int CAPACITY = 27;

    public static void open(ServerPlayer player) {
        HomeStorage storage = QuickHearth.get().homes();
        HomeLimitProvider limits = QuickHearth.get().limits();
        List<String> names = storage.sortedNames(player.getUUID());
        int max = limits.max(player);
        int used = names.size();

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.literal("Homes (" + used + "/" + max + ")"));

        if (names.isEmpty()) {
            gui.setSlot(13, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("\u00a7eNo homes yet"))
                .addLoreLine(Component.literal("\u00a77Use \u00a7f/sethome <name>\u00a77 to save your spot."))
                .addLoreLine(Component.literal("\u00a77Then use \u00a7f/home <name>\u00a77 or this menu."))
                .build());
        } else {
            int slot = 0;
            for (String name : names) {
                if (slot >= CAPACITY) break;
                Home home = storage.get(player.getUUID(), name).orElse(null);
                if (home == null) continue;
                final Home homeRef = home;
                final int slotRef = slot;
                gui.setSlot(slot, new GuiElementBuilder(Items.WHITE_BANNER)
                    .setName(Component.literal("\u00a7f" + homeRef.name()))
                    .addLoreLine(Component.literal("\u00a77" + homeRef.dimension()))
                    .addLoreLine(Component.literal("\u00a78" + (int) homeRef.x() + ", " + (int) homeRef.y() + ", " + (int) homeRef.z()))
                    .addLoreLine(Component.literal(""))
                    .addLoreLine(Component.literal("\u00a7eClick to teleport"))
                    .addLoreLine(Component.literal("\u00a7cShift-click to delete"))
                    .setCallback((index, type, input, slotGui) -> {
                        gui.close();
                        if (type.shift) {
                            storage.delete(player.getUUID(), homeRef.name());
                            player.sendSystemMessage(Component.literal("\u00a77Removed home \u00a7f" + homeRef.name()));
                        } else {
                            HomeCommand.teleport(player, homeRef);
                        }
                    })
                    .build());
                slot++;
            }
        }

        gui.open();
    }
}
