package io.github.flemmli97.simplequests_api.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuestDefinition;
import io.github.flemmli97.simplequests_api.impls.quests.SequentialQuestDefinition;
import io.github.flemmli97.simplequests_api.impls.quests.TasksQuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class QuestDefinitionRegistry {

    private static final Map<ResourceLocation, MapCodec<? extends QuestDefinition>> REGISTRY = new HashMap<>();

    public static final Codec<QuestDefinition> CODEC = ResourceLocation.CODEC.dispatch(QuestDefinition::typeId, QuestDefinitionRegistry::getCodec);

    public static void register() {
        register(TasksQuestDefinition.ID, TasksQuestDefinition.CODEC);
        register(CompositeQuestDefinition.ID, CompositeQuestDefinition.CODEC);
        register(SequentialQuestDefinition.ID, SequentialQuestDefinition.CODEC);
    }

    /**
     * Register a deserializer for a {@link QuestDefinition}
     * The context allows to conditionally serialize data
     */
    public static synchronized <T extends QuestDefinition> void register(ResourceLocation id, MapCodec<T> codec) {
        if (REGISTRY.containsKey(id))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        REGISTRY.put(id, codec);
    }

    private static MapCodec<? extends QuestDefinition> getCodec(ResourceLocation res) {
        MapCodec<? extends QuestDefinition> d = REGISTRY.get(res);
        if (d != null)
            return d;
        throw new IllegalStateException("Missing entry for key " + res);
    }
}
