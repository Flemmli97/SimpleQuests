package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Quest entry to check if a player matches a given location.
 *
 * @param location    The LocationPredicate to check
 * @param description Parsing a location predicate is way too complicated. Its easier instead to have the datapack maker provide a description instead
 */
public record LocationEntry(LocationPredicate location, String description,
                            EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<LocationEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "location"));
    public static final Codec<LocationEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.LOCATION_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.location),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (pred, desc, player) -> new LocationEntry(pred, desc, player.orElse(null))));

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
        return new TranslatableComponent(this.description);
    }

    @Override
    public Predicate<PlayerQuestData> tickable() {
        return data -> {
            ServerPlayer player = data.getPlayer();
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            return player.tickCount % 20 == 0 && this.location.matches(player.getLevel(), player.getX(), player.getY(), player.getZ());
        };
    }
}
