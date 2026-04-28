package com.ashkir.quickhearth;

public final class Config {
    private Config() {}

    public static int WARMUP_TICKS = 60;
    public static int HOME_COOLDOWN_TICKS = 600;
    public static int SPAWN_COOLDOWN_TICKS = 600;
    public static int TPA_REQUEST_TIMEOUT_TICKS = 1200;
    public static int DEFAULT_MAX_HOMES = 1;
    public static String LP_META_KEY = "homes-max";
    public static String BONUS_OBJECTIVE_NAME = "homes_bonus";
    public static int COMBAT_TAG_TICKS = 0;

    public static void apply(ModConfig cfg) {
        WARMUP_TICKS = cfg.warmupSeconds * 20;
        HOME_COOLDOWN_TICKS = cfg.homeCooldownSeconds * 20;
        SPAWN_COOLDOWN_TICKS = cfg.spawnCooldownSeconds * 20;
        TPA_REQUEST_TIMEOUT_TICKS = cfg.tpaRequestTimeoutSeconds * 20;
        DEFAULT_MAX_HOMES = cfg.defaultMaxHomes;
        LP_META_KEY = cfg.luckPermsMetaKey;
        BONUS_OBJECTIVE_NAME = cfg.bonusObjectiveName;
        COMBAT_TAG_TICKS = cfg.combatTagSeconds * 20;
    }
}
