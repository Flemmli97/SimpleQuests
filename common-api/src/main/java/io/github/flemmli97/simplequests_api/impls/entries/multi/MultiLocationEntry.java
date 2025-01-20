package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.LocationEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

public class MultiLocationEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiLocationEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_location"));
    public static final Codec<MultiLocationEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.descriptiveList(JsonCodecs.LOCATION_PREDICATE_CODEC, "location predicates can't be empty").fieldOf("locations").forGetter(d -> d.locations),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, MultiLocationEntry::new));

    private final List<Pair<LocationPredicate, String>> locations;
    private final EntityPredicate playerPredicate;

    public MultiLocationEntry(List<Pair<LocationPredicate, String>> locations, String description, Optional<EntityPredicate> player) {
        super(description);
        this.locations = locations;
        this.playerPredicate = player.orElse(null);
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Pair<LocationPredicate, String> val = this.locations.get(ctx.getRandom().nextInt(this.locations.size()));
        return new LocationEntry(val.getFirst(), val.getSecond(), this.playerPredicate);
    }
}
