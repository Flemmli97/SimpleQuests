package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
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
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Quest entry to check if a player matches a given location.
 */
public class LocationTask implements QuestTask<LocationTask.LocationTaskResolved> {

    public static final QuestEntryKey<LocationTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "location"));
    public static final Codec<LocationTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.nonEmptyList(DescriptiveValue.codecDesc(JsonCodecs.LOCATION_PREDICATE_CODEC), "location predicates can't be empty").fieldOf("locations").forGetter(d -> d.locations),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> QuestUtils.optStr(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (locs, desc, pred) -> new LocationTask(locs, desc.orElse(""), pred.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<LocationPredicate>> locations;
    private final EntityPredicate playerPredicate;

    public LocationTask(List<DescriptiveValue<LocationPredicate>> locations, String description, @Nullable EntityPredicate player) {
        if (locations.size() > 1 && description.isEmpty())
            throw new IllegalStateException("Description is required");
        this.description = description;
        this.locations = locations;
        this.playerPredicate = player;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.locations.size() == 1) {
            DescriptiveValue<LocationPredicate> loc = this.locations.get(0);
            return loc.getTranslation();
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<LocationTask> getId() {
        return ID;
    }

    @Override
    public LocationTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<LocationPredicate> val = this.locations.get(ctx.getRandom().nextInt(this.locations.size()));
        return new LocationTaskResolved(val, this.playerPredicate);
    }

    public record LocationTaskResolved(DescriptiveValue<LocationPredicate> location,
                                       EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<LocationTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(DescriptiveValue.codec(JsonCodecs.LOCATION_PREDICATE_CODEC).fieldOf("predicate").forGetter(d -> d.location),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (pred, player) -> new LocationTaskResolved(pred, player.orElse(null))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<LocationTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.location().getTranslation();
        }

        @Override
        public Predicate<PlayerQuestData> tickable() {
            return data -> {
                ServerPlayer player = data.getPlayer();
                if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                    return false;
                return player.tickCount % 20 == 0 && this.location.value().matches(player.getLevel(), player.getX(), player.getY(), player.getZ());
            };
        }
    }
}
