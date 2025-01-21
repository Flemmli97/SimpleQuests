package io.github.flemmli97.simplequests_api.forge;

import io.github.flemmli97.simplequests_api.CommonEvents;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(value = SimpleQuestsAPI.MODID)
public class SimpleQuestAPIForge {

    public SimpleQuestAPIForge() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "*", (s1, s2) -> true));
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(GlobalLootModifierSerializer.class, SimpleQuestAPIForge::registry);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestAPIForge::addReload);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestAPIForge::kill);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestAPIForge::interactSpecific);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestAPIForge::interactBlock);
        MinecraftForge.EVENT_BUS.addListener(SimpleQuestAPIForge::breakBlock);

        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
    }

    public static void registry(RegistryEvent.Register<GlobalLootModifierSerializer<?>> event) {
        QuestNumberProvider.init();
    }

    public static void addReload(AddReloadListenerEvent event) {
        event.addListener(QuestsManager.instance());
    }

    public static void kill(LivingDeathEvent event) {
        CommonEvents.onDeath(event.getEntityLiving());
    }

    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CommonEvents.onInteractEntity(player, event.getTarget(), event.getHand());
    }

    public static void interactBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getUseBlock() != Event.Result.DENY)
            CommonEvents.onBlockInteract(player, event.getPos(), true);
    }

    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player)
            CommonEvents.onBlockInteract(player, event.getPos(), false);
    }
}
