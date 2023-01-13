package io.github.flemmli97.simplequests.datapack;

import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class QuestEntryRegistry {

    private static final Map<ResourceLocation, Deserializer<?>> MAP = new HashMap<>();

    public static void register() {
        registerSerializer(QuestEntryImpls.ItemEntry.id, QuestEntryImpls.ItemEntry::fromJson);
        registerSerializer(QuestEntryImpls.XPEntry.id, QuestEntryImpls.XPEntry::fromJson);
        registerSerializer(QuestEntryImpls.AdvancementEntry.id, QuestEntryImpls.AdvancementEntry::fromJson);
        registerSerializer(QuestEntryImpls.KillEntry.id, QuestEntryImpls.KillEntry::fromJson);
        registerSerializer(QuestEntryImpls.PositionEntry.id, QuestEntryImpls.PositionEntry::fromJson);
        registerSerializer(QuestEntryImpls.LocationEntry.id, QuestEntryImpls.LocationEntry::fromJson);
        registerSerializer(QuestEntryImpls.EntityInteractEntry.id, QuestEntryImpls.EntityInteractEntry::fromJson);
        registerSerializer(QuestEntryImpls.BlockInteractEntry.id, QuestEntryImpls.BlockInteractEntry::fromJson);
    }

    /**
     * Register a deserializer for a {@link QuestEntry}
     */
    public static synchronized void registerSerializer(ResourceLocation id, Deserializer<?> deserializer) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id, deserializer);
    }

    public static QuestEntry deserialize(ResourceLocation res, JsonObject obj) {
        Deserializer<?> d = MAP.get(res);
        if (d != null)
            return d.deserialize(obj);
        throw new IllegalStateException("Missing entry for key " + res);
    }

    public interface Deserializer<T extends QuestEntry> {
        T deserialize(JsonObject obj);
    }
}
