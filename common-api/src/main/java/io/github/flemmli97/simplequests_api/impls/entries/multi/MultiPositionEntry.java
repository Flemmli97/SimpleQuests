package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.api.PlayerQuestData;
import io.github.flemmli97.simplequests_api.impls.entries.single.PositionEntry;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

public class MultiPositionEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiPositionEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_position"));
    public static final Codec<MultiPositionEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.BLOCK_POS_CODEC, "positions can't be empty").fieldOf("positions").forGetter(d -> d.positions),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("minDist").forGetter(d -> d.minDist),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, MultiPositionEntry::new));

    private final List<Either<BlockPos, Pair<BlockPos, String>>> positions;
    private final int minDist;
    private final EntityPredicate playerPredicate;

    public MultiPositionEntry(List<Either<BlockPos, Pair<BlockPos, String>>> positions, int minDist, String description, Optional<EntityPredicate> player) {
        super(description);
        this.positions = positions;
        this.minDist = minDist;
        this.playerPredicate = player.orElse(null);
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Either<BlockPos, Pair<BlockPos, String>> val = this.positions.get(ctx.getRandom().nextInt(this.positions.size()));
        return new PositionEntry(val.map(e -> e, Pair::getFirst), this.minDist, val.map(e -> "", Pair::getSecond), this.playerPredicate);
    }
}
