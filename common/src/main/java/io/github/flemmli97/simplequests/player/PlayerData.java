package io.github.flemmli97.simplequests.player;

import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerData {

    public static final DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServerPlayer player;
    private List<QuestProgress> currentQuests = new ArrayList<>();
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
        if (this.currentQuests.size() >= ConfigHandler.config.maxConcurrentQuest) {
            this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get("simplequests.active.full")).withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        if (this.isActive(quest)) {
            this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get("simplequests.active")).withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        AcceptType type = this.canAcceptQuest(quest);
        if (type != AcceptType.ACCEPT) {
            if (type == AcceptType.DELAY)
                this.player.sendSystemMessage(Component.literal(String.format(ConfigHandler.lang.get(type.langKey()), this.formattedCooldown(quest))).withStyle(ChatFormatting.DARK_RED));
            else
                this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get(type.langKey())).withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        this.currentQuests.add(new QuestProgress(quest));
        this.player.sendSystemMessage(Component.translatable(ConfigHandler.lang.get("simplequests.accept"), quest.getFormatted(this.player.getServer())).withStyle(ChatFormatting.DARK_GREEN));
        return true;
    }

    public boolean submit() {
        if (this.currentQuests.isEmpty()) {
            this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get("simplequests.current.no")).withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        boolean any = false;
        List<QuestProgress> completed = new ArrayList<>();
        for (QuestProgress prog : this.currentQuests) {
            switch (prog.submit(this.player)) {
                case COMPLETE -> {
                    this.completeQuest(prog);
                    completed.add(prog);
                    any = true;
                }
                case PARTIAL -> this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_YES, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                case NOTHING -> this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_NO, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
            }
        }
        this.currentQuests.removeAll(completed);
        return any;
    }

    public void onKill(LivingEntity entity) {
        List<QuestProgress> completed = new ArrayList<>();
        this.currentQuests.forEach(prog -> {
            Set<QuestEntry> fulfilled = prog.onKill(this.player, entity);
            if (!fulfilled.isEmpty()) {
                this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                fulfilled.forEach(e -> this.player.sendSystemMessage(Component.translatable(ConfigHandler.lang.get("simplequests.kill"), e.translation(this.player.getServer())).withStyle(ChatFormatting.DARK_GREEN)));
            }
            if (prog.isCompleted()) {
                this.completeQuest(prog);
                completed.add(prog);
            }
        });
        this.currentQuests.removeAll(completed);
    }

    private void completeQuest(QuestProgress prog) {
        LootTable lootTable = this.player.getServer().getLootTables().get(prog.getQuest().loot);
        CriteriaTriggers.GENERATE_LOOT.trigger(this.player, prog.getQuest().loot);
        LootContext.Builder builder = new LootContext.Builder(this.player.getLevel())
                .withParameter(LootContextParams.ORIGIN, this.player.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, DamageSource.MAGIC)
                .withParameter(LootContextParams.THIS_ENTITY, this.player)
                .withLuck(this.player.getLuck());
        List<ItemStack> loot = lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY));
        loot.forEach(stack -> {
            boolean bl = this.player.getInventory().add(stack);
            if (!bl || !stack.isEmpty()) {
                ItemEntity itemEntity = this.player.drop(stack, false);
                if (itemEntity != null) {
                    itemEntity.setNoPickUpDelay();
                    itemEntity.setOwner(this.player.getUUID());
                }
            }
        });
        if (this.finishedQuests.isEmpty()) {
            this.questTrackerTime = LocalDateTime.now();
        }
        this.finishedQuests.put(prog.getQuest().id, this.player.level.getGameTime());
        this.player.level.playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
        this.player.sendSystemMessage(Component.literal(String.format(ConfigHandler.lang.get("simplequests.finish"), prog.getQuest().questTaskString)).withStyle(ChatFormatting.DARK_GREEN));
        if (prog.getQuest().neededParentQuest != null && prog.getQuest().redoParent) {
            Quest quest = QuestsManager.instance().getQuests().get(prog.getQuest().neededParentQuest);
            if (quest != null)
                this.finishedQuests.remove(quest.id);
        }
    }

    public void reset(ResourceLocation res, boolean forced) {
        if (this.currentQuests.isEmpty()) {
            this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get("simplequests.current.no")).withStyle(ChatFormatting.DARK_RED));
            return;
        }
        QuestProgress prog = null;
        for (QuestProgress p : this.currentQuests) {
            if (p.getQuest().id.equals(res)) {
                prog = p;
                break;
            }
        }
        if (prog == null) {
            this.player.sendSystemMessage(Component.literal(String.format(ConfigHandler.lang.get("simplequests.reset.notfound"), res)).withStyle(ChatFormatting.DARK_RED));
            return;
        }
        if (!forced && this.resetTick == -1) {
            this.resetTick = this.player.level.getGameTime();
            this.player.sendSystemMessage(Component.literal(ConfigHandler.lang.get("simplequests.reset.confirm")).withStyle(ChatFormatting.DARK_RED));
            return;
        } else if (forced || this.player.level.getGameTime() - this.resetTick < 600) {
            this.player.sendSystemMessage(Component.literal(String.format(ConfigHandler.lang.get("simplequests.reset"), prog.getQuest().questTaskString)).withStyle(ChatFormatting.DARK_RED));
            this.currentQuests.remove(prog);
        }
        this.resetTick = -1;
    }

    public List<QuestProgress> getCurrentQuest() {
        return this.currentQuests;
    }

    public boolean isActive(Quest quest) {
        return this.currentQuests.stream().anyMatch(prog -> prog.getQuest().id.equals(quest.id));
    }

    public AcceptType canAcceptQuest(Quest quest) {
        if (this.questTrackerTime != null && this.questTrackerTime.getDayOfYear() != LocalDateTime.now().getDayOfYear()) {
            this.questTrackerTime = null;
            this.dailyQuestsTracker.clear();
        }
        if (quest.neededParentQuest != null && this.finishedQuests.get(quest.neededParentQuest) == null) {
            return AcceptType.REQUIREMENTS;
        }
        if (quest.repeatDaily > 0 && this.dailyQuestsTracker.getOrDefault(quest.id, 0) >= quest.repeatDaily)
            return AcceptType.DAILYFULL;
        //One time quests
        if (quest.repeatDelay < 0 && this.finishedQuests.containsKey(quest.id))
            return AcceptType.ONETIME;
        if (this.finishedQuests.containsKey(quest.id)) {
            return (quest.repeatDelay == 0 || Math.abs(this.player.level.getGameTime() - this.finishedQuests.get(quest.id)) > quest.repeatDelay) ? AcceptType.ACCEPT : AcceptType.DELAY;
        }
        return AcceptType.ACCEPT;
    }

    public String formattedCooldown(Quest quest) {
        long sec = Math.max(0, quest.repeatDelay - Math.abs(this.player.level.getGameTime() - this.finishedQuests.get(quest.id))) / 20;
        if (sec > 86400) {
            long days = sec / 86400;
            long hours = (sec % 86400) / 3600;
            return String.format("%dd:%dh", days, hours);
        }
        if (sec >= 3600) {
            long hours = sec / 3600;
            long minutes = (sec % 3600) / 60;
            return String.format("%dh:%dm:%ds", hours, minutes, sec % 60);
        }
        if (sec >= 60) {
            long minutes = sec / 60;
            return String.format("%dm:%ds", minutes, sec % 60);
        }
        return String.format("%ds", sec);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag quests = new ListTag();
        this.currentQuests.forEach(prog -> quests.add(prog.save()));
        tag.put("ActiveQuests", quests);
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
        if (tag.contains("ActiveQuests")) {
            ListTag quests = tag.getList("ActiveQuests", Tag.TAG_COMPOUND);
            quests.forEach(q -> {
                QuestProgress prog = new QuestProgress((CompoundTag) q);
                if (prog.getQuest() != null)
                    this.currentQuests.add(prog);
            });
        }
        CompoundTag done = tag.getCompound("FinishedQuests");
        done.getAllKeys().forEach(key -> this.finishedQuests.put(new ResourceLocation(key), done.getLong(key)));
        if (tag.contains("TimeTracker"))
            this.questTrackerTime = LocalDateTime.parse(tag.getString("TimeTracker"), time);
        CompoundTag daily = tag.getCompound("DailyQuestTracker");
        daily.getAllKeys().forEach(key -> this.dailyQuestsTracker.put(new ResourceLocation(key), done.getInt(key)));
    }

    public void clone(PlayerData data) {
        this.currentQuests = data.currentQuests;
        this.finishedQuests = data.finishedQuests;
    }

    public void resetAll() {
        this.currentQuests.clear();
        this.finishedQuests.clear();
        this.questTrackerTime = null;
        this.dailyQuestsTracker.clear();
    }

    public void resetCooldown() {
        this.finishedQuests.replaceAll((res, old) -> Long.MIN_VALUE);
    }

    public enum AcceptType {
        REQUIREMENTS("simplequests.accept.requirements"),
        DAILYFULL("simplequests.accept.daily"),
        DELAY("simplequests.accept.delay"),
        ONETIME("simplequests.accept.onetime"),
        ACCEPT("simplequests.accept.yes");

        String lang;

        AcceptType(String id) {
            this.lang = id;
        }

        public String langKey() {
            return this.lang;
        }
    }
}
