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

    private static final int OWNED_PER_PAGE = 18;
    private static final int SHARED_INLINE_LIMIT = 9;

    public static void open(ServerPlayer player) {
        open(player, 0);
    }

    public static void open(ServerPlayer player, int page) {
        HomeStorage storage = QuickHearth.get().homes();
        HomeLimitProvider limits = QuickHearth.get().limits();
        List<String> names = storage.sortedNames(player.getUUID());
        List<SharedHome> shared = QuickHearth.get().shares().sharesFor(player.getUUID());
        int max = limits.max(player);
        int used = names.size();

        boolean paginated = used > OWNED_PER_PAGE;
        int totalPages = paginated ? (used + OWNED_PER_PAGE - 1) / OWNED_PER_PAGE : 1;
        page = Math.max(0, Math.min(page, totalPages - 1));

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        String title = paginated
            ? "Homes (" + used + "/" + max + ") - Page " + (page + 1) + "/" + totalPages
            : "Homes (" + used + "/" + max + ")";
        gui.setTitle(Component.literal(title));

        if (names.isEmpty() && shared.isEmpty()) {
            gui.setSlot(13, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("\u00a7eNo homes yet"))
                .addLoreLine(Component.literal("\u00a77Use \u00a7f/sethome <name>\u00a77 to save your spot."))
                .addLoreLine(Component.literal("\u00a77Hold any item first to set a custom icon."))
                .build());
            gui.open();
            return;
        }

        int startIndex = page * OWNED_PER_PAGE;
        int endIndex = Math.min(startIndex + OWNED_PER_PAGE, used);
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Home home = storage.get(player.getUUID(), names.get(i)).orElse(null);
            if (home == null) continue;
            final Home homeRef = home;
            final int currentPage = page;
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

        if (paginated) {
            placeNavAndSharedShortcut(gui, player, page, totalPages, shared);
        } else {
            placeSharedRow(gui, player, shared);
        }

        gui.open();
    }

    private static void placeNavAndSharedShortcut(SimpleGui gui, ServerPlayer player, int page, int totalPages, List<SharedHome> shared) {
        if (page > 0) {
            final int target = page - 1;
            gui.setSlot(18, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("\u00a7ePrevious page"))
                .setCallback((idx, type, input, slotGui) -> { gui.close(); open(player, target); })
                .build());
        }
        if (page < totalPages - 1) {
            final int target = page + 1;
            gui.setSlot(26, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("\u00a7eNext page"))
                .setCallback((idx, type, input, slotGui) -> { gui.close(); open(player, target); })
                .build());
        }
        if (!shared.isEmpty()) {
            gui.setSlot(22, new GuiElementBuilder(Items.WRITABLE_BOOK)
                .setName(Component.literal("\u00a7eShared Homes (" + shared.size() + ")"))
                .addLoreLine(Component.literal("\u00a77Click to view homes shared with you."))
                .setCallback((idx, type, input, slotGui) -> { gui.close(); SharedHomesGui.open(player); })
                .build());
        }
    }

    private static void placeSharedRow(SimpleGui gui, ServerPlayer player, List<SharedHome> shared) {
        if (shared.isEmpty()) return;
        boolean overflow = shared.size() > SHARED_INLINE_LIMIT;
        int displayLimit = overflow ? SHARED_INLINE_LIMIT - 1 : SHARED_INLINE_LIMIT;
        int sharedShown = 0;
        for (SharedHome share : shared) {
            if (sharedShown >= displayLimit) break;
            Optional<Home> ownerHome = QuickHearth.get().homes().get(share.owner(), share.homeName());
            if (ownerHome.isEmpty()) continue;
            final Home homeRef = ownerHome.get();
            final SharedHome shareRef = share;
            gui.setSlot(18 + sharedShown, SharedHomesGui.sharedBuilder(homeRef, shareRef, player)
                .setCallback((idx, type, input, slotGui) -> {
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
            gui.setSlot(26, new GuiElementBuilder(Items.WRITABLE_BOOK)
                .setName(Component.literal("\u00a7eMore shared homes..."))
                .addLoreLine(Component.literal("\u00a77You have " + shared.size() + " shared homes."))
                .addLoreLine(Component.literal("\u00a77Click to open the full list."))
                .setCallback((idx, type, input, slotGui) -> { gui.close(); SharedHomesGui.open(player); })
                .build());
        }
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
