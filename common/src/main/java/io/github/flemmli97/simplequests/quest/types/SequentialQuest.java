package io.github.flemmli97.simplequests.quest.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SequentialQuest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "sequential_quest");

    private final List<ResourceLocation> quests;

    private final ResourceLocation loot;
    private final String command;

    protected SequentialQuest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                              ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, boolean isDailyQuest, EntityPredicate unlockCondition,
                              List<ResourceLocation> compositeQuests, ResourceLocation loot, String command) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock, icon, repeatDelay, repeatDaily, sortingId, isDailyQuest, unlockCondition);
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
        SimpleQuests.LOGGER.debug("Serializing " + ID + " with id " + this.id);
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

    public List<ResourceLocation> getQuests() {
        return this.quests;
    }

    @Override
    public String submissionTrigger(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.quests.size())
            return super.submissionTrigger(player, idx);
        QuestBase base = QuestsManager.instance().getAllQuests().get(this.quests.get(idx));
        return base.submissionTrigger(player, idx);
    }

    @Override
    @Nullable
    public QuestBase resolveToQuest(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.quests.size())
            return null;
        return QuestsManager.instance().getAllQuests().get(this.quests.get(idx));
    }

    @Override
    public ResourceLocation getLoot() {
        return this.loot;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        QuestBase.runCommand(player, this.command);
    }

    @Override
    public Map<String, QuestEntry> resolveTasks(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.quests.size())
            return Map.of();
        QuestBase base = QuestsManager.instance().getAllQuests().get(this.quests.get(idx));
        return base.resolveTasks(player, 0);
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
            SequentialQuest quest = new SequentialQuest(this.id, this.category, this.questTaskString, this.questDesc, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.icon, this.repeatDelay, this.repeatDaily, this.sortingId, this.isDailyQuest,
                    this.unlockCondition, this.compositeQuests, this.loot, this.command);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }
}
