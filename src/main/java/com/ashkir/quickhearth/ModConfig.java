package com.ashkir.quickhearth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public String _help = "QuickHearth config. Edit and run /quickhearth reload to apply. Full documentation: https://github.com/nickisashkir/QuickHearth#configuration";

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

    public HomeSharing homeSharing = new HomeSharing();
    public TeleportXpCost teleportXpCost = new TeleportXpCost();
    public BuyHome buyHome = new BuyHome();

    public static class HomeSharing {
        public String _doc = "Player-to-player home sharing. enabled: master toggle for /sharehome and the shared-homes UI. countTowardLimit: when true, accepted shares consume a slot from the recipient's home quota; when false (default), shared homes are bonus overage.";
        public boolean enabled = true;
        public boolean countTowardLimit = false;
    }

    public static class TeleportXpCost {
        public String _doc = "Charge XP levels for teleports. Mover pays for TPA (requester for /tpa, target for /tpahere). Formula: cost = baseCost + (distance / blocksPerLevel) for same-dimension, or crossDimensionFlat for inter-dimension. If home name matches primaryHomeName (case-insensitive), cost is multiplied by primaryHomeMultiplier. Capped at maxLevels. Set blocksPerLevel to 0 to disable distance scaling. Per-command bases: homeBase, spawnBase, tpaBase, tpahereBase. Players with quickhearth.bypass.cost permission pay nothing.";
        public boolean enabled = false;
        public int homeBase = 0;
        public int spawnBase = 0;
        public int tpaBase = 0;
        public int tpahereBase = 0;
        public int blocksPerLevel = 0;
        public int crossDimensionFlat = 0;
        public int maxLevels = 30;
        public String primaryHomeName = "home";
        public double primaryHomeMultiplier = 0.5;
    }

    public static class BuyHome {
        public String _doc = "Players run /buyhome to spend XP levels for +1 home slot, persisting via the homes_bonus scoreboard objective. Stacks on top of LuckPerms rank base. Cost formula: baseCost * (costMultiplier ^ slotsAlreadyBought). So with baseCost=10 and costMultiplier=1.5: 1st bought slot = 10 levels, 2nd = 15, 3rd = 22, 4th = 33. Set costMultiplier to 1.0 for fixed cost. maxBonus 0 means unlimited.";
        public boolean enabled = false;
        public int baseCost = 10;
        public double costMultiplier = 1.5;
        public int maxBonus = 0;
    }
}
