package io.github.flemmli97.simplequests.quest.entry;

import io.github.flemmli97.simplequests.api.QuestEntry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

public abstract class MultiQuestEntryBase implements QuestEntry {

    protected final String description;

    public MultiQuestEntryBase(String description) {
        this.description = description;
    }

    @Override
    public boolean submit(ServerPlayer player) {
        return false;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        return new TranslatableComponent(this.description);
    }
}
