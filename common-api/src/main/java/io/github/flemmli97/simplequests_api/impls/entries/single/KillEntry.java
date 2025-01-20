package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
import io.github.flemmli97.simplequests_api.mixin.EntityPredicateAccessor;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record KillEntry(EntityPredicate predicate, int amount, String description,
                        EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<KillEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "entity"));
    public static final Codec<KillEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.ENTITY_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.predicate),
                    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (pred, amount, desc, player) -> new KillEntry(pred, amount, desc.orElse(""), player.orElse(null))));

    @Override
    public boolean submit(ServerPlayer player) {
        return false;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        EntityPredicateAccessor acc = (EntityPredicateAccessor) this.predicate;
        String s = acc.getEntityType().serializeToJson().getAsString();
        if (s.startsWith("#")) {
            return new TranslatableComponent(!this.description.isEmpty() ? this.description : this.getId().toString() + ".tag", new TextComponent(s).withStyle(ChatFormatting.AQUA), this.amount);
        }
        return new TranslatableComponent(!this.description.isEmpty() ? this.description : this.getId().toString(), new TranslatableComponent(Util.makeDescriptionId("entity", new ResourceLocation(s))).withStyle(ChatFormatting.AQUA), this.amount);
    }

    public boolean check(ServerPlayer player, Entity entity) {
        return (this.playerPredicate == null || this.playerPredicate.matches(player, player))
                && this.predicate.matches(player, entity);
    }

    @Nullable
    @Override
    public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
        return progress.progressComponent(player, KillTracker.KEY, id);
    }
}
