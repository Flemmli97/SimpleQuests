package io.github.flemmli97.simplequests_api.impls.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quest containing multiple quests that need to be fininshed
 */
public class SequentialQuest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuestsAPI.MODID, "sequential_quest");

    private final List<ResourceLocation> quests;

    private final ResourceLocation loot;
    private final String command;

    protected SequentialQuest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                              ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, boolean isDailyQuest, EntityPredicate unlockCondition,
                              List<ResourceLocation> compositeQuests, ResourceLocation loot, String command, Visibility visibility) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock, icon, repeatDelay, repeatDaily, sortingId, isDailyQuest, unlockCondition, visibility);
        this.quests = compositeQuests;
        this.loot = loot;
        this.command = command;
    }

    public static SequentialQuest of(ResourceLocation id, QuestCategory category, JsonObject obj) {
        return QuestBase.of(task -> {
            SequentialQuest.Builder builder = new SequentialQuest.Builder(id, task, new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")))
                    .withCommand(GsonHelper.getAsString(obj, "command", ""));
            JsonArray entries = GsonHelper.getAsJsonArray(obj, "quests");
            entries.forEach(ent -> builder.addQuest(new ResourceLocation(ent.getAsString())));
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
        JsonArray entries = new JsonArray();
        this.quests.forEach(res -> entries.add(res.toString()));
        obj.add("quests", entries);
        obj.addProperty(QuestBase.TYPE_ID, ID.toString());
        return obj;
    }

    @Override
    public List<ResourceLocation> getSubQuests() {
        return this.quests;
    }

    @Override
    public String submissionTrigger(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.quests.size())
            return super.submissionTrigger(player, idx);
        QuestBase base = QuestsManager.instance().getQuest(this.quests.get(idx));
        return base.submissionTrigger(player, idx);
    }

    @Override
    @Nullable
    public QuestBase resolveToQuest(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.quests.size())
            return null;
        QuestBase quest = QuestsManager.instance().getQuest(this.quests.get(idx));
        if (quest == null)
            return null;
        if (quest.getSubQuests().size() > 1) {
            SimpleQuestsAPI.LOGGER.error("SequentialQuest {} does not support nested quest {}", this.id, quest.id);
            return null;
        }
        return quest;
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
    public Map<String, ResolvedQuestTask> resolveTasks(PlayerQuestData data, int idx) {
        QuestBase base = this.resolveToQuest(data.getPlayer(), idx);
        return base == null ? Map.of() : base.resolveTasks(data, 0);
    }

    public static class Builder extends BuilderBase<Builder> {

        protected final List<ResourceLocation> compositeQuests = new ArrayList<>();
        protected final ResourceLocation loot;
        protected String command = "";

        public Builder(ResourceLocation id, String task, ResourceLocation loot) {
            super(id, task);
            this.loot = loot;
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        public Builder addQuest(ResourceLocation quest) {
            this.compositeQuests.add(quest);
            return this;
        }

        public Builder withCommand(String command) {
            this.command = command;
            return this;
        }

        @Override
        public SequentialQuest build() {
            SequentialQuest quest = new SequentialQuest(this.id, this.category, this.name, this.description, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.icon, this.repeatDelay, this.repeatDaily, this.sortingId, this.isDailyQuest,
                    this.unlockCondition, this.compositeQuests, this.loot, this.command, this.visibility);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }
}
