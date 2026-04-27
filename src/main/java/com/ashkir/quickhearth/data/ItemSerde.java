package com.ashkir.quickhearth.data;

import com.ashkir.quickhearth.QuickHearth;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ItemSerde {
    private ItemSerde() {}

    public static String serialize(ItemStack stack, RegistryAccess access) {
        if (stack == null || stack.isEmpty()) return null;
        ItemStack normalized = stack.copyWithCount(1);
        RegistryOps<JsonElement> ops = access.createSerializationContext(JsonOps.INSTANCE);
        DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(ops, normalized);
        Optional<JsonElement> ok = result.result();
        if (ok.isEmpty()) {
            QuickHearth.LOGGER.debug("Failed to serialize home icon: {}",
                result.error().map(e -> e.message()).orElse("unknown"));
            return null;
        }
        return ok.get().toString();
    }

    public static Optional<ItemStack> deserialize(String json, RegistryAccess access) {
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            JsonElement element = JsonParser.parseString(json);
            RegistryOps<JsonElement> ops = access.createSerializationContext(JsonOps.INSTANCE);
            DataResult<com.mojang.datafixers.util.Pair<ItemStack, JsonElement>> result =
                ItemStack.CODEC.decode(ops, element);
            return result.result().map(com.mojang.datafixers.util.Pair::getFirst);
        } catch (Exception e) {
            QuickHearth.LOGGER.debug("Failed to deserialize home icon: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
