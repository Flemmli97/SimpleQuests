package io.github.flemmli97.simplequests_api.impls.quests;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class Quest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuestsAPI.MODID, "quest");

    public static final BiFunction<Boolean, Boolean, Codec<Quest>> CODEC = Util.memoize((withId, full) ->
            QuestBase.buildCodec(QuestData.CODEC
                    .forGetter(q -> new QuestData(q.loot,
                            q.command.isEmpty() || full ? Optional.of(q.command) : Optional.empty(),
                            q.questSubmissionTrigger.isEmpty() || full ? Optional.of(q.questSubmissionTrigger) : Optional.empty(),
                            q.entries)), withId, full, (id, task, data) -> {
                Builder builder = new Builder(id, task, data.loot);
                data.entries.forEach(builder::addTaskEntry);
                builder.withSubmissionTrigger(data.questSubmissionTrigger.orElse(""));
                builder.setCompletionCommand(data.command.orElse(""));
                return builder;
            }));

    private final Map<String, QuestTask<?>> entries;

    private final ResourceLocation loot;
    private final String command;

    public final String questSubmissionTrigger;

    protected Quest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                    ResourceLocation loot, ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, Map<String, QuestTask<?>> entries,
                    boolean isDailyQuest, String questSubmissionTrigger, EntityPredicate unlockCondition, String command, Visibility visibility) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock,
                icon, repeatDelay, repeatDaily, sortingId, isDailyQuest, unlockCondition, visibility);
        this.entries = entries;
        this.loot = loot;
        this.command = command;
        this.questSubmissionTrigger = questSubmissionTrigger;
    }

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public List<MutableComponent> getTasks(ServerPlayer player, Style taskStyle) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestTask<?>> e : this.entries.entrySet()) {
            list.add(new TranslatableComponent(SimpleQuestsAPI.MODID + ".task.formatter", e.getValue().translation(player).withStyle(taskStyle)));
        }
        return list;
    }

    @Override
    public String submissionTrigger(ServerPlayer player, int idx) {
        return this.questSubmissionTrigger;
    }

    @Override
    public QuestBase resolveToQuest(ServerPlayer player, int idx) {
        if (idx != 0)
            return null;
        return this;
    }

    @Override
    public ResourceLocation getLoot() {
        return this.loot;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        super.onComplete(player);
        QuestBase.runCommand(player, this.command);
    }

    @Override
    public Map<String, ResolvedQuestTask> resolveTasks(PlayerQuestData data, QuestProgress progress, int questIndex) {
        ImmutableMap.Builder<String, ResolvedQuestTask> builder = new ImmutableMap.Builder<>();
        for (Map.Entry<String, QuestTask<?>> i : this.entries.entrySet()) {
            ResolvedQuestTask task = i.getValue().resolve(data, progress, this);
            if (task != null)
                builder.put(i.getKey(), task);
        }
        return builder.build();
    }

    public static class Builder extends BuilderBase<Quest, Builder> {

        protected final Map<String, QuestTask<?>> entries = new LinkedHashMap<>();

        protected final ResourceLocation loot;

        protected String submissionTrigger = "";

        protected String command = "";

        public Builder(ResourceLocation id, String task, ResourceLocation loot) {
            super(id, task);
            this.loot = loot;
        }

        public Builder addTaskEntry(String name, QuestTask<?> entry) {
            this.entries.put(name, entry);
            return this;
        }

        public Builder withSubmissionTrigger(String trigger) {
            this.submissionTrigger = trigger;
            return this;
        }

        public Builder setCompletionCommand(String command) {
            this.command = command;
            return this;
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        @Override
        public Quest build() {
            Quest quest = new Quest(this.id, this.category, this.name, this.description, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.loot, this.icon, this.repeatDelay, this.repeatDaily, this.sortingId, this.entries, this.isDailyQuest,
                    this.submissionTrigger, this.unlockCondition, this.command, this.visibility);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }

    private record QuestData(ResourceLocation loot, Optional<String> command,
                             Optional<String> questSubmissionTrigger, Map<String, QuestTask<?>> entries) {
        static final MapCodec<QuestData> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                        ResourceLocation.CODEC.fieldOf("loot_table").forGetter(d -> d.loot),
                        Codec.STRING.optionalFieldOf("command").forGetter(d -> d.command),
                        Codec.STRING.optionalFieldOf("submission_trigger").forGetter(d -> d.questSubmissionTrigger),
                        Codec.unboundedMap(Codec.STRING, QuestEntryRegistry.ENTRY_CODEC).fieldOf("entries").forGetter(d -> d.entries)
                ).apply(inst, QuestData::new)
        );
    }
}
