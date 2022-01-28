package io.github.flemmli97.simplequests.player;

import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerData {

    public static final DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServerPlayer player;
    private QuestProgress currentQuest;
    private Map<ResourceLocation, Long> finishedQuests = new HashMap<>();

    private long resetTick = -1;

    private LocalDateTime questTrackerTime = null;
    private final Map<ResourceLocation, Integer> dailyQuestsTracker = new HashMap<>();

    public static PlayerData get(ServerPlayer player) {
        return ((SimpleQuestDataGet) player).simpleQuestPlayerData();
    }

    public PlayerData(ServerPlayer player) {
        this.player = player;
    }

    public boolean acceptQuest(Quest quest) {
        if (this.currentQuest != null) {
            this.player.sendMessage(new TextComponent(ConfigHandler.lang.get("simplequests.active")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            return false;
        }
        if (!this.canAcceptQuest(quest)) {
            this.player.sendMessage(new TextComponent(ConfigHandler.lang.get("simplequests.missing.requirements")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            return false;
        }
        this.currentQuest = new QuestProgress(quest);
        this.player.sendMessage(new TranslatableComponent(ConfigHandler.lang.get("simplequests.accept"), quest.getFormatted(this.player.getServer())).withStyle(ChatFormatting.DARK_GREEN), Util.NIL_UUID);
        return true;
    }

    public boolean submit() {
        if (this.currentQuest == null) {
            this.player.sendMessage(new TextComponent(ConfigHandler.lang.get("simplequests.current.no")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            return false;
        }
        switch (this.currentQuest.submit(this.player)) {
            case COMPLETE -> {
                LootTable lootTable = this.player.getServer().getLootTables().get(this.currentQuest.getQuest().loot);
                CriteriaTriggers.GENERATE_LOOT.trigger(this.player, this.currentQuest.getQuest().loot);
                LootContext.Builder builder = new LootContext.Builder(this.player.getLevel())
                        .withParameter(LootContextParams.ORIGIN, this.player.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.MAGIC);
                builder.withLuck(this.player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, this.player);
                List<ItemStack> loot = lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY));
                loot.forEach(stack -> this.player.getInventory().add(stack));
                if (this.finishedQuests.isEmpty()) {
                    this.questTrackerTime = LocalDateTime.now();
                }
                this.finishedQuests.put(this.currentQuest.getQuest().id, this.player.level.getGameTime());
                this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                this.player.sendMessage(new TextComponent(String.format(ConfigHandler.lang.get("simplequests.finish"), this.currentQuest.getQuest().questTaskString)).withStyle(ChatFormatting.DARK_GREEN), Util.NIL_UUID);
                this.currentQuest = null;
                return true;
            }
            case PARTIAL -> this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_YES, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
            case NOTHING -> this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_NO, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
        }
        return false;
    }

    public void onKill(LivingEntity entity) {
        if (this.currentQuest != null) {
            Set<QuestEntry> fulfilled = this.currentQuest.onKill(this.player, entity);
            if (!fulfilled.isEmpty()) {
                this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                fulfilled.forEach(e -> this.player.sendMessage(new TranslatableComponent(ConfigHandler.lang.get("simplequests.kill"), e.translation(this.player.getServer())).withStyle(ChatFormatting.DARK_GREEN), Util.NIL_UUID));
            }
        }
    }

    public void reset() {
        if (this.currentQuest == null) {
            this.player.sendMessage(new TextComponent(ConfigHandler.lang.get("simplequests.current.no")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            return;
        }
        if (this.resetTick == -1) {
            this.resetTick = this.player.level.getGameTime();
            this.player.sendMessage(new TextComponent(ConfigHandler.lang.get("simplequests.reset.confirm")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            return;
        } else if (this.player.level.getGameTime() - this.resetTick < 600) {
            this.player.sendMessage(new TextComponent(String.format(ConfigHandler.lang.get("simplequests.reset"), this.currentQuest.getQuest().questTaskString)).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
            this.currentQuest = null;
        }
        this.resetTick = -1;
    }

    public QuestProgress getCurrentQuest() {
        return this.currentQuest;
    }

    public boolean canAcceptQuest(Quest quest) {
        if (this.questTrackerTime != null && this.questTrackerTime.getDayOfYear() != LocalDateTime.now().getDayOfYear()) {
            this.questTrackerTime = null;
            this.dailyQuestsTracker.clear();
        }
        if (quest.neededParentQuest != null && this.finishedQuests.get(quest.neededParentQuest) == null) {
            return false;
        }
        if (quest.repeatDaily > 0 && this.dailyQuestsTracker.getOrDefault(quest.id, 0) >= quest.repeatDaily)
            return false;
        //One time quests
        if (quest.repeatDelay < 0 && this.finishedQuests.containsKey(quest.id))
            return false;
        if (this.finishedQuests.containsKey(quest.id)) {
            return quest.repeatDelay == 0 || Math.abs(this.player.level.getGameTime() - this.finishedQuests.get(quest.id)) > quest.repeatDelay;
        }
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (this.currentQuest != null)
            tag.put("ActiveQuest", this.currentQuest.save());
        CompoundTag list = new CompoundTag();
        this.finishedQuests.forEach((res, time) -> list.putLong(res.toString(), time));
        tag.put("FinishedQuests", list);
        if (this.questTrackerTime != null)
            tag.putString("TimeTracker", this.questTrackerTime.format(time));
        CompoundTag daily = new CompoundTag();
        this.dailyQuestsTracker.forEach((res, amount) -> daily.putInt(res.toString(), amount));
        tag.put("DailyQuestTracker", daily);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ActiveQuest"))
            this.currentQuest = new QuestProgress(tag.getCompound("ActiveQuest"));
        if (this.currentQuest != null && this.currentQuest.getQuest() == null) {
            this.currentQuest = null;
        }
        CompoundTag done = tag.getCompound("FinishedQuests");
        done.getAllKeys().forEach(key -> this.finishedQuests.put(new ResourceLocation(key), done.getLong(key)));
        if (tag.contains("TimeTracker"))
            this.questTrackerTime = LocalDateTime.parse(tag.getString("TimeTracker"), time);
        CompoundTag daily = tag.getCompound("DailyQuestTracker");
        daily.getAllKeys().forEach(key -> this.dailyQuestsTracker.put(new ResourceLocation(key), done.getInt(key)));
    }

    public void clone(PlayerData data) {
        this.currentQuest = data.currentQuest;
        this.finishedQuests = data.finishedQuests;
    }
}
