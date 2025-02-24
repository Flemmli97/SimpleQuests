package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.function.Predicate;

public class PredicateTask implements QuestTask<PredicateTask.PredicateTaskResolved> {

    public static final QuestEntryKey<PredicateTask> ID = new QuestEntryKey<>(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "predicates"));
    public static final MapCodec<PredicateTask> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.codecDesc(EntityPredicate.CODEC), "entity predicates can't be empty").fieldOf("predicates").forGetter(d -> d.predicates),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> QuestUtils.optStr(d.description)),
                    Codec.BOOL.fieldOf("submit").forGetter(d -> d.submit)
            ).apply(instance, (locs, desc, submit) -> new PredicateTask(locs, desc.orElse(""), submit)));

    private final String description;

    private final List<DescriptiveValue<EntityPredicate>> predicates;
    private final boolean submit;

    public PredicateTask(List<DescriptiveValue<EntityPredicate>> predicates, String description) {
        this(predicates, description, false);
    }

    public PredicateTask(List<DescriptiveValue<EntityPredicate>> predicates, String description, boolean submit) {
        this.submit = submit;
        if (predicates.size() > 1 && description.isEmpty())
            throw new IllegalStateException("Description is required");
        this.description = description;
        this.predicates = predicates;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.predicates.size() == 1) {
            DescriptiveValue<EntityPredicate> loc = this.predicates.get(0);
            return loc.getTranslation();
        }
        return Component.translatable(this.description);
    }

    @Override
    public QuestEntryKey<PredicateTask> getId() {
        return ID;
    }

    @Override
    public PredicateTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<EntityPredicate> val = this.predicates.get(ctx.getRandom().nextInt(this.predicates.size()));
        return new PredicateTaskResolved(val, this.submit);
    }

    public record PredicateTaskResolved(DescriptiveValue<EntityPredicate> predicate,
                                        boolean submit) implements ResolvedQuestTask {

        public static final MapCodec<PredicateTaskResolved> CODEC = RecordCodecBuilder.mapCodec((instance) ->
                instance.group(DescriptiveValue.codec(EntityPredicate.CODEC).fieldOf("predicate").forGetter(d -> d.predicate),
                        Codec.BOOL.fieldOf("submit").forGetter(d -> d.submit)
                ).apply(instance, PredicateTaskResolved::new));

        @Override
        public boolean submit(ServerPlayer player) {
            return this.submit && this.predicate.value().matches(player, player);
        }

        @Override
        public QuestEntryKey<PredicateTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.predicate.getTranslation();
        }

        @Override
        public Predicate<PlayerQuestData> tickable() {
            if (this.submit)
                return null;
            return data -> {
                ServerPlayer player = data.getPlayer();
                return player.tickCount % 20 == 0 && this.predicate.value().matches(player, player);
            };
        }
    }
}
