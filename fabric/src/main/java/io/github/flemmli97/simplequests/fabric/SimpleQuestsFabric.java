package io.github.flemmli97.simplequests.fabric;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class SimpleQuestsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SimpleQuests.updateLoaderImpl(new LoaderImpl());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Reloader());
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> QuestCommand.register(dispatcher)));
        QuestEntryRegistry.register();
        ConfigHandler.init();
        SimpleQuests.permissionAPI = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        SimpleQuests.ftbRanks = FabricLoader.getInstance().isModLoaded("ftbranks");
    }

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity.getKillCredit() instanceof ServerPlayer player) {
            PlayerData.get(player).onKill(entity);
        }
    }
}
