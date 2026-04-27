package com.ashkir.quickhearth.teleport;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TpaManager {
    public enum Direction { TO_TARGET, FROM_TARGET }

    public record Request(UUID requester, UUID target, Direction direction, long expiresAt) {}

    private final Map<UUID, Request> incomingByTarget = new HashMap<>();

    public boolean send(ServerPlayer requester, ServerPlayer target, Direction dir) {
        long now = currentTick();
        Request existing = incomingByTarget.get(target.getUUID());
        if (existing != null && existing.expiresAt() > now) {
            requester.sendSystemMessage(Component.literal("\u00a7cThat player already has a pending request."));
            return false;
        }
        incomingByTarget.put(target.getUUID(),
            new Request(requester.getUUID(), target.getUUID(), dir, now + Config.TPA_REQUEST_TIMEOUT_TICKS));
        return true;
    }

    public Optional<Request> consume(UUID target) {
        Request r = incomingByTarget.remove(target);
        if (r == null) return Optional.empty();
        if (r.expiresAt() <= currentTick()) return Optional.empty();
        return Optional.of(r);
    }

    public void tick() {
        if (incomingByTarget.isEmpty()) return;
        long now = currentTick();
        incomingByTarget.values().removeIf(r -> r.expiresAt() <= now);
    }

    private static long currentTick() {
        return QuickHearth.get().server().overworld().getGameTime();
    }
}
