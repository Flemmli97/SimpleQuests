package io.github.flemmli97.simplequests_api.quest.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.function.Supplier;

public class QuestNumberProvider {

    public static LootNumberProviderType CONTEXT_MULTIPLIER;

    public static void init() {
        CONTEXT_MULTIPLIER = Registry.register(Registry.LOOT_NUMBER_PROVIDER_TYPE, new ResourceLocation(SimpleQuestsAPI.MODID, "context_multiplier"), new LootNumberProviderType(new ContextMultiplierNumberProvider.Serializer()));
    }

    public static class ContextMultiplierNumberProvider implements NumberProvider {

        private final NumberProvider base;
        private final float multiplier;
        private final float max;

        public ContextMultiplierNumberProvider(NumberProvider base, float multiplier, float max) {
            this.base = base;
            this.multiplier = multiplier;
            this.max = max;
        }

        @Override
        public float getFloat(LootContext lootContext) {
            return this.base.getFloat(lootContext);
        }

        public float getFloatWith(LootContext lootContext, Supplier<Float> ctx) {
            float multiplier = 1 + ctx.get() * this.multiplier;
            return this.getFloat(lootContext) * this.max != 0 ? Math.min(this.max, multiplier) : multiplier;
        }

        @Override
        public LootNumberProviderType getType() {
            return CONTEXT_MULTIPLIER;
        }

        public static class Serializer
                implements net.minecraft.world.level.storage.loot.Serializer<ContextMultiplierNumberProvider> {
            @Override
            public void serialize(JsonObject obj, ContextMultiplierNumberProvider value, JsonSerializationContext context) {
                obj.addProperty("multiplier", value.multiplier);
                obj.addProperty("max", value.max);
                obj.add("value", context.serialize(value.base));
            }

            @Override
            public ContextMultiplierNumberProvider deserialize(JsonObject obj, JsonDeserializationContext context) {
                float f = GsonHelper.getAsFloat(obj, "multiplier");
                NumberProvider provider = GsonHelper.getAsObject(obj, "value", context, NumberProvider.class);
                return new ContextMultiplierNumberProvider(provider, f, GsonHelper.getAsFloat(obj, "max", 0));
            }
        }
    }
}
