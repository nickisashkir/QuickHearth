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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class HomeStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dir;
    private final Map<UUID, Map<String, Home>> cache = new HashMap<>();

    public HomeStorage(MinecraftServer server) {
        this.dir = server.getWorldPath(LevelResource.ROOT).resolve("quickhearth").resolve("homes");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            QuickHearth.LOGGER.error("Failed to create home storage directory at {}", dir, e);
        }
    }

    public synchronized Map<String, Home> homes(UUID owner) {
        return cache.computeIfAbsent(owner, this::load);
    }

    public synchronized Optional<Home> get(UUID owner, String name) {
        return Optional.ofNullable(homes(owner).get(name.toLowerCase(Locale.ROOT)));
    }

    public synchronized void set(UUID owner, Home home) {
        homes(owner).put(home.name().toLowerCase(Locale.ROOT), home);
        save(owner);
    }

    public synchronized boolean delete(UUID owner, String name) {
        boolean removed = homes(owner).remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) save(owner);
        return removed;
    }

    public synchronized List<String> sortedNames(UUID owner) {
        List<String> names = new ArrayList<>();
        for (Home h : homes(owner).values()) names.add(h.name());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public synchronized void flushAll() {
        for (UUID id : new ArrayList<>(cache.keySet())) save(id);
    }

    private Path file(UUID owner) {
        return dir.resolve(owner.toString() + ".json");
    }

    private Map<String, Home> load(UUID owner) {
        Path f = file(owner);
        Map<String, Home> map = new LinkedHashMap<>();
        if (!Files.exists(f)) return map;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("homes");
            if (arr != null) {
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    String iconJson = null;
                    if (o.has("icon") && !o.get("icon").isJsonNull()) {
                        JsonElement iconEl = o.get("icon");
                        iconJson = iconEl.isJsonPrimitive() ? iconEl.getAsString() : iconEl.toString();
                    }
                    Home h = new Home(
                        o.get("name").getAsString(),
                        o.get("dim").getAsString(),
                        o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(),
                        o.get("z").getAsDouble(),
                        o.has("yaw") ? o.get("yaw").getAsFloat() : 0f,
                        o.has("pitch") ? o.get("pitch").getAsFloat() : 0f,
                        iconJson
                    );
                    map.put(h.name().toLowerCase(Locale.ROOT), h);
                }
            }
        } catch (Exception e) {
            QuickHearth.LOGGER.warn("Failed to load homes for {}: {}", owner, e.getMessage());
        }
        return map;
    }

    private void save(UUID owner) {
        Map<String, Home> map = cache.get(owner);
        if (map == null) return;
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Home h : map.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", h.name());
            o.addProperty("dim", h.dimension());
            o.addProperty("x", h.x());
            o.addProperty("y", h.y());
            o.addProperty("z", h.z());
            o.addProperty("yaw", h.yaw());
            o.addProperty("pitch", h.pitch());
            if (h.icon() != null && !h.icon().isBlank()) {
                o.add("icon", JsonParser.parseString(h.icon()));
            }
            arr.add(o);
        }
        root.add("homes", arr);
        try {
            Files.writeString(file(owner), GSON.toJson(root));
        } catch (IOException e) {
            QuickHearth.LOGGER.warn("Failed to save homes for {}: {}", owner, e.getMessage());
        }
    }
}
