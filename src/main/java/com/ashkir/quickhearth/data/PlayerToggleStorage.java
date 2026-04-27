package com.ashkir.quickhearth.data;

import com.ashkir.quickhearth.QuickHearth;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerToggleStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Set<UUID> tpaDisabled = new HashSet<>();

    public PlayerToggleStorage(MinecraftServer server) {
        this.file = server.getWorldPath(LevelResource.ROOT).resolve("quickhearth").resolve("toggles.json");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            QuickHearth.LOGGER.warn("Failed to create toggles directory: {}", e.getMessage());
        }
        load();
    }

    public synchronized boolean isTpaDisabled(UUID p) {
        return tpaDisabled.contains(p);
    }

    public synchronized boolean toggleTpa(UUID p) {
        boolean nowDisabled;
        if (tpaDisabled.contains(p)) {
            tpaDisabled.remove(p);
            nowDisabled = false;
        } else {
            tpaDisabled.add(p);
            nowDisabled = true;
        }
        flush();
        return nowDisabled;
    }

    public synchronized void flush() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (UUID u : tpaDisabled) arr.add(u.toString());
        root.add("tpa_disabled", arr);
        try {
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException e) {
            QuickHearth.LOGGER.warn("Failed to save toggles: {}", e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("tpa_disabled");
            if (arr != null) {
                for (JsonElement e : arr) tpaDisabled.add(UUID.fromString(e.getAsString()));
            }
        } catch (Exception e) {
            QuickHearth.LOGGER.warn("Failed to load toggles: {}", e.getMessage());
        }
    }
}
