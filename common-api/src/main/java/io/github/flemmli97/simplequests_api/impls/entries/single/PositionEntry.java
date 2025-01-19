package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.api.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;
import java.util.function.Predicate;

public record PositionEntry(BlockPos pos, int minDist, String description,
                            EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<PositionEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "position"));
    public static final Codec<PositionEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.BLOCK_POS_CODEC.fieldOf("pos").forGetter(d -> d.pos),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("minDist").forGetter(d -> d.minDist),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (pred, amount, desc, player) -> new PositionEntry(pred, amount, desc.orElse(""), player.orElse(null))));

    @Override
    public boolean submit(ServerPlayer player) {
        return false;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        return new TranslatableComponent(!this.description.isEmpty() ? this.description : this.getId().toString(), this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    @Override
    public Predicate<PlayerQuestData> tickable() {
        return data -> {
            ServerPlayer player = data.getPlayer();
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            return player.tickCount % 20 == 0 && player.blockPosition().distSqr(this.pos) < this.minDist * this.minDist;
        };
    }
}
