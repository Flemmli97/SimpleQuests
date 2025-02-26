package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FishingTask implements QuestTask<FishingTask.FishingTaskResolved> {

    public static final QuestEntryKey<FishingTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "fishing"));
    public static final Codec<FishingTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC), "Item predicates can't be empty").fieldOf("item_predicates").forGetter(d -> d.itemPredicates),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (predicates, amount, desc, player) -> new FishingTask(predicates, amount, desc, player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<ItemPredicate>> itemPredicates;
    private final NumberProvider amount;
    @Nullable
    private final EntityPredicate playerPredicate;

    public FishingTask(List<DescriptiveValue<ItemPredicate>> itemPredicates, NumberProvider amount, String description, @Nullable EntityPredicate playerPredicate) {
        this.description = description;
        this.itemPredicates = itemPredicates;
        this.amount = amount;
        this.playerPredicate = playerPredicate;
        if (this.description.isEmpty() && !this.simple())
            throw new IllegalStateException("Description is required");
    }

    private boolean simple() {
        return this.itemPredicates.size() == 1 && this.amount instanceof ConstantValue;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.simple()) {
            DescriptiveValue<ItemPredicate> pred = this.itemPredicates.get(0);
            return pred.getTranslation(this.getId().toString(), this.amount.getInt(null));
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<FishingTask> getId() {
        return ID;
    }

    @Override
    public FishingTaskResolved resolve(PlayerQuestData data, QuestProgress progress, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<ItemPredicate> val = this.itemPredicates.get(ctx.getRandom().nextInt(this.itemPredicates.size()));
        return new FishingTaskResolved(val, QuestUtils.getAmount(this.amount, ctx, data, base.id), this.playerPredicate);
    }

    public record FishingTaskResolved(DescriptiveValue<ItemPredicate> item,
                                      int amount,
                                      @Nullable EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<FishingTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).fieldOf("item").forGetter(d -> d.item),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (predicate, amount, player) -> new FishingTaskResolved(predicate, amount, player.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<FishingTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.item.getTranslation(this.getId().toString(), this.amount);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.progressComponent(player, FishingTracker.KEY, id);
        }

        public boolean check(ServerPlayer player, ItemStack stack) {
            return this.item.value().matches(stack) && (this.playerPredicate == null || this.playerPredicate.matches(player, player));
        }
    }
}
