package com.ashkir.quickhearth.gui;

import com.ashkir.quickhearth.QuickHearth;
import com.ashkir.quickhearth.command.HomeCommand;
import com.ashkir.quickhearth.data.Home;
import com.ashkir.quickhearth.data.ItemSerde;
import com.ashkir.quickhearth.data.SharedHome;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public final class SharedHomesGui {
    private SharedHomesGui() {}

    private static final int CAPACITY = 27;

    public static void open(ServerPlayer player) {
        List<SharedHome> shares = QuickHearth.get().shares().sharesFor(player.getUUID());

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.literal("Shared Homes (" + shares.size() + ")"));

        if (shares.isEmpty()) {
            gui.setSlot(13, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("\u00a7eNo shared homes yet"))
                .addLoreLine(Component.literal("\u00a77Friends can use \u00a7f/sharehome <you> <home>\u00a77."))
                .build());
            gui.open();
            return;
        }

        int slot = 0;
        for (SharedHome share : shares) {
            if (slot >= CAPACITY) break;
            Optional<Home> ownerHome = QuickHearth.get().homes().get(share.owner(), share.homeName());
            if (ownerHome.isEmpty()) continue;
            final Home homeRef = ownerHome.get();
            final SharedHome shareRef = share;
            gui.setSlot(slot, sharedBuilder(homeRef, shareRef, player)
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
            slot++;
        }

        gui.open();
    }

    static GuiElementBuilder sharedBuilder(Home home, SharedHome share, ServerPlayer viewer) {
        GuiElementBuilder builder;
        Optional<ItemStack> custom = ItemSerde.deserialize(home.icon(), viewer.level().getServer().registryAccess());
        if (custom.isPresent() && !custom.get().isEmpty()) {
            builder = GuiElementBuilder.from(custom.get());
            builder.setName(Component.literal("\u00a7f" + home.name() + " \u00a78from \u00a77" + share.ownerName()));
        } else {
            builder = new GuiElementBuilder(Items.LIGHT_BLUE_BANNER)
                .setName(Component.literal("\u00a7f" + home.name() + " \u00a78from \u00a77" + share.ownerName()));
        }
        return builder
            .addLoreLine(Component.literal("\u00a77" + home.dimension()))
            .addLoreLine(Component.literal("\u00a78" + (int) home.x() + ", " + (int) home.y() + ", " + (int) home.z()))
            .addLoreLine(Component.literal(""))
            .addLoreLine(Component.literal("\u00a7eClick to teleport"))
            .addLoreLine(Component.literal("\u00a7cShift-click to remove"));
    }
}
