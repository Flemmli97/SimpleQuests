package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public record AdvancementEntry(ResourceLocation advancement, boolean reset,
                               EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<AdvancementEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "advancement"));
    public static final Codec<AdvancementEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(ResourceLocation.CODEC.fieldOf("advancement").forGetter(d -> d.advancement),
                    Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (advancement, reset, pred) -> new AdvancementEntry(advancement, reset, pred.orElse(null))));

    @Override
    public boolean submit(ServerPlayer player) {
        if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
            return false;
        Advancement adv = player.getServer().getAdvancements().getAdvancement(this.advancement);
        boolean ret = adv != null && (player.getAdvancements().getOrStartProgress(adv).isDone());
        if (ret && this.reset) {
            AdvancementProgress prog = player.getAdvancements().getOrStartProgress(adv);
            prog.getCompletedCriteria().forEach(s -> player.getAdvancements().revoke(adv, s));
        }
        return ret;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(this.advancement());
        Component adv;
        if (advancement == null)
            adv = new TranslatableComponent("simplequests.missing.advancement", this.advancement());
        else
            adv = advancement.getChatComponent();
        return new TranslatableComponent(this.getId().toString(), adv);
    }
}
