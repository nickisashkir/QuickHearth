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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShareStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final List<SharedHome> shares = new ArrayList<>();

    public ShareStorage(MinecraftServer server) {
        this.file = server.getWorldPath(LevelResource.ROOT).resolve("quickhearth").resolve("shares.json");
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException ignore) {}
        load();
    }

    public synchronized void share(SharedHome share) {
        shares.removeIf(s -> matches(s, share.owner(), share.homeName(), share.recipient()));
        shares.add(share);
        save();
    }

    public synchronized boolean unshare(UUID owner, String homeName, UUID recipient) {
        boolean removed = shares.removeIf(s -> matches(s, owner, homeName, recipient));
        if (removed) save();
        return removed;
    }

    public synchronized void unshareAllForHome(UUID owner, String homeName) {
        boolean removed = shares.removeIf(s -> s.owner().equals(owner)
            && s.homeName().equalsIgnoreCase(homeName));
        if (removed) save();
    }

    public synchronized List<SharedHome> sharesFor(UUID recipient) {
        List<SharedHome> result = new ArrayList<>();
        for (SharedHome s : shares) {
            if (s.recipient().equals(recipient)) result.add(s);
        }
        result.sort((a, b) -> Long.compare(b.sharedAt(), a.sharedAt()));
        return result;
    }

    public synchronized List<SharedHome> sharesByOwner(UUID owner) {
        List<SharedHome> result = new ArrayList<>();
        for (SharedHome s : shares) {
            if (s.owner().equals(owner)) result.add(s);
        }
        return result;
    }

    private static boolean matches(SharedHome s, UUID owner, String homeName, UUID recipient) {
        return s.owner().equals(owner)
            && s.homeName().toLowerCase(Locale.ROOT).equals(homeName.toLowerCase(Locale.ROOT))
            && s.recipient().equals(recipient);
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("shares");
            if (arr != null) {
                for (JsonElement e : arr) {
                    JsonObject o = e.getAsJsonObject();
                    shares.add(new SharedHome(
                        UUID.fromString(o.get("owner").getAsString()),
                        o.has("ownerName") ? o.get("ownerName").getAsString() : "Unknown",
                        o.get("homeName").getAsString(),
                        UUID.fromString(o.get("recipient").getAsString()),
                        o.has("sharedAt") ? o.get("sharedAt").getAsLong() : 0L
                    ));
                }
            }
        } catch (Exception e) {
            QuickHearth.LOGGER.warn("Failed to load shares: {}", e.getMessage());
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (SharedHome s : shares) {
            JsonObject o = new JsonObject();
            o.addProperty("owner", s.owner().toString());
            o.addProperty("ownerName", s.ownerName());
            o.addProperty("homeName", s.homeName());
            o.addProperty("recipient", s.recipient().toString());
            o.addProperty("sharedAt", s.sharedAt());
            arr.add(o);
        }
        root.add("shares", arr);
        try {
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException e) {
            QuickHearth.LOGGER.warn("Failed to save shares: {}", e.getMessage());
        }
    }
}
