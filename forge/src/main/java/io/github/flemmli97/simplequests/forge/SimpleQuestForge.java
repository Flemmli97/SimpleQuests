package io.github.flemmli97.simplequests.forge;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(value = SimpleQuests.MODID)
public class SimpleQuestForge {

    public SimpleQuestForge() {
        SimpleQuests.updateLoaderImpl(new LoaderImpl());
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::addReload);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::command);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestForge::kill);
        QuestEntryRegistry.register();
        ConfigHandler.init();
        SimpleQuests.ftbRanks = ModList.get().isLoaded("ftbranks");
    }

    public static void addReload(AddReloadListenerEvent event) {
        event.addListener(QuestsManager.instance());
    }

    public static void command(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }

    public static void kill(LivingDeathEvent event) {
        if (event.getEntityLiving().getKillCredit() instanceof ServerPlayer player) {
            PlayerData.get(player).onKill(event.getEntityLiving());
        }
    }

}
