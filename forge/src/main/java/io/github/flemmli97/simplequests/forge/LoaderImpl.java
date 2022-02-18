package io.github.flemmli97.simplequests.forge;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkHooks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoaderImpl implements LoaderHandler {

    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public ResourceLocation fromEntity(Entity entity) {
        return entity.getType().getRegistryName();
    }

    @Override
    public boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd) {
        if (SimpleQuests.ftbRanks && src.getEntity() instanceof ServerPlayer player) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(src.hasPermission(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel));
        }
        return src.hasPermission(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.ftbRanks) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(player.hasPermissions(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel));
        }
        return player.hasPermissions(!adminCmd ? ConfigHandler.config.mainPermLevel : ConfigHandler.config.opPermLevel);
    }

    private static final int warpAmount = 4;

    @Override
    public List<MutableComponent> wrapForGui(ServerPlayer player, QuestEntryImpls.IngredientEntry entry) {
        if (entry.description != null)
            return List.of(entry.description);
        //Forge clients do it already
        if (entry.ingredient.getItems().length < warpAmount || !NetworkHooks.isVanillaConnection(player.connection.connection))
            return List.of(entry.translation(player.getServer()));
        List<MutableComponent> list = new ArrayList<>();
        MutableComponent items = null;
        int i = 0;
        for (ItemStack stack : entry.ingredient.getItems()) {
            if (items == null) {
                if (list.size() == 0)
                    items = new TextComponent("[").append(new TranslatableComponent(stack.getItem().getDescriptionId()));
                else
                    items = new TranslatableComponent(stack.getItem().getDescriptionId());
            } else
                items.append(new TextComponent(", ")).append(new TranslatableComponent(stack.getItem().getDescriptionId()));
            i++;
            if ((list.size() == 0 && i >= warpAmount - 1) || i >= warpAmount) {
                if (list.size() == 0) {
                    list.add(new TranslatableComponent(ConfigHandler.lang.get(entry.getId().toString() + ".multi"), items.withStyle(ChatFormatting.AQUA), entry.amount));
                } else
                    list.add(items.withStyle(ChatFormatting.AQUA));
                i = 0;
                items = null;
            }
        }
        list.get(list.size() - 1).append(new TextComponent("]"));
        return list;
    }
}
