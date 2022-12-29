package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleQuests {

    public static final String MODID = "simplequests";

    public static final Logger logger = LogManager.getLogger("simplequests");

    private static LoaderHandler handler;

    public static boolean ftbRanks;
    public static boolean permissionAPI;

    public static void updateLoaderImpl(LoaderHandler impl) {
        handler = impl;
    }

    public static LoaderHandler getHandler() {
        return handler;
    }

    public static void onInteractEntity(ServerPlayer player, Entity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND)
            PlayerData.get(player).onInteractWith(entity);
    }
}
