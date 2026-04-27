package com.ashkir.quickhearth.teleport;

import com.ashkir.quickhearth.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeleportService {
    public record Destination(ServerLevel level, double x, double y, double z, float yaw, float pitch) {}

    private static final class Pending {
        final ServerPlayer player;
        final Destination dest;
        final Vec3 origin;
        final long completeAt;
        final Runnable onComplete;

        Pending(ServerPlayer p, Destination d, long completeAt, Runnable onComplete) {
            this.player = p;
            this.dest = d;
            this.origin = p.position();
            this.completeAt = completeAt;
            this.onComplete = onComplete;
        }
    }

    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public boolean isOnCooldown(ServerPlayer p) {
        Long until = cooldownUntil.get(p.getUUID());
        return until != null && p.level().getGameTime() < until;
    }

    public int cooldownRemaining(ServerPlayer p) {
        Long until = cooldownUntil.get(p.getUUID());
        if (until == null) return 0;
        long remaining = until - p.level().getGameTime();
        if (remaining <= 0) return 0;
        return (int) ((remaining + 19) / 20);
    }

    public boolean queue(ServerPlayer p, Destination dest, String label, int cooldownTicks) {
        return queue(p, dest, label, cooldownTicks, null);
    }

    public boolean queue(ServerPlayer p, Destination dest, String label, int cooldownTicks, Runnable onComplete) {
        if (pending.containsKey(p.getUUID())) {
            p.sendSystemMessage(Component.literal("\u00a7cYou already have a teleport queued."));
            return false;
        }
        long now = p.level().getGameTime();
        long completeAt = now + Config.WARMUP_TICKS;
        UUID id = p.getUUID();
        Pending pt = new Pending(p, dest, completeAt, () -> {
            cooldownUntil.put(id, p.level().getGameTime() + cooldownTicks);
            if (onComplete != null) onComplete.run();
        });
        pending.put(id, pt);
        int seconds = (Config.WARMUP_TICKS + 19) / 20;
        p.sendSystemMessage(Component.literal("\u00a77Teleporting to \u00a7f" + label + "\u00a77 in \u00a7e" + seconds + "s\u00a77. Don't move."));
        return true;
    }

    public void tick() {
        if (pending.isEmpty()) return;
        Iterator<Map.Entry<UUID, Pending>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Pending pt = it.next().getValue();
            ServerPlayer p = pt.player;
            if (!p.isAlive() || p.hasDisconnected() || p.isRemoved()) {
                it.remove();
                continue;
            }
            Vec3 cur = p.position();
            double dx = cur.x - pt.origin.x;
            double dz = cur.z - pt.origin.z;
            if (dx * dx + dz * dz > 0.04) {
                it.remove();
                p.sendSystemMessage(Component.literal("\u00a7cTeleport cancelled: you moved."));
                continue;
            }
            if (p.level().getGameTime() >= pt.completeAt) {
                it.remove();
                doTeleport(p, pt.dest);
                if (pt.onComplete != null) pt.onComplete.run();
            }
        }
    }

    public static void doTeleport(ServerPlayer p, Destination d) {
        p.teleportTo(d.level(), d.x(), d.y(), d.z(), Set.<Relative>of(), d.yaw(), d.pitch(), false);
    }
}
