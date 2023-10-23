package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.network.SQPacket;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import io.github.flemmli97.simplequests.quest.types.Quest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;
import java.util.List;

public interface LoaderHandler {

    Path getConfigPath();

    ResourceLocation fromEntity(Entity entity);

    default boolean hasPerm(CommandSourceStack src, String perm) {
        return this.hasPerm(src, perm, false);
    }

    boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd);

    boolean hasPerm(ServerPlayer src, String perm, boolean adminCmd);

    List<MutableComponent> wrapForGui(ServerPlayer player, QuestEntryImpls.ItemEntry entry);

    void registerQuestCompleteHandler(SimpleQuestAPI.OnQuestComplete handler);

    boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress);

    void sendToServer(SQPacket packet);
}
