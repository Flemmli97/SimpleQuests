package io.github.flemmli97.simplequests.neoforge;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.data.PlayerData;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(value = SimpleQuests.MODID)
public class SimpleQuestNeoForge {

    public SimpleQuestNeoForge() {
        NeoForge.EVENT_BUS.addListener(SimpleQuestNeoForge::command);
        ConfigHandler.init();
        SimpleQuests.FTB_RANKS = ModList.get().isLoaded("ftbranks");
        PlayerQuestDataRegistry.registerFetcher(ResourceLocation.fromNamespaceAndPath(SimpleQuests.MODID, "player_data"), PlayerData::get);
    }

    public static void command(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }
}
