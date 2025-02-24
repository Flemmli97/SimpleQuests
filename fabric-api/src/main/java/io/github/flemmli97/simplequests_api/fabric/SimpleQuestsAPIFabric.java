package io.github.flemmli97.simplequests_api.fabric;

import io.github.flemmli97.simplequests_api.CommonEvents;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.quest.OnQuestComplete;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SimpleQuestsAPIFabric implements ModInitializer {

    public static LootNumberProviderType CONTEXT_MULTIPLIER;
    public static final Event<OnQuestComplete> QUEST_COMPLETE = EventFactory.createArrayBacked(OnQuestComplete.class,
            listener -> (serverPlayer, trigger, quest, progress) -> {
                for (OnQuestComplete event : listener) {
                    if (!event.onComplete(serverPlayer, trigger, quest, progress))
                        return false;
                }
                return true;
            });

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return QuestsManager.INSTANCE.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
            }

            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "reloader");
            }
        });
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer)
                CommonEvents.onInteractEntity(serverPlayer, entity, hand);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register(((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer)
                CommonEvents.onBlockInteract(serverPlayer, hitResult.getBlockPos(), true);
            return InteractionResult.PASS;
        }));
        PlayerBlockBreakEvents.BEFORE.register(((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayer serverPlayer)
                CommonEvents.onBlockInteract(serverPlayer, pos, false);
            return true;
        }));
        CONTEXT_MULTIPLIER = Registry.register(BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE, ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "context_multiplier"), new LootNumberProviderType(QuestNumberProvider.ContextMultiplierNumberProvider.CODEC));
    }
}
