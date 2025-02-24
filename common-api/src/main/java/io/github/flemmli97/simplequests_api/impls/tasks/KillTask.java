package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class KillTask implements QuestTask<KillTask.KillTaskResolved> {

    public static final QuestEntryKey<KillTask> ID = new QuestEntryKey<>(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "entity"));
    public static final MapCodec<KillTask> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.withTranslation(EntityPredicate.CODEC), "predicates can't be empty").fieldOf("predicates").forGetter(d -> d.predicates),
                    NumberProviders.CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> QuestUtils.optStr(d.description)),
                    EntityPredicate.CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (predicates, amount, desc, player) -> new KillTask(predicates, amount, desc.orElse(""), player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<EntityPredicate>> predicates;
    private final NumberProvider amount;
    private final EntityPredicate playerPredicate;

    public KillTask(List<DescriptiveValue<EntityPredicate>> predicates, NumberProvider amount, String description, @Nullable EntityPredicate playerPredicate) {
        this.description = description;
        this.predicates = predicates;
        this.amount = amount;
        this.playerPredicate = playerPredicate;
        if (this.description.isEmpty() && !this.simple())
            throw new IllegalStateException("Description is required");
    }

    private boolean simple() {
        return this.predicates.size() == 1 && this.amount instanceof ConstantValue;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.simple()) {
            return this.predicates.get(0).getTranslation(this.getId().toString(),
                    this.amount.getInt(null));
        }
        return Component.translatable(this.description);
    }

    @Override
    public QuestEntryKey<KillTask> getId() {
        return ID;
    }

    @Override
    public KillTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<EntityPredicate> val = this.predicates.get(ctx.getRandom().nextInt(this.predicates.size()));
        return new KillTaskResolved(val, QuestUtils.getAmount(this.amount, ctx, data, base.id), this.playerPredicate);
    }

    public record KillTaskResolved(DescriptiveValue<EntityPredicate> predicate, int amount,
                                   EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final MapCodec<KillTaskResolved> CODEC = RecordCodecBuilder.mapCodec((instance) ->
                instance.group(DescriptiveValue.withTranslation(EntityPredicate.CODEC).fieldOf("predicate").forGetter(d -> d.predicate),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        EntityPredicate.CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (pred, amount, player) -> new KillTaskResolved(pred, amount, player.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<KillTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.predicate.getTranslation(this.getId().toString(), this.amount);
        }

        public boolean check(ServerPlayer player, Entity entity) {
            return (this.playerPredicate == null || this.playerPredicate.matches(player, player))
                    && this.predicate.value().matches(player, entity);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.progressComponent(player, KillTracker.KEY, id);
        }
    }
}
