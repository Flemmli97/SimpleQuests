package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public interface ProgressionTracker<T, E extends QuestEntry> {

    boolean isApplicable(T value);

    boolean apply(T value);

    E questEntry();

    MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress);

    Tag save();

    void load(Tag tag);
}
