package io.github.flemmli97.simplequests_api.fabric;

import io.github.flemmli97.simplequests_api.CommonEvents;
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
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;

public class SimpleQuestsAPIFabric implements ModInitializer {

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
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Reloader());
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
        QuestNumberProvider.init();
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
    }
}
