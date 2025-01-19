package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record XPEntry(int amount,
                      EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<XPEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "xp"));
    public static final Codec<XPEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (amount, pred) -> new XPEntry(amount, pred.orElse(null))));

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
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        return new TranslatableComponent(this.getId().toString(), this.amount);
    }
}
