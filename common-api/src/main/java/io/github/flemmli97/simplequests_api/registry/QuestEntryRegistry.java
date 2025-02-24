package io.github.flemmli97.simplequests_api.registry;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.tasks.AdvancementTask;
import io.github.flemmli97.simplequests_api.impls.tasks.BlockInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.CraftingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.EntityInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.FishingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.ItemTask;
import io.github.flemmli97.simplequests_api.impls.tasks.KillTask;
import io.github.flemmli97.simplequests_api.impls.tasks.PredicateTask;
import io.github.flemmli97.simplequests_api.impls.tasks.XPTask;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for quest entries. The actual content for a quest
 */
public class QuestEntryRegistry {

    private static final Map<ResourceLocation, TaskCodec<ResolvedQuestTask>> MAP = new HashMap<>();
    public static final Codec<ResolvedQuestTask> RESOLVED_QUEST_ENTRY_CODEC = ResourceLocation.CODEC
            .dispatch("id", e -> e.getId().id(), id -> MAP.get(id).taskCodec());
    public static final Codec<QuestTask<?>> ENTRY_CODEC = ResourceLocation.CODEC
            .dispatch("id", e -> e.getId().id(), id -> MAP.get(id).taskHolderCodec());

    public static void register() {
        registerSerializer(ItemTask.ID, ItemTask.CODEC, ItemTask.ItemTaskResolved.CODEC);
        registerSerializer(KillTask.ID, KillTask.CODEC, KillTask.KillTaskResolved.CODEC);
        registerSerializer(XPTask.ID, XPTask.CODEC, XPTask.XPTaskResolved.CODEC);
        registerSerializer(AdvancementTask.ID, AdvancementTask.CODEC, AdvancementTask.AdvancementTaskResolved.CODEC);
        registerSerializer(PredicateTask.ID, PredicateTask.CODEC, PredicateTask.PredicateTaskResolved.CODEC);
        registerSerializer(EntityInteractTask.ID, EntityInteractTask.CODEC, EntityInteractTask.EntityInteractTaskResolved.CODEC);
        registerSerializer(BlockInteractTask.ID, BlockInteractTask.CODEC, BlockInteractTask.BlockInteractTaskResolved.CODEC);
        registerSerializer(CraftingTask.ID, CraftingTask.CODEC, CraftingTask.CraftingTaskResolved.CODEC);
        registerSerializer(FishingTask.ID, FishingTask.CODEC, FishingTask.FishingTaskResolved.CODEC);
    }

    /**
     * Register a deserializer for a {@link ResolvedQuestTask}
     */
    @SuppressWarnings("unchecked")
    public static synchronized <R extends ResolvedQuestTask, T extends QuestTask<R>> void registerSerializer(QuestEntryKey<T> id, MapCodec<T> deserializer, MapCodec<R> entrySerializer) {
        if (MAP.containsKey(id.id()))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id.id(), (TaskCodec<ResolvedQuestTask>) new TaskCodec<>(deserializer, entrySerializer));
    }

    public static QuestTask<?> deserialize(ResourceLocation res, JsonObject obj) {
        TaskCodec<ResolvedQuestTask> d = MAP.get(res);
        // Legacy
        if (d == null && res.getNamespace().equals("simplequests"))
            d = MAP.get(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, res.getPath()));
        if (d != null)
            return d.taskHolderCodec().codec().parse(JsonOps.INSTANCE, obj).getOrThrow(e -> {
                SimpleQuestsAPI.LOGGER.error("Couldn't deserialize QuestEntry from json {}", e);
                return new IllegalStateException(e);
            });
        throw new IllegalStateException("Missing entry for key " + res);
    }

    public record TaskCodec<T extends ResolvedQuestTask>(MapCodec<? extends QuestTask<T>> taskHolderCodec,
                                                         MapCodec<T> taskCodec) {
    }
}
