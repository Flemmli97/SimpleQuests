package io.github.flemmli97.simplequests_api.impls.quests;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Quest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuestsAPI.MODID, "quest");

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

    public static Quest of(ResourceLocation id, QuestCategory category, JsonObject obj) {
        return QuestBase.of(task -> {
            Quest.Builder builder = new Builder(id, task, new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")))
                    .withSubmissionTrigger(GsonHelper.getAsString(obj, "submission_trigger", ""))
                    .setCompletionCommand(GsonHelper.getAsString(obj, "command", ""));
            JsonObject entries = GsonHelper.getAsJsonObject(obj, "entries");
            entries.entrySet().forEach(ent -> {
                if (!ent.getValue().isJsonObject())
                    throw new JsonSyntaxException("Expected JsonObject for " + ent.getKey() + " but was " + ent.getValue());
                ResourceLocation entryID = new ResourceLocation(GsonHelper.getAsString(ent.getValue().getAsJsonObject(), "id"));
                builder.addTaskEntry(ent.getKey(), QuestEntryRegistry.deserialize(entryID, ent.getValue().getAsJsonObject()));
            });
            return builder;
        }, category, obj).build();
    }

    @Override
    public JsonObject serialize(boolean withId, boolean full) {
        SimpleQuestsAPI.LOGGER.debug("Serializing {} with id {}", ID, this.id);
        JsonObject obj = super.serialize(withId, full);
        obj.addProperty("loot_table", this.loot.toString());
        if (!this.command.isEmpty() || full)
            obj.addProperty("command", this.command);
        if (!this.questSubmissionTrigger.isEmpty() || full)
            obj.addProperty("submission_trigger", this.questSubmissionTrigger);
        JsonObject entries = new JsonObject();
        this.entries.forEach((res, entry) -> entries.add(res, QuestEntryRegistry.ENTRY_CODEC.encodeStart(JsonOps.INSTANCE, entry).getOrThrow(false, e -> SimpleQuestsAPI.LOGGER.error("Couldn't save quest entry {}", e))));
        obj.add("entries", entries);
        obj.addProperty(QuestBase.TYPE_ID, ID.toString());
        return obj;
    }

    @Override
    public List<MutableComponent> getTasks(ServerPlayer player, Style taskStyle) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestTask<?>> e : this.entries.entrySet()) {
            list.add(Component.translatable(SimpleQuestsAPI.MODID + ".task.formatter", e.getValue().translation(player).withStyle(taskStyle)));
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

    public static class Builder extends BuilderBase<Builder> {

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
}
