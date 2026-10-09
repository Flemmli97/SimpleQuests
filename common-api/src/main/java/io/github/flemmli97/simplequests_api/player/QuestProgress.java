package io.github.flemmli97.simplequests_api.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import io.github.flemmli97.simplequests_api.APIPlatform;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.quest.InvalidQuestStateException;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestHolder;
import io.github.flemmli97.simplequests_api.quest.QuestState;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class QuestProgress {

    private final PlayerQuestData data;

    private QuestDefinition rootDefinition;

    private final List<QuestProgressState> questStack = new ArrayList<>();
    private Map<String, ResolvedQuestTask> tasks;
    private final Set<String> completedTasks = new HashSet<>();

    private final Map<ProgressionTrackerKey<?, ?>, Map<String, ProgressionTracker<?, ?>>> progressionTrackers = new HashMap<>();

    private final Map<String, Predicate<PlayerQuestData>> tickables = new HashMap<>();

    public QuestProgress(QuestDefinition quest, PlayerQuestData data, int subQuestIndex) {
        this.data = data;
        this.rootDefinition = quest;
        QuestProgressState base = new QuestProgressState(quest, subQuestIndex);
        if (base.quest() != null)
            this.questStack.add(base);
        List<QuestHolder> subQuests = quest.getSubQuests();
        if (!quest.isValidAsRoot() && !subQuests.isEmpty() && subQuestIndex < subQuests.size()) {
            quest = subQuests.get(subQuestIndex).getDefinition();
            this.questStack.add(new QuestProgressState(quest, 0));
        }
        this.advanceQuest();
    }

    public QuestProgress(CompoundTag tag, PlayerQuestData data) {
        this.data = data;
        this.load(tag);
        this.setup();
    }

    private void setup() {
        this.tasks.forEach((s, e) -> {
            e.onAccept(this.data);
            if (!this.completedTasks.contains(s)) {
                Predicate<PlayerQuestData> ticker = e.tickable();
                if (ticker != null)
                    this.tickables.put(s, ticker);
            }
        });
        if (!this.tickables.isEmpty())
            this.data.addTickableProgress(this);
    }

    public QuestDefinition getRootDefinition() {
        return this.rootDefinition;
    }

    public ResourceLocation getRootId() {
        return this.getRootQuest().id();
    }

    /**
     * @return The quest that initiated this progress. This is not always equal to the {@link #getRootDefinition}.
     * E.g. with Composite quests this is the quest that was selected
     */
    public ResolvedQuest getRootQuest() {
        return this.questStack.getFirst().quest();
    }

    public int getRootProgressStage() {
        return this.questStack.getFirst().index();
    }

    public boolean valid() {
        return this.getRootQuest() != null;
    }

    @Nullable
    public ResolvedQuest getActiveQuest() {
        return this.questStack.getLast().quest();
    }

    public QuestDefinition getActiveDefinition() {
        return this.questStack.isEmpty() ? this.rootDefinition : this.questStack.getLast().definition;
    }

    public PlayerQuestData data() {
        return this.data;
    }

    public ServerPlayer player() {
        return this.data().getPlayer();
    }

    public MutableComponent name(ServerPlayer player) {
        return this.getRootQuest().name(player);
    }

    public List<MutableComponent> description(ServerPlayer player) {
        return this.getRootQuest().description(player);
    }

    public List<MutableComponent> getTaskComponents(ServerPlayer player) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, ResolvedQuestTask> e : this.getTasks().entrySet()) {
            list.add(e.getValue().translation(player));
        }
        return list;
    }

    public Collection<ResourceLocation> getCompletionID() {
        return new ImmutableSet.Builder<ResourceLocation>()
                .add(this.rootDefinition.id())
                .addAll(this.getRootQuest().completed()).build();
    }

    public Map<String, ResolvedQuestTask> getTasks() {
        return this.tasks;
    }

    public Set<Pair<String, ResolvedQuestTask>> submit(String trigger) {
        return this.submit(trigger, Integer.MAX_VALUE);
    }

    public Set<Pair<String, ResolvedQuestTask>> submit(String trigger, int amount) {
        ServerPlayer player = this.data.getPlayer();
        Set<Pair<String, ResolvedQuestTask>> fulfilled = new HashSet<>();
        for (Map.Entry<String, ResolvedQuestTask> entry : this.tasks.entrySet()) {
            if (this.completedTasks.contains(entry.getKey()) && !this.getRootQuest().submissionTrigger(player).equals(trigger))
                continue;
            if (entry.getValue().submit(player)) {
                this.completedTasks.add(entry.getKey());
                fulfilled.add(Pair.of(entry.getKey(), entry.getValue()));
                if (fulfilled.size() >= amount) {
                    break;
                }
            }
        }
        return fulfilled;
    }

    public <V, R extends ResolvedQuestTask> Set<Pair<String, R>> tryFulfill(ServerPlayer player, ProgressionTrackerKey<V, R> key, V with) {
        return this.tryFulfill(player, key, with, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    public <V, R extends ResolvedQuestTask> Set<Pair<String, R>> tryFulfill(ServerPlayer player, ProgressionTrackerKey<V, R> key, V with, int amount) {
        Set<Pair<String, R>> fulfilled = new HashSet<>();
        for (Map.Entry<String, ResolvedQuestTask> e : this.tasks.entrySet()) {
            if (this.completedTasks.contains(e.getKey()))
                continue;
            if (e.getValue().getId().equals(key.questTaskKey())) {
                R entry = (R) e.getValue();
                ProgressionTracker<V, R> tracker = this.getOrCreateTracker(key, entry, e.getKey());
                if (tracker.progress(player, this, with)) {
                    fulfilled.add(Pair.of(e.getKey(), entry));
                    this.completedTasks.add(e.getKey());
                    if (fulfilled.size() >= amount) {
                        break;
                    }
                }
            }
        }
        return fulfilled;
    }

    public QuestState tryComplete(String trigger) {
        if (this.requireSelect())
            return QuestState.SELECT;
        ServerPlayer player = this.data.getPlayer();
        boolean completed = this.completedTasks.containsAll(this.tasks.keySet()) && this.getRootQuest().submissionTrigger(player).equals(trigger);
        if (completed && !APIPlatform.INSTANCE.onQuestComplete(player, trigger, this.getRootQuest(), this))
            completed = false;
        if (completed) {
            if (this.advanceQuest()) {
                if (this.requireSelect())
                    return QuestState.SELECT;
                return QuestState.PARTIAL_COMPLETE;
            }
        }
        return completed ? QuestState.COMPLETE : QuestState.NO;
    }

    public boolean requireSelect() {
        if (this.getActiveQuest() != null)
            return false;
        QuestDefinition def = this.getActiveDefinition();
        return !def.advanceable() && !def.getSubQuests().isEmpty();
    }

    public boolean selectSubQuest(int idx) {
        if (!this.requireSelect())
            return false;
        QuestDefinition def = this.getActiveDefinition();
        if (idx < def.getSubQuests().size()) {
            this.questStack.removeLast();
            def = def.getSubQuests().get(idx).getDefinition();
            this.questStack.add(new QuestProgressState(def, 0));
            this.setupQuest();
            this.advanceQuest();
            return true;
        }
        return false;
    }

    public List<QuestDefinition> selectableSubQuests() {
        if (!this.requireSelect())
            return List.of();
        return this.getActiveDefinition().getSubQuests().stream().map(QuestHolder::getDefinition).toList();
    }

    /**
     * Advances the quest down its progression based on subquests.
     */
    private boolean advanceQuest() {
        if (this.questStack.isEmpty())
            return false;
        while (this.questStack.size() > 1 && !this.questStack.getLast().canAdvance())
            this.questStack.removeLast();
        return this.advanceRecursively();
    }

    private boolean advanceRecursively() {
        if (this.questStack.getFirst().definition.getSubQuests().isEmpty()) {
            this.setupQuest();
            return false;
        }
        QuestProgressState state = this.questStack.getLast();
        QuestDefinition next = state.advance();
        if (next != null) {
            this.questStack.add(new QuestProgressState(next, 0));
            if (!this.advanceRecursively()) {
                this.setupQuest();
            } else {
                this.clearStates();
            }
            return true;
        }
        return false;
    }

    public List<String> completedTasks() {
        return ImmutableList.copyOf(this.completedTasks);
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

    public Pair<Boolean, Set<ResolvedQuestTask>> tickProgress() {
        Set<ResolvedQuestTask> fulfilled = new HashSet<>();
        this.tickables.entrySet().removeIf(e -> {
            if (e.getValue().test(this.data)) {
                fulfilled.add(this.tasks.get(e.getKey()));
                this.completedTasks.add(e.getKey());
                return true;
            }
            return false;
        });
        return Pair.of(this.tickables.isEmpty(), fulfilled);
    }

    private void setupQuest() {
        this.clearStates();
        ResolvedQuest quest = this.getActiveQuest();
        if (quest != null) {
            this.tasks = quest.resolveTasks(this);
        }
        this.setup();
    }

    private void clearStates() {
        this.tasks = Map.of();
        this.resetTrackers();
    }

    public void resetTrackers() {
        this.completedTasks.clear();
        this.progressionTrackers.clear();
        this.tickables.clear();
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        DynamicOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        tag.putString("Quest", this.rootDefinition.id().toString());
        ListTag states = new ListTag();
        this.questStack.forEach(state -> {
            CompoundTag statesTag = new CompoundTag();
            statesTag.putString("Id", state.definition.id().toString());
            statesTag.put("State", state.save(provider));
            states.add(statesTag);
        });
        tag.put("QuestStates", states);

        CompoundTag entries = new CompoundTag();
        this.tasks.forEach((id, entry) -> entries.put(id, QuestEntryRegistry.RESOLVED_QUEST_ENTRY_CODEC.encodeStart(ops, entry)
                .mapError(e -> "Couldn't save quest entry " + e).getOrThrow()));
        tag.put("QuestTasks", entries);
        ListTag list = new ListTag();
        this.completedTasks.forEach(res -> list.add(StringTag.valueOf(res)));
        tag.put("CompletedTasks", list);
        CompoundTag progressionTrackers = new CompoundTag();
        this.progressionTrackers.forEach((key, trackers) -> {
            CompoundTag trackerTag = new CompoundTag();
            trackers.forEach((res, i) -> trackerTag.put(res, i.save()));
            progressionTrackers.put(key.toString(), trackerTag);
        });
        tag.put("ProgressionTrackers", progressionTrackers);
        return tag;
    }

    public void load(CompoundTag tag) {
        HolderLookup.Provider provider = this.data.getPlayer().getServer().registryAccess();
        DynamicOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        this.rootDefinition = QuestsManager.instance().getQuest(ResourceLocation.parse(tag.getString("Quest")));
        if (this.rootDefinition == null) {
            throw new InvalidQuestStateException("Cant find quest with id " + tag.getString("Quest") + "!");
        }
        if (tag.contains("QuestIndex")) {
            // Legacy Data
            QuestDefinition quest = this.rootDefinition;
            int subQuestIndex = tag.getInt("QuestIndex");
            QuestProgressState base = new QuestProgressState(quest, subQuestIndex);
            if (base.quest() != null)
                this.questStack.add(base);
            List<QuestHolder> subQuests = this.rootDefinition.getSubQuests();
            if (!subQuests.isEmpty() && subQuestIndex < subQuests.size()) {
                quest = subQuests.get(subQuestIndex).getDefinition();
                this.questStack.add(new QuestProgressState(quest, 0));
            }
        } else {
            ListTag states = tag.getList("QuestStates", Tag.TAG_COMPOUND);
            states.forEach(t -> {
                CompoundTag statesTag = (CompoundTag) t;
                this.questStack.add(new QuestProgressState(statesTag.getString("Id"), statesTag.getCompound("State"), provider));
            });
        }
        if (tag.contains("QuestTasks") || tag.contains("QuestEntries")) {
            ImmutableMap.Builder<String, ResolvedQuestTask> builder = new ImmutableMap.Builder<>();
            CompoundTag entries = tag.contains("QuestTasks") ? tag.getCompound("QuestTasks") : tag.getCompound("QuestEntries");
            entries.getAllKeys().forEach(key -> builder.put(key, QuestEntryRegistry.RESOLVED_QUEST_ENTRY_CODEC.parse(ops, entries.getCompound(key))
                    .mapError(e -> "Couldn't read quest entry " + e).getOrThrow()));
            this.tasks = builder.build();
        } else {
            this.tasks = this.getActiveQuest().resolveTasks(this);
        }
        ListTag list = tag.contains("CompletedTasks") ? tag.getList("CompletedTasks", Tag.TAG_STRING) : tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.completedTasks.add(t.getAsString()));

        CompoundTag progressionTrackers = tag.getCompound("ProgressionTrackers");
        progressionTrackers.getAllKeys().forEach(key -> {
            CompoundTag trackers = progressionTrackers.getCompound(key);
            ProgressionTrackerKey<?, ResolvedQuestTask> id = ProgressionTrackerRegistry.getKey(ResourceLocation.parse(key));
            Map<String, ProgressionTracker<?, ?>> t = this.progressionTrackers.computeIfAbsent(id, k -> new HashMap<>());
            trackers.getAllKeys().forEach(entry -> this.loadTracker(id, t, entry, trackers));
        });
    }

    private void loadTracker(ProgressionTrackerKey<?, ResolvedQuestTask> key, Map<String, ProgressionTracker<?, ?>> map, String name, CompoundTag tag) {
        try {
            ProgressionTracker<?, ResolvedQuestTask> entry = ProgressionTrackerRegistry.deserialize(key, this.tasks.get(name), tag.get(name));
            map.putIfAbsent(name, entry);
        } catch (ClassCastException e) {
            SimpleQuestsAPI.LOGGER.error("Couldn't find quest entry for tracker {}", name);
        }
    }

    private class QuestProgressState {

        private final QuestDefinition definition;
        @Nullable
        private final ResolvedQuest quest;
        private int nextIndex;

        private QuestProgressState(QuestDefinition definition, int index) {
            this.definition = definition;
            this.quest = definition.resolve(QuestProgress.this);
            this.nextIndex = index;
        }

        private QuestProgressState(String idS, CompoundTag tag, HolderLookup.Provider provider) {
            ResourceLocation id = ResourceLocation.parse(idS);
            this.definition = QuestProgress.this.getActiveDefinition().getSubQuest(id).orElse(QuestsManager.instance().getQuest(id));
            if (this.definition == null) {
                throw new InvalidQuestStateException("Cant find quest with id " + id + "!");
            }
            this.quest = this.definition.load(QuestProgress.this, tag.get("Quest"), provider);
            this.nextIndex = tag.getInt("QuestIndex");
        }

        public boolean canAdvance() {
            if (this.quest == null) {
                return false;
            }
            return this.quest.source().advanceable() && this.nextIndex < this.quest.source().getSubQuests().size();
        }

        public QuestDefinition advance() {
            if (!this.canAdvance()) {
                return null;
            }
            List<QuestHolder> subQuests = this.quest.source().getSubQuests();
            if (this.nextIndex >= subQuests.size()) {
                return null;
            }
            return subQuests.get(this.nextIndex++).getDefinition();
        }

        public ResolvedQuest quest() {
            return this.quest;
        }

        public int index() {
            return this.nextIndex - 1;
        }

        public Tag save(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.put("Quest", this.quest == null ? ByteTag.ZERO : this.quest.save(provider));
            tag.putInt("QuestIndex", this.nextIndex);
            return tag;
        }

        @Override
        public String toString() {
            return String.format("QuestState(%s, %s)", this.definition, this.nextIndex - 1);
        }
    }
}
