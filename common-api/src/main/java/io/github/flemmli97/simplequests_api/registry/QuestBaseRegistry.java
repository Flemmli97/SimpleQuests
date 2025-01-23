package io.github.flemmli97.simplequests_api.registry;

import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuest;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.impls.quests.SequentialQuest;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for different types of quests
 */
public class QuestBaseRegistry {

    private static final Map<ResourceLocation, QuestReader> MAP = new HashMap<>();

    public static void register() {
        registerSerializer(Quest.ID, Quest::of);
        registerSerializer(CompositeQuest.ID, CompositeQuest::of);
        registerSerializer(SequentialQuest.ID, SequentialQuest::of);
    }

    /**
     * Register a deserializer for a {@link QuestBase}
     */
    public static synchronized void registerSerializer(ResourceLocation id, QuestReader deserializer) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id, deserializer);
    }

    public static QuestBase deserializeFull(JsonObject obj) {
        return deserialize(new ResourceLocation(obj.get(QuestBase.TYPE_ID).getAsString()), new ResourceLocation(obj.get("id").getAsString()),
                QuestsManager.instance().getQuestCategory(new ResourceLocation(obj.get("category").getAsString())), obj);
    }

    public static QuestBase deserialize(ResourceLocation type, ResourceLocation res, QuestCategory category, JsonObject obj) {
        QuestReader d = MAP.get(type);
        // Legacy
        if (d == null && res.getNamespace().equals(SimpleQuestsAPI.MODID))
            d = MAP.get(new ResourceLocation("simplequests", res.getPath()));
        if (d != null)
            return d.fromJson(res, category, obj);
        throw new IllegalStateException("Missing entry for key " + type);
    }

    public interface QuestReader {

        QuestBase fromJson(ResourceLocation id, QuestCategory category, JsonObject obj);

    }
}
