package io.github.flemmli97.simplequests.datapack;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import io.github.flemmli97.simplequests.quest.QuestEntryMultiImpl;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class QuestEntryRegistry {

    private static final Map<ResourceLocation, Codec<QuestEntry>> MAP = new HashMap<>();
    public static final Codec<QuestEntry> CODEC = ResourceLocation.CODEC
            .dispatch("id", QuestEntry::getId, MAP::get);

    public static void register() {
        registerSerializer(QuestEntryImpls.ItemEntry.ID, QuestEntryImpls.ItemEntry.CODEC);
        registerSerializer(QuestEntryImpls.KillEntry.ID, QuestEntryImpls.KillEntry.CODEC);
        registerSerializer(QuestEntryImpls.XPEntry.ID, QuestEntryImpls.XPEntry.CODEC);
        registerSerializer(QuestEntryImpls.AdvancementEntry.ID, QuestEntryImpls.AdvancementEntry.CODEC);
        registerSerializer(QuestEntryImpls.PositionEntry.ID, QuestEntryImpls.PositionEntry.CODEC);
        registerSerializer(QuestEntryImpls.LocationEntry.ID, QuestEntryImpls.LocationEntry.CODEC);
        registerSerializer(QuestEntryImpls.EntityInteractEntry.ID, QuestEntryImpls.EntityInteractEntry.CODEC);
        registerSerializer(QuestEntryImpls.BlockInteractEntry.ID, QuestEntryImpls.BlockInteractEntry.CODEC);
        registerSerializer(QuestEntryImpls.CraftingEntry.ID, QuestEntryImpls.CraftingEntry.CODEC);
        registerSerializer(QuestEntryImpls.FishingEntry.ID, QuestEntryImpls.FishingEntry.CODEC);

        registerSerializer(QuestEntryMultiImpl.MultiItemEntry.ID, QuestEntryMultiImpl.MultiItemEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiKillEntry.ID, QuestEntryMultiImpl.MultiKillEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.XPRangeEntry.ID, QuestEntryMultiImpl.XPRangeEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiAdvancementEntry.ID, QuestEntryMultiImpl.MultiAdvancementEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiPositionEntry.ID, QuestEntryMultiImpl.MultiPositionEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiLocationEntry.ID, QuestEntryMultiImpl.MultiLocationEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiEntityInteractEntry.ID, QuestEntryMultiImpl.MultiEntityInteractEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiBlockInteractEntry.ID, QuestEntryMultiImpl.MultiBlockInteractEntry.CODEC);
        registerSerializer(QuestEntryMultiImpl.MultiFishingEntry.ID, QuestEntryMultiImpl.MultiFishingEntry.CODEC);
    }

    /**
     * Register a deserializer for a {@link QuestEntry}
     */
    @SuppressWarnings("unchecked")
    public static synchronized void registerSerializer(ResourceLocation id, Codec<? extends QuestEntry> deserializer) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id, (Codec<QuestEntry>) deserializer);
    }

    public static QuestEntry deserialize(ResourceLocation res, JsonObject obj) {
        Codec<QuestEntry> d = MAP.get(res);
        if (d != null)
            return d.parse(JsonOps.INSTANCE, obj).getOrThrow(false, e -> SimpleQuests.logger.error("Couldn't deserialize QuestEntry from json " + e));
        throw new IllegalStateException("Missing entry for key " + res);
    }
}
