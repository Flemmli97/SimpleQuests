package io.github.flemmli97.simplequests.fabric;

import io.github.flemmli97.simplequests.QuestCommand;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestBaseRegistry;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class SimpleQuestsFabric implements ModInitializer {

    public static final Event<SimpleQuestAPI.OnQuestComplete> QUEST_COMPLETE = EventFactory.createArrayBacked(SimpleQuestAPI.OnQuestComplete.class,
            listener -> (serverPlayer, trigger, quest, progress) -> {
                for (SimpleQuestAPI.OnQuestComplete event : listener) {
                    if (!event.onComplete(serverPlayer, trigger, quest, progress))
                        return false;
                }
                return true;
            });

    @Override
    public void onInitialize() {
        SimpleQuests.updateLoaderImpl(new LoaderImpl());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Reloader());
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> QuestCommand.register(dispatcher)));
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ConfigHandler.init();
        SimpleQuests.PERMISSION_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        SimpleQuests.FTB_RANKS = FabricLoader.getInstance().isModLoaded("ftbranks");
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer)
                SimpleQuests.onInteractEntity(serverPlayer, entity, hand);
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register(((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer)
                PlayerData.get(serverPlayer).onBlockInteract(hitResult.getBlockPos(), true);
            return InteractionResult.PASS;
        }));
        PlayerBlockBreakEvents.BEFORE.register(((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayer serverPlayer)
                PlayerData.get(serverPlayer).onBlockInteract(pos, false);
            return true;
        }));
    }

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity.getKillCredit() instanceof ServerPlayer player) {
            PlayerData.get(player).onKill(entity);
        }
    }
}
