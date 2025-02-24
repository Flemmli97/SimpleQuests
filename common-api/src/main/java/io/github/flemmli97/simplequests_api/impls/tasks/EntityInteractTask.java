package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.util.DescriptiveValue;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Quest entry to check if a player interacts with an entity.
 */
public class EntityInteractTask implements QuestTask<EntityInteractTask.EntityInteractTaskResolved> {

    public static final QuestEntryKey<EntityInteractTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "entity_interact"));
    public static final Codec<EntityInteractTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),

                    JsonCodecs.nonEmptyList(DescriptiveValue.withTranslation(JsonCodecs.ENTITY_PREDICATE_CODEC), "Empty entity predicates")
                            .fieldOf("entity_predicates").forGetter(d -> d.entityPredicates),
                    DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).listOf()
                            .optionalFieldOf("item_predicates").forGetter(d -> d.itemPredicates.isEmpty() ? Optional.empty() : Optional.of(d.itemPredicates)),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
            ).apply(instance, (consume, desc, player, entity, item, amount) -> new EntityInteractTask(entity, item.orElse(List.of()), amount, consume, desc, player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<EntityPredicate>> entityPredicates;
    private final List<DescriptiveValue<ItemPredicate>> itemPredicates;
    private final NumberProvider amount;
    private final boolean consume;
    private final EntityPredicate playerPredicate;

    public EntityInteractTask(DescriptiveValue<EntityPredicate> entityPredicates, DescriptiveValue<ItemPredicate> itemPredicates, int amount, boolean consume, String description, @Nullable EntityPredicate playerPredicate) {
        this(List.of(entityPredicates), List.of(itemPredicates), ConstantValue.exactly(amount), consume, description, playerPredicate);
    }

    public EntityInteractTask(List<DescriptiveValue<EntityPredicate>> entityPredicates, List<DescriptiveValue<ItemPredicate>> itemPredicates, NumberProvider amount, boolean consume, String description, @Nullable EntityPredicate playerPredicate) {
        this.description = description;
        this.itemPredicates = itemPredicates;
        this.entityPredicates = entityPredicates;
        this.amount = amount;
        this.consume = consume;
        this.playerPredicate = playerPredicate;
    }

    private boolean simple() {
        return this.entityPredicates.size() == 1 && this.itemPredicates.size() <= 1 && this.amount instanceof ConstantValue;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.simple()) {
            DescriptiveValue<EntityPredicate> pred = this.entityPredicates.get(0);
            if (this.itemPredicates.isEmpty() || this.itemPredicates.get(0).value() == ItemPredicate.ANY)
                return pred.getTranslation(this.getId().toString(), this.amount.getInt(null));
            return pred.getTranslation(this.getId().toString() + ".item", this.itemPredicates.get(0).getTranslation(), this.amount.getInt(null));
        }
        return Component.translatable(this.description);
    }

    @Override
    public QuestEntryKey<EntityInteractTask> getId() {
        return ID;
    }

    @Override
    public EntityInteractTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<EntityPredicate> entity = this.entityPredicates.isEmpty() ? DescriptiveValue.of(EntityPredicate.ANY) : this.entityPredicates.get(ctx.getRandom().nextInt(this.entityPredicates.size()));
        DescriptiveValue<ItemPredicate> item = this.itemPredicates.isEmpty() ? DescriptiveValue.of(ItemPredicate.ANY) : this.itemPredicates.get(ctx.getRandom().nextInt(this.itemPredicates.size()));
        return new EntityInteractTaskResolved(entity, item, QuestUtils.getAmount(this.amount, ctx, data, base.id), this.consume, this.playerPredicate);
    }

    public record EntityInteractTaskResolved(DescriptiveValue<EntityPredicate> entityPredicate,
                                             DescriptiveValue<ItemPredicate> heldItem, int amount,
                                             boolean consume,
                                             EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<EntityInteractTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),

                        DescriptiveValue.withTranslation(JsonCodecs.ENTITY_PREDICATE_CODEC).fieldOf("predicate").forGetter(d -> d.entityPredicate),
                        DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).optionalFieldOf("item").forGetter(d -> d.heldItem.value() == ItemPredicate.ANY ? Optional.empty() : Optional.of(d.heldItem)),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount)
                ).apply(instance, (consume, player, pred, item, amount) ->
                        new EntityInteractTaskResolved(pred, item.orElse(null), amount, consume, player.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<EntityInteractTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            if (this.heldItem == null || this.heldItem.value() == ItemPredicate.ANY)
                return this.entityPredicate.getTranslation(this.getId().toString() + ".simple", this.amount);
            return this.entityPredicate.getTranslation(this.getId().toString(), this.heldItem.getTranslation(), this.amount);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.progressComponent(player, EntityTracker.KEY, id);
        }

        public boolean check(ServerPlayer player, Entity entity) {
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            boolean b = (this.heldItem == null || this.heldItem.value().matches(player.getMainHandItem())) &&
                    this.entityPredicate.value().matches(player, entity);
            if (b && this.consume && !player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }
            return b;
        }
    }
}
