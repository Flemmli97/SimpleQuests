package io.github.flemmli97.simplequests.forge;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

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
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(!adminCmd || player.hasPermissions(2));
        }
        return !adminCmd || src.hasPermission(2);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.ftbRanks) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(!adminCmd || player.hasPermissions(2));
        }
        return !adminCmd || player.hasPermissions(2);
    }
}
