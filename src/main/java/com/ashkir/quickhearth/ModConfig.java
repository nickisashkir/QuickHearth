package com.ashkir.quickhearth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public int warmupSeconds = 3;
    public int homeCooldownSeconds = 30;
    public int spawnCooldownSeconds = 30;
    public int tpaRequestTimeoutSeconds = 60;
    public int defaultMaxHomes = 1;
    public String luckPermsMetaKey = "homes-max";
    public String bonusObjectiveName = "homes_bonus";
    public List<String> blockedHomeDimensions = new ArrayList<>();
    public Map<String, Integer> perDimensionPlayerCap = new LinkedHashMap<>();
    public int combatTagSeconds = 0;
}
