package io.github.flemmli97.simplequests.forge;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.forge.client.ForgeClientHandler;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import io.github.flemmli97.simplequests.quest.types.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoaderImpl implements LoaderHandler {

    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public LootNumberProviderType getQuestContextProvider() {
        return SimpleQuestForge.CONTEXT_MULTIPLIER.get();
    }

    @Override
    public boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd) {
        if (SimpleQuests.FTB_RANKS && src.getEntity() instanceof ServerPlayer player) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(src.hasPermission(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel));
        }
        return src.hasPermission(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.FTB_RANKS) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(player.hasPermissions(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel));
        }
        return player.hasPermissions(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
    }

    private static final int WRAP_AMOUNT = 4;

    @Override
    public List<MutableComponent> wrapForGui(ServerPlayer player, QuestEntryImpls.ItemEntry entry) {
        if (!entry.description().isEmpty())
            return List.of(Component.translatable(entry.description()));
        //Forge clients do it already
        List<MutableComponent> all = QuestEntryImpls.ItemEntry.itemComponents(entry.predicate());
        if (all.size() < WRAP_AMOUNT || player.connection.getConnectionType().isNeoForge())
            return List.of(entry.translation(player));
        List<MutableComponent> list = new ArrayList<>();
        MutableComponent items = null;
        int i = 0;
        for (MutableComponent comp : all) {
            if (items == null) {
                if (list.isEmpty())
                    items = Component.literal("[").append(comp);
                else
                    items = comp;
            } else
                items.append(Component.literal(", ")).append(comp);
            i++;
            if ((list.isEmpty() && i >= WRAP_AMOUNT - 1) || i >= WRAP_AMOUNT) {
                if (list.isEmpty()) {
                    list.add(Component.translatable(entry.getId().toString() + ".multi", items.withStyle(ChatFormatting.AQUA), entry.amount()));
                } else
                    list.add(items.withStyle(ChatFormatting.AQUA));
                i = 0;
                items = null;
            }
        }
        list.getLast().append(Component.literal("]"));
        return list;
    }

    @Override
    public void registerQuestCompleteHandler(SimpleQuestAPI.OnQuestComplete handler) {
        Consumer<QuestCompleteEvent> cons = event -> {
            if (handler.onComplete(event.player, event.trigger, event.quest, event.progress))
                event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cons);
    }

    @Override
    public boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        return !NeoForge.EVENT_BUS.post(new QuestCompleteEvent(player, trigger, quest, progress)).isCanceled();
    }

    @Override
    public void sendToServer(CustomPacketPayload packet) {
        ForgeClientHandler.sendNotifyPkt();
    }
}
