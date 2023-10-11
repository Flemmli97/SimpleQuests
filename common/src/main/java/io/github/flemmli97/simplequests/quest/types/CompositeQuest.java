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
        return QuestBase.of(task -> {
            CompositeQuest.Builder builder = new CompositeQuest.Builder(id, task);
            JsonArray entries = GsonHelper.getAsJsonArray(obj, "quests");
            entries.forEach(ent -> builder.addQuest(new ResourceLocation(ent.getAsString())));
            return builder;
        }, category, obj).build();
    }

    @Override
    public JsonObject serialize(boolean withId, boolean full) {
        SimpleQuests.LOGGER.debug("Serializing " + ID + " with id " + this.id);
        JsonObject obj = super.serialize(withId, full);
        JsonArray entries = new JsonArray();
        this.compositeQuests.forEach(res -> entries.add(res.toString()));
        obj.add("quests", entries);
        obj.addProperty(QuestBase.TYPE_ID, ID.toString());
        return obj;
    }

    public List<ResourceLocation> getCompositeQuests() {
        return this.compositeQuests;
    }

    @Override
    @Nullable
    public QuestBase resolveToQuest(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return null;
        return QuestsManager.instance().getAllQuests().get(this.compositeQuests.get(idx));
    }

    @Override
    public ResourceLocation getLoot() {
        return null;
    }

    @Override
    public void onComplete(ServerPlayer player) {
    }

    @Override
    public String submissionTrigger(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return super.submissionTrigger(player, idx);
        QuestBase base = QuestsManager.instance().getAllQuests().get(this.compositeQuests.get(idx));
        return base.submissionTrigger(player, idx);
    }

    @Override
    public Map<String, QuestEntry> resolveTasks(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return Map.of();
        QuestBase base = QuestsManager.instance().getAllQuests().get(this.compositeQuests.get(idx));
        return base.resolveTasks(player, 0);
    }

    public static class Builder extends BuilderBase<Builder> {

        protected final List<ResourceLocation> compositeQuests = new ArrayList<>();

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
