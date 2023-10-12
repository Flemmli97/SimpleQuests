package io.github.flemmli97.simplequests.fabric;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.fabric.client.FabricClientHandler;
import io.github.flemmli97.simplequests.network.SQPacket;
import io.github.flemmli97.simplequests.player.QuestProgress;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import io.github.flemmli97.simplequests.quest.types.Quest;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoaderImpl implements LoaderHandler {

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public ResourceLocation fromEntity(Entity entity) {
        return Registry.ENTITY_TYPE.getKey(entity.getType());
    }

    @Override
    public boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd) {
        if (SimpleQuests.PERMISSION_API) {
            return Permissions.check(src, perm, !adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
        }
        if (SimpleQuests.FTB_RANKS && src.getEntity() instanceof ServerPlayer player) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(src.hasPermission(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel));
        }
        return src.hasPermission(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.PERMISSION_API) {
            return Permissions.check(player, perm, !adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
        }
        if (SimpleQuests.FTB_RANKS) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(player.hasPermissions(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel));
        }
        return player.hasPermissions(!adminCmd ? ConfigHandler.CONFIG.mainPermLevel : ConfigHandler.CONFIG.opPermLevel);
    }

    //Choosing some reasonable size
    private static final int WRAP_AMOUNT = 4;

    @Override
    public List<MutableComponent> wrapForGui(ServerPlayer player, QuestEntryImpls.ItemEntry entry) {
        if (!entry.description().isEmpty())
            return List.of(new TranslatableComponent(entry.description()));
        List<MutableComponent> all = QuestEntryImpls.ItemEntry.itemComponents(entry.predicate());
        if (all.size() < WRAP_AMOUNT)
            return List.of(entry.translation(player));
        List<MutableComponent> list = new ArrayList<>();
        MutableComponent items = null;
        int i = 0;
        for (MutableComponent comp : all) {
            if (items == null) {
                if (list.size() == 0)
                    items = new TextComponent("[").append(comp);
                else
                    items = comp;
            } else
                items.append(new TextComponent(", ")).append(comp);
            i++;
            if ((list.size() == 0 && i >= WRAP_AMOUNT - 1) || i >= WRAP_AMOUNT) {
                if (list.size() == 0) {
                    list.add(new TranslatableComponent(ConfigHandler.LANG.get(player, entry.getId().toString() + ".multi"), items.withStyle(ChatFormatting.AQUA), entry.amount()));
                } else
                    list.add(items.withStyle(ChatFormatting.AQUA));
                i = 0;
                items = null;
            }
        }
        list.get(list.size() - 1).append(new TextComponent("]"));
        return list;
    }

    @Override
    public void registerQuestCompleteHandler(SimpleQuestAPI.OnQuestComplete handler) {
        SimpleQuestsFabric.QUEST_COMPLETE.register(handler);
    }

    @Override
    public boolean onQuestComplete(ServerPlayer player, String trigger, Quest quest, QuestProgress progress) {
        return SimpleQuestsFabric.QUEST_COMPLETE.invoker().onComplete(player, trigger, quest, progress);
    }

    @Override
    public void sendToServer(SQPacket packet) {
        FabricClientHandler.sendToServer(packet);
    }
}
