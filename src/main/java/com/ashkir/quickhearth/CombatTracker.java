package com.ashkir.quickhearth;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTracker {
    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::onDamage);
    }

    private void onDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
        if (Config.COMBAT_TAG_TICKS <= 0) return;
        if (!(victim instanceof ServerPlayer victimPlayer)) return;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player) || attacker == victim) return;
        long until = victimPlayer.level().getGameTime() + Config.COMBAT_TAG_TICKS;
        taggedUntil.put(victimPlayer.getUUID(), until);
    }

    public boolean isTagged(ServerPlayer player) {
        if (Config.COMBAT_TAG_TICKS <= 0) return false;
        Long until = taggedUntil.get(player.getUUID());
        if (until == null) return false;
        return player.level().getGameTime() < until;
    }

    public int remainingSeconds(ServerPlayer player) {
        Long until = taggedUntil.get(player.getUUID());
        if (until == null) return 0;
        long remain = until - player.level().getGameTime();
        if (remain <= 0) return 0;
        return (int) ((remain + 19) / 20);
    }
}
