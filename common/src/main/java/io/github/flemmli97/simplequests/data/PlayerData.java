package io.github.flemmli97.simplequests.data;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.api.SimpleQuestImplAPI;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class PlayerData implements PlayerQuestData {

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServerPlayer player;
    private List<QuestProgress> currentQuests = new ArrayList<>();
    private Map<ResourceLocation, Long> cooldownTracker = new HashMap<>();
    private List<QuestProgress> tickables = new ArrayList<>();

    private Set<ResourceLocation> unlockTracker = new HashSet<>();

    private long resetTick = -1;

    private LocalDateTime questTrackerTime = LocalDateTime.now();
    private long dailySeed;
    private final Random questRandom = new Random();
    private final Map<ResourceLocation, Integer> dailyQuestsTracker = new HashMap<>();
    private final Map<ResourceLocation, Integer> dailyQuestsCategoryTracker = new HashMap<>();

    private final Map<ResourceLocation, Integer> finishedQuestsTracker = new HashMap<>();

    private int interactionCooldown;

    private boolean adminMode;

    public static PlayerData get(ServerPlayer player) {
        return ((SimpleQuestDataGet) player).simpleQuestPlayerData();
    }

    public PlayerData(ServerPlayer player) {
        this.player = player;
    }

    public boolean acceptQuest(QuestBase quest, int subQuestIndex) {
        int maxConcurrent = quest.category.getMaxConcurrentQuests() == -1 ? ConfigHandler.CONFIG.maxConcurrentQuest : quest.category.getMaxConcurrentQuests();
        if (maxConcurrent > 0 && this.currentQuests.stream()
                .filter(p -> !p.getQuest().isDailyQuest && (!quest.category.sameCategoryOnly || p.getQuest().category == quest.category)).toList().size() >= maxConcurrent) {
            this.player.sendSystemMessage(Component.translatable("simplequests.active.full").withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        if (this.isActive(quest)) {
            this.player.sendSystemMessage(Component.translatable("simplequests.active").withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        AcceptType type = this.canAcceptQuest(quest);
        if (type != AcceptType.ACCEPT) {
            if (type == AcceptType.DELAY)
                this.player.sendSystemMessage(Component.translatable(type.langKey(), this.formattedCooldown(quest)).withStyle(ChatFormatting.DARK_RED));
            else
                this.player.sendSystemMessage(Component.translatable(type.langKey()).withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        QuestProgress prog = new QuestProgress(quest, this, subQuestIndex);
        this.currentQuests.add(prog);
        if (!prog.getQuest().category.isSilent) {
            this.player.sendSystemMessage(Component.translatable("simplequests.accept",
                    prog.getName(this.player).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).withStyle(ChatFormatting.GREEN));
            this.player.sendSystemMessage(Component.literal(""));
            for (MutableComponent comp : prog.getTaskComponents(this.player)) {
                this.player.sendSystemMessage(Component.translatable("simplequests.task.chat_format",
                        comp.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)).withStyle(ChatFormatting.GOLD));
            }
        }
        return true;
    }

    public Map<ResourceLocation, QuestState> submit(String trigger, boolean sendFailMessage) {
        if (this.currentQuests.isEmpty()) {
            if (sendFailMessage)
                this.player.sendSystemMessage(Component.translatable("simplequests.current.no").withStyle(ChatFormatting.DARK_RED));
            return Map.of();
        }
        Map<ResourceLocation, QuestState> completion = new HashMap<>();
        List<QuestProgress> completed = new ArrayList<>();
        for (QuestProgress prog : this.currentQuests) {
            List<ResolvedQuestTask> tasks = new ArrayList<>();
            switch (prog.submit(this, trigger, tasks::add)) {
                case COMPLETE -> {
                    this.completeQuest(prog);
                    completed.add(prog);
                    completion.put(prog.getQuest().id, QuestState.COMPLETE);
                }
                case PARTIAL_COMPLETE -> {
                    completion.put(prog.getQuest().id, QuestState.PARTIAL_COMPLETE);
                    this.player.sendSystemMessage(Component.translatable("simplequests.finish.sub",
                                    prog.getName(this.player).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE))
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
                case PARTIAL -> {
                    this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_YES, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                    if (!prog.getQuest().category.isSilent) {
                        tasks.forEach(t -> this.player.sendSystemMessage(Component.translatable("simplequests.task.complete", t.translation(this.player)
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                    }
                }
                case NOTHING -> {
                    if (sendFailMessage)
                        this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.VILLAGER_NO, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
                }
            }
        }
        this.currentQuests.removeAll(completed);
        return completion;
    }

    public <V, R extends ResolvedQuestTask> Map<ResourceLocation, QuestState> trigger(ProgressionTrackerKey<V, R> key, V with, SimpleQuestImplAPI.QuestTriggerHook<R> onFullfill, String trigger) {
        if (key.equals(EntityTracker.KEY)) {
            if (this.interactionCooldown > 0)
                return Map.of();
            this.interactionCooldown = 2;
        }
        List<QuestProgress> completed = new ArrayList<>();
        Map<ResourceLocation, QuestState> completion = new HashMap<>();
        this.currentQuests.forEach(prog -> {
            Set<Pair<String, R>> fulfilled = prog.tryFullFill(this.player, key, with);
            if (!fulfilled.isEmpty()) {
                this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
            }
            QuestState state = prog.tryComplete(this, trigger);
            fulfilled.forEach(p -> onFullfill.onFullfill(prog, p, state));
            if (state == QuestState.COMPLETE) {
                this.completeQuest(prog);
                completed.add(prog);
                completion.put(prog.getQuest().id, QuestState.COMPLETE);
            } else if (state == QuestState.PARTIAL_COMPLETE) {
                completion.put(prog.getQuest().id, QuestState.PARTIAL_COMPLETE);
                this.player.sendSystemMessage(Component.translatable("simplequests.finish.sub",
                                prog.getName(this.player).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        });
        this.currentQuests.removeAll(completed);
        return completion;
    }

    @Override
    public <V, R extends ResolvedQuestTask> Map<ResourceLocation, QuestState> trigger(ProgressionTrackerKey<V, R> key, V with, @NotNull String trigger) {
        return this.trigger(key, with, (prog, p, state) -> {
            if (state == QuestState.NO && !prog.getQuest().category.isSilent)
                this.player.sendSystemMessage(Component.translatable("simplequests.task.complete", p.getSecond().translation(this.player)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }, trigger);
    }

    private void completeQuest(QuestProgress prog) {
        prog.getQuest().onComplete(this.player);
        prog.getCompletionID().forEach(id -> {
            this.cooldownTracker.put(id, this.player.level().getGameTime());
            this.unlockTracker.add(id);
            this.dailyQuestsTracker.compute(id, (key, i) -> i == null ? 1 : ++i);
            this.finishedQuestsTracker.compute(id, (key, i) -> i == null ? 1 : ++i);
        });
        this.dailyQuestsTracker.compute(prog.getQuest().category.id, (key, i) -> i == null ? 1 : ++i);
        this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
        if (!prog.getQuest().category.isSilent)
            this.player.sendSystemMessage(Component.translatable("simplequests.finish",
                            prog.getName(this.player).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (!prog.getQuest().neededParentQuests.isEmpty() && prog.getQuest().redoParent) {
            prog.getQuest().neededParentQuests.forEach(res -> {
                Quest quest = QuestsManager.instance().getActualQuest(res, null);
                if (quest != null)
                    this.unlockTracker.remove(quest.id);
            });
        }
    }

    public void reset(ResourceLocation res, boolean forced) {
        this.reset(res, forced, true);
    }

    public void reset(ResourceLocation res, boolean forced, boolean sendMsg) {
        if (this.currentQuests.isEmpty()) {
            if (sendMsg)
                this.player.sendSystemMessage(Component.translatable("simplequests.current.no").withStyle(ChatFormatting.DARK_RED));
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
            if (sendMsg)
                this.player.sendSystemMessage(Component.translatable("simplequests.reset.notfound", res).withStyle(ChatFormatting.DARK_RED));
            return;
        }
        if (!forced && this.resetTick == -1) {
            this.resetTick = this.player.level().getGameTime();
            if (sendMsg)
                this.player.sendSystemMessage(Component.translatable("simplequests.reset.confirm").withStyle(ChatFormatting.DARK_RED));
            return;
        } else if (forced || this.player.level().getGameTime() - this.resetTick < 600) {
            if (sendMsg)
                this.player.sendSystemMessage(Component.translatable("simplequests.reset", prog.getName(this.player)
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)).withStyle(ChatFormatting.DARK_RED));
            this.currentQuests.remove(prog);
            this.removeTickableQuestProgress(prog);
            prog.getQuest().onReset(this.player);
        }
        this.resetTick = -1;
    }

    @Override
    public List<QuestProgress> getCurrentQuest() {
        return ImmutableList.copyOf(this.currentQuests);
    }

    public boolean isActive(QuestBase quest) {
        return this.isActive(quest.id);
    }

    public boolean isActive(ResourceLocation quest) {
        return this.currentQuests.stream().anyMatch(prog -> prog.getQuest().id.equals(quest));
    }

    public List<QuestProgress> getCurrentQuests(QuestCategory category) {
        return this.currentQuests.stream().filter(p -> p.getQuest().category.id.equals(category.id)).toList();
    }

    public AcceptType canAcceptQuest(QuestBase quest) {
        if (quest.isDailyQuest || quest.needsUnlock && !this.unlockTracker.contains(quest.id)) {
            return AcceptType.LOCKED;
        }
        if (!quest.isUnlocked(this.player)
                || (!quest.neededParentQuests.isEmpty() && !this.unlockTracker.containsAll(quest.neededParentQuests))) {
            return AcceptType.REQUIREMENTS;
        }
        if (quest.repeatDaily > 0 && this.dailyQuestsTracker.getOrDefault(quest.id, 0) >= quest.repeatDaily)
            return AcceptType.DAILYFULL;
        if (quest.category.maxDaily > 0 && this.dailyQuestsCategoryTracker.getOrDefault(quest.category.id, 0) >= quest.category.maxDaily)
            return AcceptType.DAILYFULL;
        if (quest.maxRepeat > 0 && this.finishedQuestsTracker.getOrDefault(quest.category.id, 0) >= quest.maxRepeat)
            return AcceptType.MAX;
        //One time quests
        if (quest.repeatDelay < 0 && this.cooldownTracker.containsKey(quest.id))
            return AcceptType.ONETIME;
        if (this.cooldownTracker.containsKey(quest.id)) {
            return (quest.repeatDelay == 0 || Math.abs(this.player.level().getGameTime() - this.cooldownTracker.get(quest.id)) > quest.repeatDelay) ? AcceptType.ACCEPT : AcceptType.DELAY;
        }
        return AcceptType.ACCEPT;
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    public boolean isAdminMode() {
        return this.adminMode;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    @Override
    public void addTickableProgress(QuestProgress progress) {
        if (!this.tickables.contains(progress))
            this.tickables.add(progress);
    }

    @Override
    public void removeTickableQuestProgress(QuestProgress progress) {
        this.tickables.remove(progress);
    }

    public void unlockQuest(ResourceLocation quest) {
        this.unlockTracker.add(quest);
    }

    public void lockQuest(ResourceLocation quest) {
        this.unlockTracker.remove(quest);
    }

    @Override
    public int getTimesCompleted(ResourceLocation quest) {
        return this.finishedQuestsTracker.getOrDefault(quest, 0);
    }

    public void tickTickableQuests(String trigger) {
        List<QuestProgress> completed = new ArrayList<>();
        this.tickables.removeIf(prog -> {
            Pair<Boolean, Set<ResolvedQuestTask>> fulfilled = prog.tickProgress(this);
            if (!fulfilled.getSecond().isEmpty()) {
                this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.PLAYER_LEVELUP, this.player.getSoundSource(), 2 * 0.75f, 1.0f);
            }
            QuestState state = prog.tryComplete(this, trigger);
            switch (state) {
                case COMPLETE -> {
                    this.completeQuest(prog);
                    completed.add(prog);
                    return true;
                }
                case PARTIAL_COMPLETE -> this.player.sendSystemMessage(Component.translatable("simplequests.finish.sub",
                                prog.getName(this.player).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                case NO -> fulfilled.getSecond().forEach(e -> {
                    if (!prog.getQuest().category.isSilent)
                        this.player.sendSystemMessage(Component.translatable("simplequests.task.complete", e.translation(this.player)).withStyle(ChatFormatting.DARK_GREEN));
                });
            }
            return fulfilled.getFirst();
        });
        this.currentQuests.removeAll(completed);
    }

    public void tick() {
        --this.interactionCooldown;
        this.tickTickableQuests("");

        LocalDateTime now = LocalDateTime.now();
        if (this.questTrackerTime == null || this.questTrackerTime.getDayOfYear() != now.getDayOfYear()) {
            this.dailySeed = this.player.getRandom().nextLong();
            this.questTrackerTime = now;
            this.dailyQuestsTracker.forEach((r, i) -> {
                Quest quest = QuestsManager.instance().getActualQuest(r, null);
                if (quest != null && quest.isDailyQuest)
                    this.reset(r, true, false);
            });
            this.dailyQuestsTracker.clear();
            this.dailyQuestsCategoryTracker.clear();
            List<Quest> daily = QuestsManager.instance().getDailyQuests().stream().toList();
            this.questRandom.setSeed(this.dailySeed);
            Collections.shuffle(daily, this.questRandom);
            int amount = ConfigHandler.CONFIG.dailyQuestAmount == -1 ? daily.size() : Mth.clamp(ConfigHandler.CONFIG.dailyQuestAmount, 0, daily.size());
            for (int i = 0; i < amount; i++) {
                Quest quest = daily.get(i);
                this.currentQuests.add(new QuestProgress(quest, this, 0));
                this.dailyQuestsTracker.put(quest.id, 1);
            }
        }
    }

    @Override
    public long getRandomSeed(@Nullable ResourceLocation quest) {
        long time = this.cooldownTracker.getOrDefault(quest, 0L);
        return this.dailySeed + time;
    }

    public String formattedCooldown(QuestBase quest) {
        long sec = Math.max(0, quest.repeatDelay - Math.abs(this.player.level().getGameTime() - this.cooldownTracker.get(quest.id))) / 20;
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
        this.currentQuests.forEach(prog -> quests.add(prog.save(this.player.registryAccess())));
        tag.put("ActiveQuests", quests);
        CompoundTag list = new CompoundTag();
        this.cooldownTracker.forEach((res, time) -> list.putLong(res.toString(), time));
        tag.put("FinishedQuests", list);
        if (this.questTrackerTime != null)
            tag.putString("TimeTracker", this.questTrackerTime.format(TIME));
        CompoundTag daily = new CompoundTag();
        this.dailyQuestsTracker.forEach((res, amount) -> daily.putInt(res.toString(), amount));
        tag.put("DailyQuestTracker", daily);
        CompoundTag dailyCategory = new CompoundTag();
        this.dailyQuestsCategoryTracker.forEach((res, amount) -> dailyCategory.putInt(res.toString(), amount));
        tag.put("DailyQuestCategoryTracker", dailyCategory);
        CompoundTag total = new CompoundTag();
        this.finishedQuestsTracker.forEach((res, amount) -> total.putInt(res.toString(), amount));
        tag.put("FinishedQuestTracker", total);
        ListTag unlocked = new ListTag();
        this.unlockTracker.forEach(res -> unlocked.add(StringTag.valueOf(res.toString())));
        tag.put("UnlockedQuests", unlocked);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ActiveQuests")) {
            ListTag quests = tag.getList("ActiveQuests", Tag.TAG_COMPOUND);
            quests.forEach(q -> {
                try {
                    QuestProgress prog = new QuestProgress((CompoundTag) q, this);
                    if (prog.getQuest() != null)
                        this.currentQuests.add(prog);
                } catch (IllegalStateException ignored) {
                }
            });
        }
        CompoundTag done = tag.getCompound("FinishedQuests");
        done.getAllKeys().forEach(key -> this.cooldownTracker.put(ResourceLocation.parse(key), done.getLong(key)));
        if (tag.contains("TimeTracker"))
            this.questTrackerTime = LocalDateTime.parse(tag.getString("TimeTracker"), TIME);
        CompoundTag daily = tag.getCompound("DailyQuestTracker");
        daily.getAllKeys().forEach(key -> this.dailyQuestsTracker.put(ResourceLocation.parse(key), daily.getInt(key)));
        CompoundTag dailyCategory = tag.getCompound("DailyQuestCategoryTracker");
        dailyCategory.getAllKeys().forEach(key -> this.dailyQuestsCategoryTracker.put(ResourceLocation.parse(key), dailyCategory.getInt(key)));
        CompoundTag total = tag.getCompound("FinishedQuestTracker");
        total.getAllKeys().forEach(key -> this.finishedQuestsTracker.put(ResourceLocation.parse(key), total.getInt(key)));
        ListTag unlocked = tag.getList("UnlockedQuests", Tag.TAG_STRING);
        unlocked.forEach(t -> this.unlockTracker.add(ResourceLocation.parse(t.getAsString())));
    }

    public void clone(PlayerData data) {
        this.currentQuests = data.currentQuests;
        this.tickables = data.tickables;
        this.cooldownTracker = data.cooldownTracker;
        this.unlockTracker = data.unlockTracker;
        this.questTrackerTime = data.questTrackerTime;
        this.dailySeed = data.dailySeed;
        this.dailyQuestsTracker.clear();
        this.dailyQuestsTracker.putAll(data.dailyQuestsTracker);
        this.dailyQuestsCategoryTracker.clear();
        this.dailyQuestsCategoryTracker.putAll(data.dailyQuestsCategoryTracker);
        this.finishedQuestsTracker.putAll(data.finishedQuestsTracker);
    }

    public void resetAll() {
        this.currentQuests.forEach(p -> p.getQuest().onReset(this.player));
        this.currentQuests.clear();
        this.tickables.clear();
        this.cooldownTracker.clear();
        this.unlockTracker.clear();
        this.questTrackerTime = null;
        this.dailyQuestsTracker.clear();
        this.dailyQuestsCategoryTracker.clear();
        this.finishedQuestsTracker.clear();
    }

    public void resetCooldown() {
        this.cooldownTracker.replaceAll((res, old) -> Long.MIN_VALUE);
    }

    public enum AcceptType {

        REQUIREMENTS("simplequests.accept.requirements"),
        DAILYFULL("simplequests.accept.daily"),
        DELAY("simplequests.accept.delay"),
        MAX("simplequests.accept.max"),
        ONETIME("simplequests.accept.onetime"),
        ACCEPT("simplequests.accept.yes"),
        LOCKED("simplequests.accept.locked");

        final String lang;

        AcceptType(String id) {
            this.lang = id;
        }

        public String langKey() {
            return this.lang;
        }

        public boolean guiVisible(ServerPlayer player) {
            if (PlayerData.get(player).adminMode) {
                return true;
            }
            return this == AcceptType.REQUIREMENTS || this == AcceptType.ONETIME
                    || this == AcceptType.MAX || this == AcceptType.LOCKED;
        }
    }
}
