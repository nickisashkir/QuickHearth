package com.ashkir.quickhearth.data;

import java.util.UUID;

public record SharedHome(UUID owner, String ownerName, String homeName, UUID recipient, long sharedAt) {}
