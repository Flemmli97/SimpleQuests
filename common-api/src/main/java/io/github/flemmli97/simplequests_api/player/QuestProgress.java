package io.github.flemmli97.simplequests_api.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuest;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class QuestProgress {

    private final Set<String> entries = new HashSet<>();

    private final Map<ProgressionTrackerKey<?, ?>, Map<String, ProgressionTracker<?, ?>>> progressionTrackers = new HashMap<>();

    private final Map<String, Predicate<PlayerQuestData>> tickables = new HashMap<>();

    private QuestBase base;
    private int questIndex;
    private QuestBase quest;
    private Map<String, QuestEntry> questEntries;

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

    public MutableComponent getTask(ServerPlayer player) {
        return this.getQuest().getTask(player, this.questIndex);
    }

    public MutableComponent formattedQuest(ServerPlayer player) {
        return this.getQuest().getFormattedWith(player, this.questIndex, this.getQuestEntries());
    }

    public List<MutableComponent> getDescription(ServerPlayer player) {
        return this.getQuest().getDescription(player, this.questIndex);
    }

    public Collection<ResourceLocation> getCompletionID() {
        if (this.base instanceof CompositeQuest)
            return Set.of(this.base.id, this.quest.id);
        return Set.of(this.base.id);
    }

    public Map<String, QuestEntry> getQuestEntries() {
        return this.questEntries;
    }

    public SubmitType submit(PlayerQuestData data, String trigger) {
        boolean any = false;
        ServerPlayer player = data.getPlayer();
        for (Map.Entry<String, QuestEntry> entry : this.questEntries.entrySet()) {
            if (this.entries.contains(entry.getKey()) && !this.getQuest().submissionTrigger(player, this.questIndex).equals(trigger))
                continue;
            if (entry.getValue().submit(player)) {
                this.entries.add(entry.getKey());
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
    public <V, T extends QuestEntry> Set<Pair<String, T>> tryFullFill(ServerPlayer player, ProgressionTrackerKey<V, T> key, V with) {
        Set<Pair<String, T>> fullfilled = new HashSet<>();
        for (Map.Entry<String, QuestEntry> e : this.questEntries.entrySet()) {
            if (this.entries.contains(e.getKey()))
                continue;
            if (e.getValue().getId().equals(key.questEntryKey())) {
                T entry = (T) e.getValue();
                ProgressionTracker<V, T> tracker = this.getOrCreateTracker(key, entry, e.getKey());
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
    public <T, E extends QuestEntry> ProgressionTracker<T, E> getTracker(ProgressionTrackerKey<T, E> key, String entryName) {
        Map<String, ProgressionTracker<?, ?>> tracks = this.progressionTrackers.get(key);
        if (tracks == null)
            return null;
        return (ProgressionTracker<T, E>) tracks.get(entryName);
    }

    @SuppressWarnings("unchecked")
    public <T, E extends QuestEntry> ProgressionTracker<T, E> getOrCreateTracker(ProgressionTrackerKey<T, E> key, E entry, String entryName) {
        Map<String, ProgressionTracker<?, ?>> tracks = this.progressionTrackers.computeIfAbsent(key, k -> new HashMap<>());
        return (ProgressionTracker<T, E>) tracks.computeIfAbsent(entryName, (res) -> ProgressionTrackerRegistry.create(key, entry));
    }

    public Pair<Boolean, Set<QuestEntry>> tickProgress(PlayerQuestData data) {
        Set<QuestEntry> fullfilled = new HashSet<>();
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (this.base.isDynamic()) {
            tag.putBoolean("DynamicQuest", true);
            tag.put("DynamicQuest", JsonCodecs.NullableJsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, this.base.serialize(true, false)));
        } else {
            tag.putString("Quest", this.base.id.toString());
        }
        tag.putInt("QuestIndex", this.questIndex);
        CompoundTag entries = new CompoundTag();
        this.questEntries.forEach((id, entry) -> entries.put(id, QuestEntryRegistry.CODEC.encodeStart(NbtOps.INSTANCE, entry).getOrThrow(false, e -> SimpleQuestsAPI.LOGGER.error("Couldn't save quest entry {}", e))));
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
        if (tag.contains("DynamicQuest")) {
            JsonElement e = NbtOps.INSTANCE.convertTo(JsonCodecs.NullableJsonOps.INSTANCE, tag.getCompound("DynamicQuest"));
            try {
                this.base = QuestBaseRegistry.deserializeFull(e.getAsJsonObject());
            } catch (Exception ex) {
                SimpleQuestsAPI.LOGGER.error("Couldn't reconstruct dynamic quest. Skipping");
                throw new IllegalStateException();
            }
        } else {
            this.base = QuestsManager.instance().getActualQuest(new ResourceLocation(tag.getString("Quest")), null);
            if (this.base == null) {
                SimpleQuestsAPI.LOGGER.error("Cant find quest with id {}. Skipping", tag.getString("Quest"));
                throw new IllegalStateException();
            }
        }
        this.questIndex = tag.getInt("QuestIndex");
        this.quest = this.base.resolveToQuest(data.getPlayer(), this.questIndex);
        if (tag.contains("QuestEntries")) {
            ImmutableMap.Builder<String, QuestEntry> builder = new ImmutableMap.Builder<>();
            CompoundTag entries = tag.getCompound("QuestEntries");
            entries.getAllKeys().forEach(key -> builder.put(key, QuestEntryRegistry.CODEC.parse(NbtOps.INSTANCE, QuestsManager.parseLegacy(entries.getCompound(key)))
                    .getOrThrow(false, e -> SimpleQuestsAPI.LOGGER.error("Couldn't read quest entry{}", e))));
            this.questEntries = builder.build();
        } else {
            this.questEntries = this.quest.resolveTasks(data, this.questIndex);
        }
        ListTag list = tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.entries.add(t.getAsString()));

        CompoundTag progressionTrackers = tag.getCompound("ProgressionTrackers");
        progressionTrackers.getAllKeys().forEach(key -> {
            CompoundTag trackers = progressionTrackers.getCompound(key);
            ProgressionTrackerKey<?, QuestEntry> id = ProgressionTrackerRegistry.getKey(new ResourceLocation(key));
            Map<String, ProgressionTracker<?, ?>> t = this.progressionTrackers.computeIfAbsent(id, k -> new HashMap<>());
            trackers.getAllKeys().forEach(entry -> this.loadTracker(id, t, entry, trackers));
        });
    }

    private void loadTracker(ProgressionTrackerKey<?, QuestEntry> key, Map<String, ProgressionTracker<?, ?>> map, String name, CompoundTag tag) {
        if (this.quest == null) {
            SimpleQuestsAPI.LOGGER.error("Quest not set. This shouldn't be!");
            throw new IllegalStateException();
        }
        try {
            ProgressionTracker<?, QuestEntry> entry = ProgressionTrackerRegistry.deserialize(key, this.questEntries.get(name), tag.get(name));
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
