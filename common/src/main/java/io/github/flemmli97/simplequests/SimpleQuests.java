package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class SimpleQuests {

    public static final String MODID = "simplequests";

    public static final Logger LOGGER = LogManager.getLogger("simplequests");

    private static LoaderHandler HANDLER;

    public static boolean FTB_RANKS;
    public static boolean PERMISSION_API;

    public static void updateLoaderImpl(LoaderHandler impl) {
        HANDLER = impl;
    }

    public static LoaderHandler getHandler() {
        return HANDLER;
    }

    public static void onInteractEntity(ServerPlayer player, Entity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND)
            PlayerData.get(player).onInteractWith(entity);
    }

    public static LootContext createContext(ServerPlayer player, Entity entity, @Nullable ResourceLocation quest) {
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .create(LootContextParamSets.ADVANCEMENT_ENTITY);
        return new LootContext.Builder(params).withOptionalRandomSeed(PlayerData.get(player).getRandomSeed(quest)).create(null);
    }
}
