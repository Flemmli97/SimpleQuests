package io.github.flemmli97.simplequests_api.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuest;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class QuestProgress {

    private final Set<String> entries = new HashSet<>();

    private final Map<ProgressionTrackerKey<?, ?>, Map<String, ProgressionTracker<?, ?>>> progressionTrackers = new HashMap<>();

    private final Map<String, Predicate<PlayerQuestData>> tickables = new HashMap<>();

    private QuestBase base;
    private int questIndex;
    private QuestBase quest;
    private Map<String, ResolvedQuestTask> questEntries;

    public QuestProgress(QuestBase quest, PlayerQuestData data, int subQuestIndex) {
        this.base = quest;
        this.questIndex = subQuestIndex;
        this.quest = quest.resolveToQuest(data.getPlayer(), this.questIndex);
        this.questEntries = this.base.resolveTasks(data, this.questIndex);
        this.setup(data);
        if (!this.tickables.isEmpty())
            data.addTickableProgress(this);
    }

    public QuestProgress(CompoundTag tag, PlayerQuestData data) {
        this.load(tag, data);
        this.setup(data);
        if (!this.tickables.isEmpty())
            data.addTickableProgress(this);
    }

    private void setup(PlayerQuestData data) {
        this.questEntries.forEach((s, e) -> {
            e.onAccept(data);
            if (!this.entries.contains(s)) {
                Predicate<PlayerQuestData> ticker = e.tickable();
                if (ticker != null)
                    this.tickables.put(s, ticker);
            }
        });
    }

    public QuestBase getQuest() {
        return this.base;
    }

    public MutableComponent getName(ServerPlayer player) {
        return this.getQuest().getName(player, this.questIndex);
    }

    public List<MutableComponent> getDescription(ServerPlayer player) {
        return this.getQuest().getDescription(player, this.questIndex);
    }

    public List<MutableComponent> getTaskComponents(ServerPlayer player) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, ResolvedQuestTask> e : this.getQuestEntries().entrySet()) {
            list.add(e.getValue().translation(player));
        }
        return list;
    }

    public Collection<ResourceLocation> getCompletionID() {
        if (this.base instanceof CompositeQuest)
            return Set.of(this.base.id, this.quest.id);
        return Set.of(this.base.id);
    }

    public Map<String, ResolvedQuestTask> getQuestEntries() {
        return this.questEntries;
    }

    public SubmitType submit(PlayerQuestData data, String trigger, Consumer<ResolvedQuestTask> cons) {
        boolean any = false;
        ServerPlayer player = data.getPlayer();
        for (Map.Entry<String, ResolvedQuestTask> entry : this.questEntries.entrySet()) {
            if (this.entries.contains(entry.getKey()) && !this.getQuest().submissionTrigger(player, this.questIndex).equals(trigger))
                continue;
            if (entry.getValue().submit(player)) {
                this.entries.add(entry.getKey());
                cons.accept(entry.getValue());
                any = true;
            }
        }
        return switch (this.tryComplete(data, trigger)) {
            case COMPLETE -> SubmitType.COMPLETE;
            case PARTIAL_COMPLETE -> SubmitType.PARTIAL_COMPLETE;
            case NO -> any ? SubmitType.PARTIAL : SubmitType.NOTHING;
        };
    }

    @SuppressWarnings("unchecked")
    public <V, R extends ResolvedQuestTask> Set<Pair<String, R>> tryFullFill(ServerPlayer player, ProgressionTrackerKey<V, R> key, V with) {
        Set<Pair<String, R>> fullfilled = new HashSet<>();
        for (Map.Entry<String, ResolvedQuestTask> e : this.questEntries.entrySet()) {
            if (this.entries.contains(e.getKey()))
                continue;
            if (e.getValue().getId().equals(key.questEntryKey())) {
                R entry = (R) e.getValue();
                ProgressionTracker<V, R> tracker = this.getOrCreateTracker(key, entry, e.getKey());
                if (tracker.progress(player, this, with)) {
                    fullfilled.add(Pair.of(e.getKey(), entry));
                    this.entries.add(e.getKey());
                }
            }
        }
        return fullfilled;
    }

    public QuestState tryComplete(PlayerQuestData data, String trigger) {
        ServerPlayer player = data.getPlayer();
        boolean completed = this.getQuest().submissionTrigger(player, this.questIndex).equals(trigger) && this.entries.containsAll(this.questEntries.keySet());
        QuestBase toResolve = this.getQuest() instanceof CompositeQuest ? this.quest : this.getQuest();
        if (completed) {
            QuestBase next = toResolve.resolveToQuest(player, this.questIndex + 1);
            if (next != null) {
                this.quest = next;
                this.questEntries = toResolve.resolveTasks(data, this.questIndex + 1);
                this.questIndex += 1;
                this.resetTrackers();
                this.setup(data);
                if (!this.tickables.isEmpty())
                    data.addTickableProgress(this);
                return QuestState.PARTIAL_COMPLETE;
            }
        }
        return completed ? QuestState.COMPLETE : QuestState.NO;
    }

    public List<String> finishedTasks() {
        return ImmutableList.copyOf(this.entries);
    }

    public MutableComponent progressComponent(ServerPlayer player, ProgressionTrackerKey<?, ?> key, String entry) {
        ProgressionTracker<?, ?> tracker = this.getTracker(key, entry);
        if (tracker == null)
            return null;
        return tracker.formattedProgress(player, this);
    }

    @SuppressWarnings("unchecked")
    public <V, R extends ResolvedQuestTask> ProgressionTracker<V, R> getTracker(ProgressionTrackerKey<V, R> key, String entryName) {
        Map<String, ProgressionTracker<?, ?>> tracks = this.progressionTrackers.get(key);
        if (tracks == null)
            return null;
        return (ProgressionTracker<V, R>) tracks.get(entryName);
    }

    @SuppressWarnings("unchecked")
    public <T, R extends ResolvedQuestTask> ProgressionTracker<T, R> getOrCreateTracker(ProgressionTrackerKey<T, R> key, R entry, String entryName) {
        Map<String, ProgressionTracker<?, ?>> tracks = this.progressionTrackers.computeIfAbsent(key, k -> new HashMap<>());
        return (ProgressionTracker<T, R>) tracks.computeIfAbsent(entryName, (res) -> ProgressionTrackerRegistry.create(key, entry));
    }

    public Pair<Boolean, Set<ResolvedQuestTask>> tickProgress(PlayerQuestData data) {
        Set<ResolvedQuestTask> fullfilled = new HashSet<>();
        this.tickables.entrySet().removeIf(e -> {
            if (e.getValue().test(data)) {
                fullfilled.add(this.questEntries.get(e.getKey()));
                this.entries.add(e.getKey());
                return true;
            }
            return false;
        });
        return Pair.of(this.tickables.isEmpty(), fullfilled);
    }

    public void resetTrackers() {
        this.entries.clear();
        this.progressionTrackers.clear();
        this.tickables.clear();
    }

    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        DynamicOps<Tag> ops = lookup.createSerializationContext(NbtOps.INSTANCE);
        if (this.base.isDynamic()) {
            tag.putBoolean("DynamicQuest", true);
            tag.put("DynamicQuest", QuestBaseRegistry.CODEC.apply(true, false)
                    .encodeStart(ops, this.base).getOrThrow());
        } else {
            tag.putString("Quest", this.base.id.toString());
        }
        tag.putInt("QuestIndex", this.questIndex);
        CompoundTag entries = new CompoundTag();
        this.questEntries.forEach((id, entry) -> entries.put(id, QuestEntryRegistry.RESOLVED_QUEST_ENTRY_CODEC.encodeStart(ops, entry)
                .mapError(e -> "Couldn't save quest entry " + e).getOrThrow()));
        tag.put("QuestEntries", entries);

        ListTag list = new ListTag();
        this.entries.forEach(res -> list.add(StringTag.valueOf(res)));
        tag.put("FinishedEntries", list);

        CompoundTag progressionTrackers = new CompoundTag();
        this.progressionTrackers.forEach((key, trackers) -> {
            CompoundTag trackerTag = new CompoundTag();
            trackers.forEach((res, i) -> trackerTag.put(res, i.save()));
            progressionTrackers.put(key.toString(), trackerTag);
        });
        tag.put("ProgressionTrackers", progressionTrackers);
        return tag;
    }

    public void load(CompoundTag tag, PlayerQuestData data) {
        DynamicOps<Tag> ops = data.getPlayer().getServer()
                .registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (tag.contains("DynamicQuest")) {
            try {
                this.base = QuestBaseRegistry.CODEC.apply(true, true).parse(ops, tag.getCompound("DynamicQuest"))
                        .getOrThrow();
            } catch (Exception ex) {
                SimpleQuestsAPI.LOGGER.error("Couldn't reconstruct dynamic quest. Skipping");
                throw new IllegalStateException();
            }
        } else {
            this.base = QuestsManager.instance().getActualQuest(ResourceLocation.parse(tag.getString("Quest")), null);
            if (this.base == null) {
                SimpleQuestsAPI.LOGGER.error("Cant find quest with id {}. Skipping", tag.getString("Quest"));
                throw new IllegalStateException();
            }
        }
        this.questIndex = tag.getInt("QuestIndex");
        this.quest = this.base.resolveToQuest(data.getPlayer(), this.questIndex);
        if (tag.contains("QuestEntries")) {
            ImmutableMap.Builder<String, ResolvedQuestTask> builder = new ImmutableMap.Builder<>();
            CompoundTag entries = tag.getCompound("QuestEntries");
            entries.getAllKeys().forEach(key -> builder.put(key, QuestEntryRegistry.RESOLVED_QUEST_ENTRY_CODEC.parse(ops, entries.getCompound(key))
                    .mapError(e -> "Couldn't read quest entry" + e).getOrThrow()));
            this.questEntries = builder.build();
        } else {
            this.questEntries = this.quest.resolveTasks(data, this.questIndex);
        }
        ListTag list = tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.entries.add(t.getAsString()));

        CompoundTag progressionTrackers = tag.getCompound("ProgressionTrackers");
        progressionTrackers.getAllKeys().forEach(key -> {
            CompoundTag trackers = progressionTrackers.getCompound(key);
            ProgressionTrackerKey<?, ResolvedQuestTask> id = ProgressionTrackerRegistry.getKey(ResourceLocation.parse(key));
            Map<String, ProgressionTracker<?, ?>> t = this.progressionTrackers.computeIfAbsent(id, k -> new HashMap<>());
            trackers.getAllKeys().forEach(entry -> this.loadTracker(id, t, entry, trackers));
        });
    }

    private void loadTracker(ProgressionTrackerKey<?, ResolvedQuestTask> key, Map<String, ProgressionTracker<?, ?>> map, String name, CompoundTag tag) {
        if (this.quest == null) {
            SimpleQuestsAPI.LOGGER.error("Quest not set. This shouldn't be!");
            throw new IllegalStateException();
        }
        try {
            ProgressionTracker<?, ResolvedQuestTask> entry = ProgressionTrackerRegistry.deserialize(key, this.questEntries.get(name), tag.get(name));
            map.putIfAbsent(name, entry);
        } catch (ClassCastException e) {
            SimpleQuestsAPI.LOGGER.error("Couldn't find quest entry for tracker {}", name);
        }
    }

    public enum SubmitType {
        COMPLETE,
        PARTIAL,
        PARTIAL_COMPLETE,
        NOTHING
    }
}
