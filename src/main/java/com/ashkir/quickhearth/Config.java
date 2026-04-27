package com.ashkir.quickhearth;

public final class Config {
    private Config() {}

    public static final int WARMUP_TICKS = 60;
    public static final int HOME_COOLDOWN_TICKS = 600;
    public static final int SPAWN_COOLDOWN_TICKS = 600;
    public static final int TPA_REQUEST_TIMEOUT_TICKS = 1200;
    public static final int DEFAULT_MAX_HOMES = 1;
    public static final String LP_META_KEY = "homes-max";
}
