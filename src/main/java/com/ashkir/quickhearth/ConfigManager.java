package com.ashkir.quickhearth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private ModConfig current;

    public ConfigManager() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve("quickhearth.json");
        this.current = new ModConfig();
    }

    public ModConfig get() {
        return current;
    }

    public void load() {
        if (!Files.exists(configFile)) {
            QuickHearth.LOGGER.info("No quickhearth.json found, writing defaults to {}", configFile);
            save();
            return;
        }
        try {
            String json = Files.readString(configFile);
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);
            if (loaded != null) {
                current = loaded;
            }
            normalize();
            Config.apply(current);
            QuickHearth.LOGGER.info("Loaded config from {}", configFile);
        } catch (Exception e) {
            QuickHearth.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            current = new ModConfig();
            Config.apply(current);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(current));
            Config.apply(current);
        } catch (IOException e) {
            QuickHearth.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public void reload() {
        load();
    }

    private void normalize() {
        if (current.blockedHomeDimensions == null) current.blockedHomeDimensions = new ArrayList<>();
        if (current.perDimensionPlayerCap == null) current.perDimensionPlayerCap = new LinkedHashMap<>();
        if (current.luckPermsMetaKey == null || current.luckPermsMetaKey.isBlank()) current.luckPermsMetaKey = "homes-max";
        if (current.bonusObjectiveName == null || current.bonusObjectiveName.isBlank()) current.bonusObjectiveName = "homes_bonus";
        if (current.warmupSeconds < 0) current.warmupSeconds = 0;
        if (current.homeCooldownSeconds < 0) current.homeCooldownSeconds = 0;
        if (current.spawnCooldownSeconds < 0) current.spawnCooldownSeconds = 0;
        if (current.tpaRequestTimeoutSeconds < 5) current.tpaRequestTimeoutSeconds = 5;
        if (current.defaultMaxHomes < 0) current.defaultMaxHomes = 0;
        if (current.combatTagSeconds < 0) current.combatTagSeconds = 0;
    }
}
