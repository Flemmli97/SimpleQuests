package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.api.PlayerQuestData;
import io.github.flemmli97.simplequests_api.impls.entries.single.FishingEntry;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class MultiFishingEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiFishingEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_fishing"));
    public static final Codec<MultiFishingEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                    JsonCodecs.descriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "item predicates can't be empty").fieldOf("itemPredicates").forGetter(d -> d.heldItems),
                    Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(JsonCodecs.ENTITY_PREDICATE_CODEC, Codec.STRING)).listOf()
                            .optionalFieldOf("entityPredicates").forGetter(d -> d.entityPredicates.isEmpty() ? Optional.empty() : Optional.of(d.entityPredicates)),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
            ).apply(instance, (desc, taskDescription, item, pred, amount) -> new MultiFishingEntry(item, pred.orElse(List.of()), amount, desc, taskDescription)));

    private final List<Pair<ItemPredicate, String>> heldItems;
    private final List<Pair<EntityPredicate, String>> entityPredicates;
    private final NumberProvider amount;
    private final String taskDescription;

    public MultiFishingEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<EntityPredicate, String>> entityPredicates, NumberProvider amount, String description, String taskDescription) {
        super(description);
        this.heldItems = heldItems;
        this.entityPredicates = entityPredicates;
        this.amount = amount;
        this.taskDescription = taskDescription;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
        Pair<EntityPredicate, String> entity = this.entityPredicates.isEmpty() ? Pair.of(EntityPredicate.ANY, "") : this.entityPredicates.get(ctx.getRandom().nextInt(this.entityPredicates.size()));
        return new FishingEntry(val.getFirst(), entity.getFirst(), QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), this.taskDescription, val.getSecond(), entity.getSecond());
    }
}
