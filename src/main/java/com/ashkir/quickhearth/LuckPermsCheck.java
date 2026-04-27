package com.ashkir.quickhearth;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

final class LuckPermsCheck {
    private LuckPermsCheck() {}

    static Boolean check(UUID uuid, String permission) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return null;
        CachedPermissionData data = user.getCachedData().getPermissionData();
        Tristate t = data.checkPermission(permission);
        if (t == Tristate.UNDEFINED) return null;
        return t.asBoolean();
    }
}
