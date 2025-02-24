package io.github.flemmli97.simplequests_api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.APIPlatform;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import java.util.Optional;
import java.util.function.Supplier;

public class QuestNumberProvider {

    public static class ContextMultiplierNumberProvider implements NumberProvider {

        public static final MapCodec<ContextMultiplierNumberProvider> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.FLOAT.fieldOf("multiplier").forGetter(d -> d.multiplier),
                Codec.FLOAT.optionalFieldOf("max").forGetter(d -> d.max == 0 ? Optional.empty() : Optional.of(d.max)),
                NumberProviders.CODEC.fieldOf("value").forGetter(d -> d.base)
        ).apply(inst, (mult, max, val) -> new ContextMultiplierNumberProvider(val, max.orElse(0f), mult)));

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
            return APIPlatform.INSTANCE.getQuestContextProvider();
        }
    }
}
