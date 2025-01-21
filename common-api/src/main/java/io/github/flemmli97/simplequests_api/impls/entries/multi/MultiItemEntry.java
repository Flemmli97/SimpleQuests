package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.ItemEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class MultiItemEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiItemEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_item"));
    public static final Codec<MultiItemEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "predicates cant' be empty").fieldOf("predicates").forGetter(d -> d.predicate),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("consumeItems").forGetter(d -> d.consumeItems),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (pred, amount, consume, desc, player) -> new MultiItemEntry(pred, amount, desc, consume, player.orElse(null))));

    private final List<Either<ItemPredicate, Pair<ItemPredicate, String>>> predicate;
    private final NumberProvider amount;
    private final boolean consumeItems;
    private final EntityPredicate playerPredicate;

    public MultiItemEntry(List<Either<ItemPredicate, Pair<ItemPredicate, String>>> predicate, NumberProvider amount, String description, boolean consumeItems, EntityPredicate playerPredicate) {
        super(description);
        this.predicate = predicate;
        this.amount = amount;
        this.consumeItems = consumeItems;
        this.playerPredicate = playerPredicate;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Either<ItemPredicate, Pair<ItemPredicate, String>> val = this.predicate.get(ctx.getRandom().nextInt(this.predicate.size()));
        return new ItemEntry(val.map(e -> e, Pair::getFirst),
                QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), val.map(e -> "", Pair::getSecond), this.consumeItems, this.playerPredicate);
    }
}
