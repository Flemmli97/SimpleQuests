package io.github.flemmli97.simplequests.fabric;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.data.PlayerData;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public class SimpleQuestsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated, selection) -> QuestCommand.register(dispatcher)));
        ConfigHandler.init();
        SimpleQuests.PERMISSION_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        SimpleQuests.FTB_RANKS = FabricLoader.getInstance().isModLoaded("ftbranks");
        PlayerQuestDataRegistry.registerFetcher(new ResourceLocation(SimpleQuests.MODID, "player_data"), PlayerData::get);
    }
}
