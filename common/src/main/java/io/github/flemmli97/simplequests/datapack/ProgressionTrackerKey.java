package io.github.flemmli97.simplequests.datapack;

import io.github.flemmli97.simplequests.api.QuestEntry;
import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 */
public record ProgressionTrackerKey<T, E extends QuestEntry>(ResourceLocation id) {

    public ProgressionTrackerKey(String id) {
        this(new ResourceLocation(id));
    }

    public ProgressionTrackerKey(String namespace, String path) {
        this(new ResourceLocation(namespace, path));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProgressionTrackerKey<?, ?> key)
            return this.id.equals(key.id);
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
