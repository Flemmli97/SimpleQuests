package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.KillEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class MultiKillEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiKillEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_kill"));
    public static final Codec<MultiKillEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.ENTITY_PREDICATE_CODEC, "predicates can't be empty").fieldOf("predicates").forGetter(d -> d.predicate),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, MultiKillEntry::new));

    private final List<Either<EntityPredicate, Pair<EntityPredicate, String>>> predicate;
    private final NumberProvider amount;
    private final EntityPredicate playerPredicate;

    public MultiKillEntry(List<Either<EntityPredicate, Pair<EntityPredicate, String>>> predicate, NumberProvider amount, String description, Optional<EntityPredicate> player) {
        super(description);
        this.predicate = predicate;
        this.amount = amount;
        this.playerPredicate = player.orElse(null);
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Either<EntityPredicate, Pair<EntityPredicate, String>> val = this.predicate.get(ctx.getRandom().nextInt(this.predicate.size()));
        return new KillEntry(val.map(e -> e, Pair::getFirst),
                QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), val.map(e -> "", Pair::getSecond), this.playerPredicate);
    }
}
