package io.github.flemmli97.simplequests.forge;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.data.PlayerData;
import io.github.flemmli97.simplequests_api.registry.PlayerQuestDataRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(value = SimpleQuests.MODID)
public class SimpleQuestForge {

    public SimpleQuestForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "*", (s1, s2) -> true));
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::command);
        ConfigHandler.init();
        SimpleQuests.FTB_RANKS = ModList.get().isLoaded("ftbranks");
        PlayerQuestDataRegistry.registerFetcher(new ResourceLocation(SimpleQuests.MODID, "player_data"), PlayerData::get);
    }

    public static void command(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }
}
