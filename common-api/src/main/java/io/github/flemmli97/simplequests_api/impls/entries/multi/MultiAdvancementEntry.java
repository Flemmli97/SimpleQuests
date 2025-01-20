package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.AdvancementEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

public class MultiAdvancementEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiAdvancementEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_advancements"));
    public static final Codec<MultiAdvancementEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.nonEmptyList(ResourceLocation.CODEC, "advancements list can't be empty").fieldOf("advancements").forGetter(d -> d.advancements),
                    Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, MultiAdvancementEntry::new));

    private final List<ResourceLocation> advancements;
    private final boolean reset;
    private final EntityPredicate playerPredicate;

    public MultiAdvancementEntry(List<ResourceLocation> advancements, boolean reset, String description, Optional<EntityPredicate> player) {
        super(description);
        this.advancements = advancements;
        this.reset = reset;
        this.playerPredicate = player.orElse(null);
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        return new AdvancementEntry(this.advancements.get(ctx.getRandom().nextInt(this.advancements.size())), this.reset, this.playerPredicate);
    }
}
