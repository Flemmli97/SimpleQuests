package io.github.flemmli97.simplequests_api.quest;

import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The instance of an active quest. This allows for e.g. dynamic values that should only be available once a player accepts a quest.
 */
public interface ResolvedQuest {

    static void dropLoot(ServerPlayer player, ResourceKey<LootTable> lootID) {
        LootTable lootTable = player.getServer().reloadableRegistries().getLootTable(lootID);
        CriteriaTriggers.GENERATE_LOOT.trigger(player, lootID);
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, player.damageSources().magic())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.ENTITY);
        List<ItemStack> loot = lootTable.getRandomItems(params);
        loot.forEach(stack -> {
            boolean bl = player.getInventory().add(stack);
            if (!bl || !stack.isEmpty()) {
                ItemEntity itemEntity = player.drop(stack, false);
                if (itemEntity != null) {
                    itemEntity.setNoPickUpDelay();
                    itemEntity.setThrower(player);
                }
            }
        });
    }

    static void runCommand(ServerPlayer player, String command) {
        if (!command.isEmpty())
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
    }

    QuestDefinition source();

    default ResourceLocation id() {
        return this.source().id();
    }

    MutableComponent name(ServerPlayer player);

    List<MutableComponent> description(ServerPlayer player);

    Map<String, ResolvedQuestTask> resolveTasks(QuestProgress progress);

    default Set<ResourceLocation> completed() {
        return Set.of(this.id());
    }

    /**
     * The trigger required to complete this quest
     */
    default String submissionTrigger(ServerPlayer player) {
        return "";
    }

    default void onComplete(ServerPlayer player) {
    }

    default void onReset(ServerPlayer player) {
    }

    Tag save(HolderLookup.Provider lookup);
}
