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
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

public class AdvancementTask implements QuestTask<AdvancementTask.AdvancementTaskResolved> {

    public static final QuestEntryKey<AdvancementTask> ID = new QuestEntryKey<>(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "advancement"));
    public static final MapCodec<AdvancementTask> CODEC = RecordCodecBuilder.mapCodec((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.codec(ResourceLocation.CODEC), "Advancements list can't be empty").fieldOf("advancements").forGetter(d -> d.advancements),
                    Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                    EntityPredicate.CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (advancements, reset, description, predicate) -> new AdvancementTask(advancements, reset, description.orElse(""), predicate.orElse(null))));
    public static final String MISSING_ADVANCEMENT = "simplequests_api.missing.advancement";

    private final String description;

    private final List<DescriptiveValue<ResourceLocation>> advancements;
    private final boolean reset;
    private final EntityPredicate playerPredicate;

    public AdvancementTask(List<DescriptiveValue<ResourceLocation>> advancements, boolean reset, String description, EntityPredicate player) {
        if (advancements.size() > 1 && description.isEmpty())
            throw new IllegalStateException("Description is required");
        this.description = description;
        this.advancements = advancements;
        this.reset = reset;
        this.playerPredicate = player;
    }

    public static Component fromAdvancement(ResourceLocation advancementID, MinecraftServer server) {
        AdvancementHolder advancement = server.getAdvancements().get(advancementID);
        Component adv;
        if (advancement == null)
            adv = Component.translatable("simplequests.missing.advancement", advancementID);
        else
            adv = Advancement.name(advancement);
        return adv;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.advancements.size() == 1) {
            DescriptiveValue<ResourceLocation> advancement = this.advancements.get(0);
            return advancement.getTranslation(this.getId().toString(), fromAdvancement(advancement.value(), player.getServer()));
        }
        Object[] advancements = this.advancements.stream().map(r -> fromAdvancement(r.value(), player.getServer()))
                .toArray(Object[]::new);
        return Component.translatable(this.description, advancements);
    }

    @Override
    public QuestEntryKey<AdvancementTask> getId() {
        return ID;
    }

    @Override
    public AdvancementTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        return new AdvancementTaskResolved(this.advancements.get(ctx.getRandom().nextInt(this.advancements.size())), this.reset, this.playerPredicate);
    }

    public record AdvancementTaskResolved(DescriptiveValue<ResourceLocation> advancement, boolean reset,
                                          EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final MapCodec<AdvancementTaskResolved> CODEC = RecordCodecBuilder.mapCodec((instance) ->
                instance.group(DescriptiveValue.codec(ResourceLocation.CODEC).fieldOf("advancement").forGetter(d -> d.advancement),
                        Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset),
                        EntityPredicate.CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (advancement, reset, pred) -> new AdvancementTaskResolved(advancement, reset, pred.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            AdvancementHolder adv = player.getServer().getAdvancements().get(this.advancement.value());
            boolean ret = adv != null && (player.getAdvancements().getOrStartProgress(adv).isDone());
            if (ret && this.reset) {
                AdvancementProgress prog = player.getAdvancements().getOrStartProgress(adv);
                prog.getCompletedCriteria().forEach(s -> player.getAdvancements().revoke(adv, s));
            }
            return ret;
        }

        @Override
        public QuestEntryKey<AdvancementTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.advancement().getTranslation(this.getId().toString(), fromAdvancement(this.advancement().value(), player.getServer()));
        }
    }
}
