package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Quest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "quest");

    private final Map<String, QuestEntry> entries;

    public final ResourceLocation loot;
    public final String command;

    public final String questSubmissionTrigger;

    private Quest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                  ResourceLocation loot, ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, Map<String, QuestEntry> entries,
                  boolean isDailyQuest, String questSubmissionTrigger, EntityPredicate unlockCondition, String command) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock,
                icon, repeatDelay, repeatDaily, sortingId, isDailyQuest, unlockCondition);
        this.entries = entries;
        this.loot = loot;
        this.command = command;
        this.questSubmissionTrigger = questSubmissionTrigger;
    }

    public static Quest of(ResourceLocation id, QuestCategory category, JsonObject obj) {
        ImmutableMap.Builder<String, QuestEntry> builder = new ImmutableMap.Builder<>();
        JsonObject entries = GsonHelper.getAsJsonObject(obj, "entries");
        entries.entrySet().forEach(ent -> {
            if (!ent.getValue().isJsonObject())
                throw new JsonSyntaxException("Expected JsonObject for " + ent.getKey() + " but was " + ent.getValue());
            ResourceLocation entryID = new ResourceLocation(GsonHelper.getAsString(ent.getValue().getAsJsonObject(), "id"));
            builder.put(ent.getKey(), QuestEntryRegistry.deserialize(entryID, ent.getValue().getAsJsonObject()));
        });
        ImmutableList.Builder<ResourceLocation> parents = new ImmutableList.Builder<>();
        JsonElement e = obj.get("parent_id");
        if (e != null) {
            if (e.isJsonPrimitive() && !e.getAsString().isEmpty())
                parents.add(new ResourceLocation(e.getAsString()));
            else if (e.isJsonArray()) {
                e.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        parents.add(new ResourceLocation(ea.getAsString()));
                    }
                });
            }
        }
        ImmutableList.Builder<String> desc = new ImmutableList.Builder<>();
        JsonElement descEl = obj.get("description");
        if (descEl != null) {
            if (descEl.isJsonPrimitive() && !descEl.getAsString().isEmpty())
                desc.add(descEl.getAsString());
            else if (descEl.isJsonArray()) {
                descEl.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        desc.add(ea.getAsString());
                    }
                });
            }
        }
        return new Quest(id,
                category,
                GsonHelper.getAsString(obj, "task"),
                desc.build(),
                parents.build(),
                GsonHelper.getAsBoolean(obj, "redo_parent", false),
                GsonHelper.getAsBoolean(obj, "need_unlock", false),
                new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")),
                ParseHelper.icon(obj, "icon", Items.PAPER),
                ParseHelper.tryParseTime(obj, "repeat_delay", 0),
                GsonHelper.getAsInt(obj, "repeat_daily", 0),
                GsonHelper.getAsInt(obj, "sorting_id", 0),
                builder.build(),
                GsonHelper.getAsBoolean(obj, "daily_quest", false),
                GsonHelper.getAsString(obj, "submission_trigger", ""),
                EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "unlock_condition", null)),
                GsonHelper.getAsString(obj, "command", ""));
    }

    @Override
    public JsonObject serialize(boolean withId, boolean full) {
        SimpleQuests.logger.debug("Serializing " + ID + " with id " + this.id);
        JsonObject obj = super.serialize(withId, full);
        obj.addProperty("loot_table", this.loot.toString());
        if (!this.command.isEmpty() || full)
            obj.addProperty("command", this.command);
        if (!this.questSubmissionTrigger.isEmpty() || full)
            obj.addProperty("submission_trigger", this.questSubmissionTrigger);
        JsonObject entries = new JsonObject();
        SimpleQuests.logger.debug("Serializing " + this.id);
        this.entries.forEach((res, entry) -> entries.add(res, QuestEntryRegistry.CODEC.encodeStart(JsonOps.INSTANCE, entry).getOrThrow(false, e -> SimpleQuests.logger.error("Couldn't save quest entry" + e))));
        obj.add("entries", entries);
        obj.addProperty("type", ID.toString());
        return obj;
    }

    @Override
    public List<MutableComponent> getFormattedGuiTasks(ServerPlayer player) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestEntry> e : this.entries.entrySet()) {
            if (!(e.getValue() instanceof QuestEntryImpls.ItemEntry ing))
                list.add(new TextComponent(" - ").append(e.getValue().translation(player)));
            else {
                List<MutableComponent> wrapped = SimpleQuests.getHandler().wrapForGui(player, ing);
                boolean start = true;
                for (MutableComponent comp : wrapped) {
                    if (start) {
                        list.add(new TextComponent(" - ").append(comp));
                        start = false;
                    } else
                        list.add(new TextComponent("   ").append(comp));
                }
            }
        }
        return list;
    }

    @Override
    public Quest resolveToQuest(ServerPlayer player, int idx) {
        return this;
    }

    public Map<String, QuestEntry> resolveTasks(ServerPlayer player) {
        ImmutableMap.Builder<String, QuestEntry> builder = new ImmutableMap.Builder<>();
        for (Map.Entry<String, QuestEntry> i : this.entries.entrySet()) {
            builder.put(i.getKey(), i.getValue().resolve(player));
        }
        return builder.build();
    }

    public static class Builder extends BuilderBase<Builder> {

        private final Map<String, QuestEntry> entries = new LinkedHashMap<>();

        private final ResourceLocation loot;

        private String submissionTrigger = "";

        private String command = "";

        public Builder(ResourceLocation id, String task, ResourceLocation loot) {
            super(id, task);
            this.loot = loot;
        }

        public Builder addTaskEntry(String name, QuestEntry entry) {
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
            Quest quest = new Quest(this.id, this.category, this.questTaskString, this.questDesc, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.loot, this.icon, this.repeatDelay, this.repeatDaily, this.sortingId, this.entries, this.isDailyQuest,
                    this.submissionTrigger, this.unlockCondition, this.command);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }
}
