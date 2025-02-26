package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class XPTask implements QuestTask<XPTask.XPTaskResolved> {

    public static final QuestEntryKey<XPTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "xp"));
    public static final Codec<XPTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (amount, desc, pred) -> new XPTask(amount, desc.orElse(""), pred.orElse(null))));

    private final String description;

    private final NumberProvider amount;
    @Nullable
    private final EntityPredicate playerPredicate;

    public XPTask(NumberProvider amount, String description, @Nullable EntityPredicate player) {
        if (description.isEmpty() && !(amount instanceof ConstantValue))
            throw new IllegalStateException("Description is required");
        this.description = description;
        this.amount = amount;
        this.playerPredicate = player;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.amount instanceof ConstantValue c) {
            // Can pass null since its constant
            return new TranslatableComponent(this.getId().toString(), c.getInt(null));
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<XPTask> getId() {
        return ID;
    }

    @Override
    public XPTaskResolved resolve(PlayerQuestData data, QuestProgress progress, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        return new XPTaskResolved(QuestUtils.getAmount(this.amount, ctx, data, base.id), this.playerPredicate);
    }

    public record XPTaskResolved(int amount, @Nullable EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<XPTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (amount, pred) -> new XPTaskResolved(amount, pred.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            if (player.experienceLevel >= this.amount) {
                player.giveExperienceLevels(-this.amount);
                return true;
            }
            return false;
        }

        @Override
        public QuestEntryKey<XPTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(this.getId().toString(), this.amount);
        }
    }
}
