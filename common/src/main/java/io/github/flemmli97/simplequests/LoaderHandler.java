package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface LoaderHandler {

    LoaderHandler INSTANCE = SimpleQuestsAPI.getPlatformInstance(LoaderHandler.class,
            "io.github.flemmli97.simplequests.fabric.LoaderImpl",
            "io.github.flemmli97.simplequests.forge.LoaderImpl");

    Path getConfigPath();

    default boolean hasPerm(CommandSourceStack src, String perm) {
        return this.hasPerm(src, perm, false);
    }

    boolean hasPerm(CommandSourceStack src, String perm, boolean adminCmd);

    boolean hasPerm(ServerPlayer src, String perm, boolean adminCmd);
}
