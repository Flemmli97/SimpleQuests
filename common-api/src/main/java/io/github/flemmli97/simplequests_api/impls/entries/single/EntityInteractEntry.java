package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
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
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Quest entry to check if a player interacts with an entity.
 *
 * @param description Parsing the predicates is way too complicated. Its easier instead to have the datapack maker provide a description instead
 */
public record EntityInteractEntry(ItemPredicate heldItem, EntityPredicate entityPredicate, int amount,
                                  boolean consume, String description, String heldDescription,
                                  String entityDescription,
                                  EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<EntityInteractEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "entity_interact"));
    public static final Codec<EntityInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    Codec.STRING.optionalFieldOf("held_description").forGetter(d -> d.heldDescription.isEmpty() ? Optional.empty() : Optional.of(d.heldDescription)),
                    Codec.STRING.optionalFieldOf("entity_description").forGetter(d -> d.entityDescription.isEmpty() ? Optional.empty() : Optional.of(d.entityDescription)),

                    JsonCodecs.ITEM_PREDICATE_CODEC.optionalFieldOf("item").forGetter(d -> d.heldItem == ItemPredicate.ANY ? Optional.empty() : Optional.of(d.heldItem)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("predicate").forGetter(d -> d.entityPredicate == EntityPredicate.ANY ? Optional.empty() : Optional.of(d.entityPredicate)),
                    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (desc, heldDesc, entityDesc, item, pred, amount, consume, player) ->
                    new EntityInteractEntry(item.orElse(null), pred.orElse(null), amount, consume, desc, heldDesc.orElse(""), entityDesc.orElse(""), player.orElse(null))));

    public EntityInteractEntry(ItemPredicate heldItem, EntityPredicate entityPredicate, int amount, boolean consume, String description) {
        this(heldItem, entityPredicate, amount, consume, description, "", "", null);
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
        return new TranslatableComponent(this.description, new TranslatableComponent(this.heldDescription), new TranslatableComponent(this.entityDescription), this.amount);
    }

    @Nullable
    @Override
    public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
        return progress.progressComponent(player, EntityTracker.KEY, id);
    }

    public boolean check(ServerPlayer player, Entity entity) {
        if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
            return false;
        boolean b = (this.heldItem == null || this.heldItem.matches(player.getMainHandItem())) &&
                (this.entityPredicate == null || this.entityPredicate.matches(player, entity));
        if (b && this.consume && !player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }
        return b;
    }
}
