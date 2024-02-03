package io.github.flemmli97.simplequests.datapack;

import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.player.ProgressionTracker;
import io.github.flemmli97.simplequests.player.ProgressionTrackerImpl;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class ProgressionTrackerRegistry {

    private static final Map<ProgressionTrackerKey<?, ?>, TrackerFactory<?, ?>> MAP = new HashMap<>();

    public static void register() {
        registerSerializer(ProgressionTrackerImpl.FishingTracker.KEY, ProgressionTrackerImpl.FishingTracker::new);
        registerSerializer(ProgressionTrackerImpl.KillTracker.KEY, ProgressionTrackerImpl.KillTracker::new);
        registerSerializer(ProgressionTrackerImpl.CraftingTracker.KEY, ProgressionTrackerImpl.CraftingTracker::new);
        registerSerializer(ProgressionTrackerImpl.BlockTracker.KEY, ProgressionTrackerImpl.BlockTracker::new);
        registerSerializer(ProgressionTrackerImpl.EntityTracker.KEY, ProgressionTrackerImpl.EntityTracker::new);
    }

    /**
     * Register a deserializer for a {@link QuestBase}
     */
    public static synchronized <T, E extends QuestEntry> void registerSerializer(ProgressionTrackerKey<T, E> id, TrackerFactory<T, E> create) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Tracker for " + id + " already registered");
        MAP.put(id, create);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends QuestEntry> ProgressionTracker<T, E> deserialize(ProgressionTrackerKey<T, E> key, E entry, Tag tag) {
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
    public static <T, E extends QuestEntry> ProgressionTracker<T, E> create(ProgressionTrackerKey<T, E> key, E entry) {
        TrackerFactory<?, ?> d = MAP.get(key);
        if (d != null) {
            TrackerFactory<T, E> tracker = (TrackerFactory<T, E>) d;
            return tracker.create(entry);
        }
        throw new IllegalStateException("Missing entry for key " + key);
    }

    public interface TrackerFactory<T, E extends QuestEntry> {
        ProgressionTracker<T, E> create(E entry);

    }
}
