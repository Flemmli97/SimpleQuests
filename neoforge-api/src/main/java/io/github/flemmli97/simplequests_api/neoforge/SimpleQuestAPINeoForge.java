package io.github.flemmli97.simplequests_api.neoforge;

import io.github.flemmli97.simplequests_api.CommonEvents;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(value = SimpleQuestsAPI.MODID)
public class SimpleQuestAPINeoForge {

    private static final DeferredRegister<LootNumberProviderType> NUMBER_PROVIDERS = DeferredRegister.create(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, SimpleQuestsAPI.MODID);
    public static final DeferredHolder<LootNumberProviderType, LootNumberProviderType> CONTEXT_MULTIPLIER = NUMBER_PROVIDERS.register("context_multiplier", () -> new LootNumberProviderType(QuestNumberProvider.ContextMultiplierNumberProvider.CODEC));

    public SimpleQuestAPINeoForge(IEventBus modBus) {
        NUMBER_PROVIDERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(SimpleQuestAPINeoForge::addReload);
        NeoForge.EVENT_BUS.addListener(SimpleQuestAPINeoForge::kill);
        NeoForge.EVENT_BUS.addListener(SimpleQuestAPINeoForge::interactSpecific);
        NeoForge.EVENT_BUS.addListener(SimpleQuestAPINeoForge::interactBlock);
        NeoForge.EVENT_BUS.addListener(SimpleQuestAPINeoForge::breakBlock);

        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
    }

    public static void addReload(AddReloadListenerEvent event) {
        event.addListener(QuestsManager.create(event.getServerResources().getRegistryLookup()));
    }

    public static void kill(LivingDeathEvent event) {
        CommonEvents.onDeath(event.getEntity());
    }

    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player)
            CommonEvents.onInteractEntity(player, event.getTarget(), event.getHand());
    }

    public static void interactBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getUseBlock() != TriState.FALSE)
            CommonEvents.onBlockInteract(serverPlayer, event.getPos(), true);
    }

    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player)
            CommonEvents.onBlockInteract(player, event.getPos(), false);
    }
}
