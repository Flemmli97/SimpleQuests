package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.util.DescriptiveValue;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class PositionTask implements QuestTask<PositionTask.PositionTaskResolved> {

    public static final QuestEntryKey<PositionTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "position"));
    public static final Codec<PositionTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.codec(JsonCodecs.BLOCK_POS_CODEC), "positions can't be empty").fieldOf("positions").forGetter(d -> d.positions),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("min_dist").forGetter(d -> d.minDist),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> QuestUtils.optStr(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (poss, dist, desc, player) -> new PositionTask(poss, dist, desc.orElse(""), player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<BlockPos>> positions;
    private final int minDist;
    private final EntityPredicate playerPredicate;

    public PositionTask(List<DescriptiveValue<BlockPos>> positions, int minDist, String description, @Nullable EntityPredicate player) {
        if (positions.size() > 1 && description.isEmpty())
            throw new IllegalStateException("Description is required");
        this.description = description;
        this.positions = positions;
        this.minDist = minDist;
        this.playerPredicate = player;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.positions.size() == 1) {
            DescriptiveValue<BlockPos> pos = this.positions.get(0);
            return pos.getTranslation(this.getId().toString(), pos.value().getX(), pos.value().getY(), pos.value().getZ());
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<PositionTask> getId() {
        return ID;
    }

    @Override
    public PositionTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<BlockPos> val = this.positions.get(ctx.getRandom().nextInt(this.positions.size()));
        return new PositionTaskResolved(val, this.minDist, this.playerPredicate);
    }

    public record PositionTaskResolved(DescriptiveValue<BlockPos> pos, int minDist,
                                       EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<PositionTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(DescriptiveValue.codec(JsonCodecs.BLOCK_POS_CODEC).fieldOf("pos").forGetter(d -> d.pos),
                        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("min_dist").forGetter(d -> d.minDist),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (pred, amount, player) -> new PositionTaskResolved(pred, amount, player.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<PositionTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.pos.getTranslation(this.getId().toString(), this.pos.value().getX(), this.pos.value().getY(), this.pos.value().getZ());
        }

        @Override
        public Predicate<PlayerQuestData> tickable() {
            return data -> {
                ServerPlayer player = data.getPlayer();
                if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                    return false;
                return player.tickCount % 20 == 0 && player.blockPosition().distSqr(this.pos.value()) < this.minDist * this.minDist;
            };
        }
    }
}
