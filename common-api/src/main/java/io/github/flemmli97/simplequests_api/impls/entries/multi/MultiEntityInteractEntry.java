package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.EntityInteractEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class MultiEntityInteractEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiEntityInteractEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_entity_interaction"));
    public static final Codec<MultiEntityInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                    JsonCodecs.descriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "empty item predicates")
                            .optionalFieldOf("itemPredicates").forGetter(d -> d.heldItems.isEmpty() ? Optional.empty() : Optional.of(d.heldItems)),
                    JsonCodecs.descriptiveList(JsonCodecs.ENTITY_PREDICATE_CODEC, "empty entity predicates")
                            .optionalFieldOf("entityPredicates").forGetter(d -> d.entityPredicates.isEmpty() ? Optional.empty() : Optional.of(d.entityPredicates)),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (consume, desc, taskDescription, item, pred, amount, player) -> new MultiEntityInteractEntry(item.orElse(List.of()), pred.orElse(List.of()), amount, consume, desc, taskDescription, player.orElse(null))));

    private final List<Pair<ItemPredicate, String>> heldItems;
    private final List<Pair<EntityPredicate, String>> entityPredicates;
    private final NumberProvider amount;
    private final boolean consume;
    private final String taskDescription;
    private final EntityPredicate playerPredicate;

    public MultiEntityInteractEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<EntityPredicate, String>> entityPredicates, NumberProvider amount, boolean consume, String description, String taskDescription, EntityPredicate playerPredicate) {
        super(description);
        this.heldItems = heldItems;
        this.entityPredicates = entityPredicates;
        this.amount = amount;
        this.consume = consume;
        this.taskDescription = taskDescription;
        this.playerPredicate = playerPredicate;
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
        return new EntityInteractEntry(val.getFirst(), entity.getFirst(), QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), this.consume, this.taskDescription, val.getSecond(), entity.getSecond(), this.playerPredicate);
    }
}
