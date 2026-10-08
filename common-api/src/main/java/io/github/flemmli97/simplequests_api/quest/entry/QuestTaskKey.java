package io.github.flemmli97.simplequests_api.quest.entry;

import net.minecraft.resources.ResourceLocation;

/**
 * A custom Resourcelocation class to ensure generics
 */
public record QuestTaskKey<E extends QuestTask<? extends ResolvedQuestTask>>(ResourceLocation id) {

    public QuestTaskKey(String id) {
        this(ResourceLocation.parse(id));
    }

    public QuestTaskKey(String namespace, String path) {
        this(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QuestTaskKey<?> key)
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
