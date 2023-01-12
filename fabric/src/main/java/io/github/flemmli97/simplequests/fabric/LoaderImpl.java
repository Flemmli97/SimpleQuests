package io.github.flemmli97.simplequests.fabric;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        if (SimpleQuests.permissionAPI) {
            return Permissions.check(src, perm, !adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
        }
        if (SimpleQuests.ftbRanks && src.getEntity() instanceof ServerPlayer player) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(src.hasPermission(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel));
        }
        return src.hasPermission(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.permissionAPI) {
            return Permissions.check(player, perm, !adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
        }
        if (SimpleQuests.ftbRanks) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(player.hasPermissions(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel));
        }
        return player.hasPermissions(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
    }

    //Choosing some reasonable size
    private static final int warpAmount = 4;

    @Override
    public List<MutableComponent> wrapForGui(ServerPlayer player, QuestEntryImpls.ItemEntry entry) {
        if (entry.description != null)
            return List.of(entry.description);
        List<MutableComponent> all = QuestEntryImpls.ItemEntry.itemComponents(entry.predicate);
        if (all.size() < warpAmount)
            return List.of(entry.translation(player.getServer()));
        List<MutableComponent> list = new ArrayList<>();
        MutableComponent items = null;
        int i = 0;
        for (MutableComponent comp : all) {
            if (items == null) {
                if (list.size() == 0)
                    items = Component.literal("[").append(comp);
                else
                    items = comp;
            } else
                items.append(Component.literal(", ")).append(comp);
            i++;
            if ((list.size() == 0 && i >= warpAmount - 1) || i >= warpAmount) {
                if (list.size() == 0) {
                    list.add(Component.translatable(ConfigHandler.lang.get(entry.getId().toString() + ".multi"), items.withStyle(ChatFormatting.AQUA), entry.amount));
                } else
                    list.add(items.withStyle(ChatFormatting.AQUA));
                i = 0;
                items = null;
            }
        }
        list.get(list.size() - 1).append(Component.literal("]"));
        return list;
    }
}
