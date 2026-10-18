package io.github.flemmli97.simplequests_api.impls.quests;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionImpl;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionRef;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.TimeDuration;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A quest multiple tasks to complete
 */
public class TasksQuestDefinition extends QuestDefinitionImpl {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "quest");

    public static final MapCodec<TasksQuestDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(true)),
            ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(d -> d.loot == null ? Optional.empty() : Optional.of(d.loot.location())),
            Codec.STRING.optionalFieldOf("command").forGetter(d -> Optional.of(d.command)),
            Codec.STRING.optionalFieldOf("submission_trigger").forGetter(d -> Optional.of(d.trigger)),
            Codec.unboundedMap(Codec.STRING, QuestEntryRegistry.ENTRY_CODEC).fieldOf("tasks").forGetter(d -> d.tasks)
    ).apply(instance, (data, loot, command, trigger, tasks) -> {
        Builder builder = data.applyTo(name -> new Builder(name, loot.orElse(null)));
        tasks.forEach(builder::addTaskEntry);
        builder.withSubmissionTrigger(trigger.orElse(""));
        builder.setCompletionCommand(command.orElse(""));
        return builder.build();
    }));

    public static final MapCodec<TasksQuestDefinition> OPT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(false)),
            ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(d -> d.loot == null ? Optional.empty() : Optional.of(d.loot.location())),
            Codec.STRING.optionalFieldOf("command").forGetter(d -> !d.command.isEmpty() ? Optional.of(d.command) : Optional.empty()),
            Codec.STRING.optionalFieldOf("submission_trigger").forGetter(d -> !d.trigger.isEmpty() ? Optional.of(d.trigger) : Optional.empty()),
            Codec.unboundedMap(Codec.STRING, QuestEntryRegistry.ENTRY_CODEC).fieldOf("tasks").forGetter(d -> d.tasks)
    ).apply(instance, (data, loot, command, trigger, tasks) -> {
        Builder builder = data.applyTo(name -> new Builder(name, loot.orElse(null)));
        tasks.forEach(builder::addTaskEntry);
        builder.withSubmissionTrigger(trigger.orElse(""));
        builder.setCompletionCommand(command.orElse(""));
        return builder.build();
    }));

    private final Map<String, QuestTask<?>> tasks;

    @Nullable
    private final ResourceKey<LootTable> loot;
    private final String command;

    private final String trigger;

    protected TasksQuestDefinition(QuestCategory category, String name, List<String> description,
                                   List<ResourceLocation> requiredParentQuests, boolean redoParent,
                                   TimeDuration repeatDelay, int repeatDaily, int maxRepeat, boolean isDailyQuest,
                                   boolean needsUnlock, EntityPredicate unlockCondition, int sortingId, ItemStack icon, Visibility visibility,
                                   Map<String, QuestTask<?>> tasks,
                                   ResourceLocation loot, String command, String trigger) {
        super(category, name, description, requiredParentQuests, redoParent, repeatDelay, repeatDaily, maxRepeat, isDailyQuest, needsUnlock, unlockCondition, sortingId, icon, visibility);
        this.tasks = tasks;
        this.loot = loot == null ? null : ResourceKey.create(Registries.LOOT_TABLE, loot);
        this.command = command;
        this.trigger = trigger;
    }

    @Override
    public MapCodec<? extends QuestDefinition> codec() {
        return CODEC;
    }

    @Override
    public ResourceLocation typeId() {
        return ID;
    }

    @Override
    public MapCodec<? extends QuestDefinition> simplifiedCodec() {
        return OPT_CODEC;
    }

    @Override
    public List<MutableComponent> tasksDescription(ServerPlayer player, Style taskStyle) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestTask<?>> e : this.tasks.entrySet()) {
            list.add(Component.translatable(SimpleQuestsAPI.MODID + ".task.formatter", e.getValue().translation(player).withStyle(taskStyle)));
        }
        return list;
    }

    @Override
    public Map<String, QuestTask<?>> unresolvedTasks() {
        return ImmutableMap.copyOf(this.tasks);
    }

    @Override
    public ResolvedQuest resolve(QuestProgress progress) {
        return new TaskQuestDefinitionRef(this);
    }

    @Override
    public ResolvedQuest load(QuestProgress progress, Tag tag, HolderLookup.Provider provider) {
        return new TaskQuestDefinitionRef(this);
    }

    public static class TaskQuestDefinitionRef extends QuestDefinitionRef<TasksQuestDefinition> {

        public TaskQuestDefinitionRef(TasksQuestDefinition definition) {
            super(definition);
        }

        @Override
        public Map<String, ResolvedQuestTask> resolveTasks(QuestProgress progress) {
            ImmutableMap.Builder<String, ResolvedQuestTask> builder = new ImmutableMap.Builder<>();
            for (Map.Entry<String, QuestTask<?>> entry : this.source().tasks.entrySet()) {
                ResolvedQuestTask task = entry.getValue().resolve(progress, this);
                if (task != null)
                    builder.put(entry.getKey(), task);
            }
            return builder.build();
        }

        @Override
        public String submissionTrigger(ServerPlayer player) {
            return this.source().trigger;
        }

        @Override
        public void onComplete(ServerPlayer player) {
            if (this.source().loot != null) {
                ResolvedQuest.dropLoot(player, this.source().loot);
            }
            ResolvedQuest.runCommand(player, this.source().command);
        }
    }

    public static class Builder extends BuilderBase<TasksQuestDefinition, Builder> {

        protected final Map<String, QuestTask<?>> entries = new LinkedHashMap<>();

        protected final ResourceLocation loot;
        protected String command = "";
        protected String trigger = "";

        public Builder(String name) {
            this(name, null);
        }

        public Builder(String name, ResourceLocation loot) {
            super(name);
            this.loot = loot;
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        public Builder addTaskEntry(String name, QuestTask<?> entry) {
            this.entries.put(name, entry);
            return this;
        }

        public Builder withSubmissionTrigger(String trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder setCompletionCommand(String command) {
            this.command = command;
            return this;
        }

        @Override
        public TasksQuestDefinition build() {
            return new TasksQuestDefinition(this.category, this.name, this.description,
                    this.requiredParentQuests, this.redoParent,
                    this.repeatDelay, this.repeatDaily, this.maxRepeat,
                    this.isDailyQuest, this.needsUnlock, this.unlockCondition, this.sortingId, this.icon, this.visibility,
                    this.entries, this.loot, this.command, this.trigger);
        }
    }
}
