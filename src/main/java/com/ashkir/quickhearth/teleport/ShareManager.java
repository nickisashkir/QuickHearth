package com.ashkir.quickhearth.teleport;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ShareManager {
    public record Pending(UUID owner, String ownerName, String homeName, UUID recipient, long expiresAt) {}

    private final Map<UUID, Pending> pendingByRecipient = new HashMap<>();

    public boolean propose(ServerPlayer owner, ServerPlayer recipient, String homeName) {
        long now = currentTick();
        Pending existing = pendingByRecipient.get(recipient.getUUID());
        if (existing != null && existing.expiresAt > now) return false;
        pendingByRecipient.put(recipient.getUUID(),
            new Pending(owner.getUUID(), owner.getGameProfile().name(),
                homeName, recipient.getUUID(),
                now + Config.TPA_REQUEST_TIMEOUT_TICKS));
        return true;
    }

    public Optional<Pending> consume(UUID recipient) {
        Pending p = pendingByRecipient.remove(recipient);
        if (p == null) return Optional.empty();
        if (p.expiresAt <= currentTick()) return Optional.empty();
        return Optional.of(p);
    }

    public void tick() {
        if (pendingByRecipient.isEmpty()) return;
        long now = currentTick();
        pendingByRecipient.values().removeIf(p -> p.expiresAt <= now);
    }

    private static long currentTick() {
        return QuickHearth.get().server().overworld().getGameTime();
    }
}
