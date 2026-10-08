package io.github.flemmli97.simplequests_api.quest;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/**
 * Simple impl for a {@link ResolvedQuest} pointing to a {@link QuestDefinition}.
 */
public class QuestDefinitionRef<T extends QuestDefinition> implements ResolvedQuest {

    private final T definition;

    public QuestDefinitionRef(T definition) {
        this.definition = definition;
    }

    @Override
    public T source() {
        return this.definition;
    }

    @Override
    public MutableComponent name(ServerPlayer player) {
        return this.definition.name(player);
    }

    @Override
    public List<MutableComponent> description(ServerPlayer player) {
        return this.definition.description(player);
    }

    @Override
    public Map<String, ResolvedQuestTask> resolveTasks(QuestProgress progress) {
        return Map.of();
    }

    @Override
    public Tag save(HolderLookup.Provider lookup) {
        return ByteTag.ZERO;
    }
}
