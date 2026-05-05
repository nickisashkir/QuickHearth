package com.ashkir.quickhearth;

import com.ashkir.quickhearth.command.BuyHomeCommand;
import com.ashkir.quickhearth.command.DelHomeCommand;
import com.ashkir.quickhearth.command.HomeCommand;
import com.ashkir.quickhearth.command.HomeIconCommand;
import com.ashkir.quickhearth.command.HomeRenameCommand;
import com.ashkir.quickhearth.command.HomesCommand;
import com.ashkir.quickhearth.command.QuickHearthCommand;
import com.ashkir.quickhearth.command.SetHomeCommand;
import com.ashkir.quickhearth.command.ShareAcceptCommand;
import com.ashkir.quickhearth.command.ShareDenyCommand;
import com.ashkir.quickhearth.command.ShareHomeCommand;
import com.ashkir.quickhearth.command.SharedHomesCommand;
import com.ashkir.quickhearth.command.SpawnCommand;
import com.ashkir.quickhearth.command.TpAcceptCommand;
import com.ashkir.quickhearth.command.TpDenyCommand;
import com.ashkir.quickhearth.command.TpaCommand;
import com.ashkir.quickhearth.command.TpaHereCommand;
import com.ashkir.quickhearth.command.TpaToggleCommand;
import com.ashkir.quickhearth.command.UnshareHomeCommand;
import com.ashkir.quickhearth.data.HomeStorage;
import com.ashkir.quickhearth.data.PlayerToggleStorage;
import com.ashkir.quickhearth.data.ShareStorage;
import com.ashkir.quickhearth.limits.HomeLimitProvider;
import com.ashkir.quickhearth.teleport.ShareManager;
import com.ashkir.quickhearth.teleport.TeleportService;
import com.ashkir.quickhearth.teleport.TpaManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuickHearth implements ModInitializer {
    public static final String MOD_ID = "quickhearth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static QuickHearth INSTANCE;

    private MinecraftServer server;
    private ConfigManager configManager;
    private HomeStorage homeStorage;
    private PlayerToggleStorage toggleStorage;
    private ShareStorage shareStorage;
    private HomeLimitProvider limitProvider;
    private TeleportService teleportService;
    private TpaManager tpaManager;
    private ShareManager sharePending;
    private CombatTracker combatTracker;

    public static QuickHearth get() { return INSTANCE; }

    public MinecraftServer server() { return server; }
    public ConfigManager configManager() { return configManager; }
    public HomeStorage homes() { return homeStorage; }
    public PlayerToggleStorage toggles() { return toggleStorage; }
    public ShareStorage shares() { return shareStorage; }
    public HomeLimitProvider limits() { return limitProvider; }
    public TeleportService teleport() { return teleportService; }
    public TpaManager tpa() { return tpaManager; }
    public ShareManager sharePending() { return sharePending; }
    public CombatTracker combat() { return combatTracker; }

    @Override
    public void onInitialize() {
        INSTANCE = this;

        this.configManager = new ConfigManager();
        this.configManager.load();

        this.combatTracker = new CombatTracker();
        this.combatTracker.register();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (teleportService != null) teleportService.tick();
            if (tpaManager != null) tpaManager.tick();
            if (sharePending != null) sharePending.tick();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            HomeCommand.register(dispatcher);
            SetHomeCommand.register(dispatcher);
            DelHomeCommand.register(dispatcher);
            HomesCommand.register(dispatcher);
            SpawnCommand.register(dispatcher);
            TpaCommand.register(dispatcher);
            TpaHereCommand.register(dispatcher);
            TpAcceptCommand.register(dispatcher);
            TpDenyCommand.register(dispatcher);
            TpaToggleCommand.register(dispatcher);
            ShareHomeCommand.register(dispatcher);
            ShareAcceptCommand.register(dispatcher);
            ShareDenyCommand.register(dispatcher);
            UnshareHomeCommand.register(dispatcher);
            SharedHomesCommand.register(dispatcher);
            HomeIconCommand.register(dispatcher);
            HomeRenameCommand.register(dispatcher);
            BuyHomeCommand.register(dispatcher);
            QuickHearthCommand.register(dispatcher);
        });
    }

    private void onServerStarted(MinecraftServer s) {
        this.server = s;
        this.homeStorage = new HomeStorage(s);
        this.toggleStorage = new PlayerToggleStorage(s);
        this.shareStorage = new ShareStorage(s);
        this.limitProvider = new HomeLimitProvider();
        this.teleportService = new TeleportService();
        this.tpaManager = new TpaManager();
        this.sharePending = new ShareManager();
        ensureBonusObjective(s);
    }

    private void onServerStopping(MinecraftServer s) {
        if (homeStorage != null) homeStorage.flushAll();
        if (toggleStorage != null) toggleStorage.flush();
        if (shareStorage != null) shareStorage.save();
    }

    private void ensureBonusObjective(MinecraftServer s) {
        String name = Config.BONUS_OBJECTIVE_NAME;
        Scoreboard sb = s.getScoreboard();
        Objective obj = sb.getObjective(name);
        if (obj == null) {
            sb.addObjective(name, ObjectiveCriteria.DUMMY,
                Component.literal("Bonus Homes"), ObjectiveCriteria.RenderType.INTEGER, false, null);
            LOGGER.info("Created scoreboard objective '{}' for bonus home grants", name);
        }
    }
}
