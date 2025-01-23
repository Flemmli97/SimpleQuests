package io.github.flemmli97.simplequests_api.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiAdvancementEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiBlockInteractEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiCraftingEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiEntityInteractEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiFishingEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiItemEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiKillEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiLocationEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.MultiPositionEntry;
import io.github.flemmli97.simplequests_api.impls.entries.multi.XPRangeEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.AdvancementEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.BlockInteractEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.CraftingEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.EntityInteractEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.FishingEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.ItemEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.KillEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.LocationEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.PositionEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.XPEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for quest entries. The actual content for a quest
 */
public class QuestEntryRegistry {

    private static final Map<ResourceLocation, Codec<QuestEntry>> MAP = new HashMap<>();
    public static final Codec<QuestEntry> CODEC = ResourceLocation.CODEC
            .dispatch("id", e -> e.getId().id(), MAP::get);

    public static void register() {
        registerSerializer(ItemEntry.ID, ItemEntry.CODEC);
        registerSerializer(KillEntry.ID, KillEntry.CODEC);
        registerSerializer(XPEntry.ID, XPEntry.CODEC);
        registerSerializer(AdvancementEntry.ID, AdvancementEntry.CODEC);
        registerSerializer(PositionEntry.ID, PositionEntry.CODEC);
        registerSerializer(LocationEntry.ID, LocationEntry.CODEC);
        registerSerializer(EntityInteractEntry.ID, EntityInteractEntry.CODEC);
        registerSerializer(BlockInteractEntry.ID, BlockInteractEntry.CODEC);
        registerSerializer(CraftingEntry.ID, CraftingEntry.CODEC);
        registerSerializer(FishingEntry.ID, FishingEntry.CODEC);

        registerSerializer(MultiItemEntry.ID, MultiItemEntry.CODEC);
        registerSerializer(MultiKillEntry.ID, MultiKillEntry.CODEC);
        registerSerializer(XPRangeEntry.ID, XPRangeEntry.CODEC);
        registerSerializer(MultiAdvancementEntry.ID, MultiAdvancementEntry.CODEC);
        registerSerializer(MultiPositionEntry.ID, MultiPositionEntry.CODEC);
        registerSerializer(MultiLocationEntry.ID, MultiLocationEntry.CODEC);
        registerSerializer(MultiEntityInteractEntry.ID, MultiEntityInteractEntry.CODEC);
        registerSerializer(MultiBlockInteractEntry.ID, MultiBlockInteractEntry.CODEC);
        registerSerializer(MultiCraftingEntry.ID, MultiCraftingEntry.CODEC);
        registerSerializer(MultiFishingEntry.ID, MultiFishingEntry.CODEC);
    }

    /**
     * Register a deserializer for a {@link QuestEntry}
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T extends QuestEntry> void registerSerializer(QuestEntryKey<T> id, Codec<T> deserializer) {
        if (MAP.containsKey(id.id()))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id.id(), (Codec<QuestEntry>) deserializer);
    }

    public static QuestEntry deserialize(ResourceLocation res, JsonObject obj) {
        Codec<QuestEntry> d = MAP.get(res);
        // Legacy
        if (d == null && res.getNamespace().equals(SimpleQuestsAPI.MODID))
            d = MAP.get(new ResourceLocation("simplequests", res.getPath()));
        if (d != null)
            return d.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> SimpleQuestsAPI.LOGGER.error("Couldn't deserialize QuestEntry from json {}", e));
        throw new IllegalStateException("Missing entry for key " + res);
    }
}
