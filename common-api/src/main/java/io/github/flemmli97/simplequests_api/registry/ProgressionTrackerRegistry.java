package io.github.flemmli97.simplequests_api.registry;

import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.impls.progression.CraftingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
import io.github.flemmli97.simplequests_api.player.ProgressionTracker;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for progression trackers used in various entries that require e.g. number tracking
 */
public class ProgressionTrackerRegistry {

    private static final Map<ProgressionTrackerKey<?, ?>, TrackerFactory<?, ?>> MAP = new HashMap<>();
    private static final Map<ResourceLocation, ProgressionTrackerKey<?, ?>> KEYS = new HashMap<>();

    public static void register() {
        registerSerializer(FishingTracker.KEY, FishingTracker::new);
        registerSerializer(KillTracker.KEY, KillTracker::new);
        registerSerializer(CraftingTracker.KEY, CraftingTracker::new);
        registerSerializer(BlockTracker.KEY, BlockTracker::new);
        registerSerializer(EntityTracker.KEY, EntityTracker::new);
    }

    /**
     * Register a deserializer for a {@link QuestBase}
     */
    public static synchronized <T, E extends ResolvedQuestTask> void registerSerializer(ProgressionTrackerKey<T, E> id, TrackerFactory<T, E> create) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Tracker for " + id + " already registered");
        MAP.put(id, create);
        KEYS.put(id.id(), id);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends ResolvedQuestTask> ProgressionTracker<T, E> deserialize(ProgressionTrackerKey<T, E> key, E entry, Tag tag) {
        TrackerFactory<?, ?> d = MAP.get(key);
        if (d != null) {
            TrackerFactory<T, E> factory = (TrackerFactory<T, E>) d;
            ProgressionTracker<T, E> tracker = factory.create(entry);
            tracker.load(tag);
            return tracker;
        }
        throw new IllegalStateException("Missing entry for key " + key);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends ResolvedQuestTask> ProgressionTracker<T, E> create(ProgressionTrackerKey<T, E> key, E entry) {
        TrackerFactory<?, ?> d = MAP.get(key);
        if (d != null) {
            TrackerFactory<T, E> tracker = (TrackerFactory<T, E>) d;
            return tracker.create(entry);
        }
        throw new IllegalStateException("Missing entry for key " + key);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends ResolvedQuestTask> ProgressionTrackerKey<T, E> getKey(ResourceLocation id) {
        return (ProgressionTrackerKey<T, E>) KEYS.get(id);
    }

    public interface TrackerFactory<T, E extends ResolvedQuestTask> {
        ProgressionTracker<T, E> create(E entry);

    }
}
