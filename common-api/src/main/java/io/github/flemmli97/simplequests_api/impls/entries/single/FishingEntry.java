package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FishingEntry(ItemPredicate item, EntityPredicate playerPredicate, int amount,
                           String description, String itemDescription,
                           String entityDescription) implements QuestEntry {

    public static final QuestEntryKey<FishingEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "fishing"));
    public static final Codec<FishingEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    Codec.STRING.optionalFieldOf("itemDescription").forGetter(d -> d.itemDescription.isEmpty() ? Optional.empty() : Optional.ofNullable(d.itemDescription)),
                    Codec.STRING.optionalFieldOf("entityDescription").forGetter(d -> d.entityDescription.isEmpty() ? Optional.empty() : Optional.ofNullable(d.entityDescription)),

                    JsonCodecs.ITEM_PREDICATE_CODEC.fieldOf("item").forGetter(d -> d.item),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> d.playerPredicate == EntityPredicate.ANY ? Optional.empty() : Optional.ofNullable(d.playerPredicate)),
                    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount)
            ).apply(instance, (desc, heldDesc, entityDesc, item, pred, amount) -> new FishingEntry(item, pred.orElse(EntityPredicate.ANY), amount, desc, heldDesc.orElse(""), entityDesc.orElse(""))));

    public FishingEntry(ItemPredicate item, EntityPredicate playerPredicate, int amount, String description) {
        this(item, playerPredicate, amount, description, "", "");
    }

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
        return new TranslatableComponent(this.description, new TranslatableComponent(this.itemDescription), new TranslatableComponent(this.entityDescription), this.amount);
    }

    @Nullable
    @Override
    public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
        return progress.progressComponent(player, FishingTracker.KEY, id);
    }

    public boolean check(ServerPlayer player, ItemStack stack) {
        return this.item.matches(stack) && (this.playerPredicate == null || this.playerPredicate.matches(player, player));
    }
}
