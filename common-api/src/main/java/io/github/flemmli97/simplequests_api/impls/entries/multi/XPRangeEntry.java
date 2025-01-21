package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.XPEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Optional;

public class XPRangeEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<XPRangeEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_xp"));
    public static final Codec<XPRangeEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, XPRangeEntry::new));

    private final NumberProvider amount;
    private final EntityPredicate playerPredicate;

    public XPRangeEntry(NumberProvider amount, String description, Optional<EntityPredicate> player) {
        super(description);
        this.amount = amount;
        this.playerPredicate = player.orElse(null);
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        return new XPEntry(QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), this.playerPredicate);
    }
}
