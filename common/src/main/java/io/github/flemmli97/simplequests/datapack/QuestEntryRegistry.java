package io.github.flemmli97.simplequests.datapack;

import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class QuestEntryRegistry {

    private static final Map<ResourceLocation, Deserializer<?>> map = new HashMap<>();

    public static void register() {
        map.put(QuestEntryImpls.IngredientEntry.id, QuestEntryImpls.IngredientEntry::fromJson);
        map.put(QuestEntryImpls.XPEntry.id, QuestEntryImpls.XPEntry::fromJson);
        map.put(QuestEntryImpls.AdvancementEntry.id, QuestEntryImpls.AdvancementEntry::fromJson);
        map.put(QuestEntryImpls.KillEntry.id, QuestEntryImpls.KillEntry::fromJson);
    }

    public static QuestEntry deserialize(ResourceLocation res, JsonObject obj) {
        Deserializer<?> d = map.get(res);
        if (d != null)
            return d.deserialize(obj);
        return null;
    }

    public interface Deserializer<T extends QuestEntry> {
        T deserialize(JsonObject obj);
    }
}
