package com.ashkir.quickhearth.limits;

import com.ashkir.quickhearth.Config;
import com.ashkir.quickhearth.QuickHearth;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.UUID;

final class LuckPermsBridge {
    private LuckPermsBridge() {}

    static int lookupRankBase(UUID uuid) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return Config.DEFAULT_MAX_HOMES;

        CachedMetaData meta = user.getCachedData().getMetaData();
        String value = meta.getMetaValue(Config.LP_META_KEY);
        if (value == null) return Config.DEFAULT_MAX_HOMES;

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            QuickHearth.LOGGER.warn("Invalid {} meta value for {}: '{}'", Config.LP_META_KEY, uuid, value);
            return Config.DEFAULT_MAX_HOMES;
        }
    }
}
