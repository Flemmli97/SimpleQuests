package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CompositeQuest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "composite_quest");

    private final List<ResourceLocation> compositeQuests;

    protected CompositeQuest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                             ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, boolean isDailyQuest, EntityPredicate unlockCondition,
                             List<ResourceLocation> compositeQuests) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock, icon, repeatDelay, repeatDaily, sortingId, isDailyQuest, unlockCondition);
        this.compositeQuests = compositeQuests;
    }

    public static CompositeQuest of(ResourceLocation id, QuestCategory category, JsonObject obj) {
        ImmutableList.Builder<ResourceLocation> builder = new ImmutableList.Builder<>();
        JsonArray entries = GsonHelper.getAsJsonArray(obj, "quests");
        entries.forEach(ent -> builder.add(new ResourceLocation(ent.getAsString())));
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
        return new CompositeQuest(id,
                category,
                GsonHelper.getAsString(obj, "task"),
                desc.build(),
                parents.build(),
                GsonHelper.getAsBoolean(obj, "redo_parent", false),
                GsonHelper.getAsBoolean(obj, "need_unlock", false),
                ParseHelper.icon(obj, "icon", Items.PAPER),
                ParseHelper.tryParseTime(obj, "repeat_delay", 0),
                GsonHelper.getAsInt(obj, "repeat_daily", 0),
                GsonHelper.getAsInt(obj, "sorting_id", 0),
                GsonHelper.getAsBoolean(obj, "daily_quest", false),
                EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "unlock_condition", null)),
                builder.build());
    }

    @Override
    public JsonObject serialize(boolean withId, boolean full) {
        SimpleQuests.logger.debug("Serializing " + ID + " with id " + this.id);
        JsonObject obj = super.serialize(withId, full);
        JsonArray entries = new JsonArray();
        this.compositeQuests.forEach(res -> entries.add(res.toString()));
        obj.add("quests", entries);
        obj.addProperty("type", ID.toString());
        return obj;
    }

    public List<ResourceLocation> getCompositeQuests() {
        return this.compositeQuests;
    }

    @Override
    @Nullable
    public Quest resolveToQuest(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return null;
        QuestBase base = QuestsManager.instance().getAllQuests().get(this.compositeQuests.get(idx));
        return base instanceof Quest quest ? quest : null;
    }

    public static class Builder extends BuilderBase<Builder> {

        private final List<ResourceLocation> compositeQuests = new ArrayList<>();

        public Builder(ResourceLocation id, String task) {
            super(id, task);
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        public Builder addQuest(ResourceLocation quest) {
            this.compositeQuests.add(quest);
            return this;
        }

        @Override
        public CompositeQuest build() {
            CompositeQuest quest = new CompositeQuest(this.id, this.category, this.questTaskString, this.questDesc, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.icon, this.repeatDelay, this.repeatDaily, this.sortingId, this.isDailyQuest,
                    this.unlockCondition, this.compositeQuests);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }
}
