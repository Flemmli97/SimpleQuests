package io.github.flemmli97.simplequests.fabric;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import io.github.flemmli97.simplequests.LoaderHandler;
import io.github.flemmli97.simplequests.SimpleQuests;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

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
            if (adminCmd)
                return Permissions.check(src, perm, 2);
            return Permissions.check(src, perm, true);
        }
        if (SimpleQuests.ftbRanks && src.getEntity() instanceof ServerPlayer player) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(!adminCmd || player.hasPermissions(2));
        }
        return !adminCmd || src.hasPermission(2);
    }

    @Override
    public boolean hasPerm(ServerPlayer player, String perm, boolean adminCmd) {
        if (SimpleQuests.permissionAPI) {
            if (adminCmd)
                return Permissions.check(player, perm, 2);
            return Permissions.check(player, perm, true);
        }
        if (SimpleQuests.ftbRanks) {
            return FTBRanksAPI.getPermissionValue(player, perm).asBoolean().orElse(!adminCmd || player.hasPermissions(2));
        }
        return !adminCmd || player.hasPermissions(2);
    }
}
