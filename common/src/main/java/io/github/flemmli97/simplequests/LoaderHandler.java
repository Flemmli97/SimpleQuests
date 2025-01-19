package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.network.SQPacket;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

public interface LoaderHandler {

    Path getConfigPath();

    ResourceLocation fromEntity(Entity entity);

    default boolean hasPerm(CommandSourceStack src, String perm) {
        return this.hasPerm(src, perm, false);
    }

    boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd);

    boolean hasPerm(ServerPlayer src, String perm, boolean adminCmd);

    void registerQuestCompleteHandler(SimpleQuestAPI.OnQuestComplete handler);

    boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress);

    void sendToServer(SQPacket packet);
}
