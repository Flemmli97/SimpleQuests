package io.github.flemmli97.simplequests.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.api.QuestEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class QuestBase implements Comparable<QuestBase> {

    public final ResourceLocation id;
    public final QuestCategory category;
    public final List<ResourceLocation> neededParentQuests;

    public final int repeatDelay, repeatDaily;

    private final String questTaskString;
    private final List<String> questTaskDesc;

    public final boolean redoParent, needsUnlock, isDailyQuest;

    public final int sortingId;

    private final ItemStack icon;

    private String repeatDelayString;

    public final EntityPredicate unlockCondition;

    public QuestBase(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                     ItemStack icon, int repeatDelay, int repeatDaily, int sortingId,
                     boolean isDailyQuest, EntityPredicate unlockCondition) {
        this.id = id;
        this.category = category == null ? QuestCategory.DEFAULT_CATEGORY : category;
        this.questTaskString = questTaskString;
        this.questTaskDesc = questTaskDesc;
        this.neededParentQuests = parents;
        this.redoParent = redoParent;
        this.needsUnlock = needsUnlock;
        this.repeatDelay = repeatDelay;
        this.repeatDaily = repeatDaily;
        this.sortingId = sortingId;
        this.icon = icon;
        this.isDailyQuest = isDailyQuest;
        this.unlockCondition = unlockCondition;
    }

    public static List<MutableComponent> getFormattedTasks(ServerPlayer player, Map<String, QuestEntry> resolvedTasks) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestEntry> e : resolvedTasks.entrySet()) {
            list.add(new TextComponent(" - ").append(e.getValue().translation(player)));
        }
        return list;
    }

    public JsonObject serialize(boolean withId, boolean full) {
        JsonObject obj = new JsonObject();
        if (withId)
            obj.addProperty("id", this.id.toString());
        if (this.category != QuestCategory.DEFAULT_CATEGORY)
            obj.addProperty("category", this.category.id.toString());
        obj.addProperty("task", this.questTaskString);
        if (!this.questTaskDesc.isEmpty() || full) {
            if (this.questTaskDesc.size() == 1)
                obj.addProperty("description", this.questTaskDesc.get(0));
            else {
                JsonArray arr = new JsonArray();
                this.questTaskDesc.forEach(arr::add);
                obj.add("description", arr);
            }
        }
        if (!this.neededParentQuests.isEmpty() || full) {
            if (this.neededParentQuests.size() == 1)
                obj.addProperty("parent_id", this.neededParentQuests.get(0).toString());
            else {
                JsonArray arr = new JsonArray();
                this.neededParentQuests.forEach(r -> arr.add(r.toString()));
                obj.add("parent_id", arr);
            }
        }
        if (this.redoParent || full)
            obj.addProperty("redo_parent", this.redoParent);
        if (this.needsUnlock || full)
            obj.addProperty("need_unlock", this.needsUnlock);
        if (this.unlockCondition != EntityPredicate.ANY || full)
            obj.add("unlock_condition", this.unlockCondition.serializeToJson());
        ParseHelper.writeItemStackToJson(this.icon, full ? null : Items.PAPER)
                .ifPresent(icon -> obj.add("icon", icon));
        if (this.repeatDelayString != null)
            obj.addProperty("repeat_delay", this.repeatDelayString);
        else if (this.repeatDelay != 0 || full)
            obj.addProperty("repeat_delay", this.repeatDelay);
        if (this.repeatDaily != 0 || full)
            obj.addProperty("repeat_daily", this.repeatDaily);
        if (this.sortingId != 0 || full)
            obj.addProperty("sorting_id", this.sortingId);
        if (this.isDailyQuest || full)
            obj.addProperty("daily_quest", this.isDailyQuest);
        return obj;
    }

    public MutableComponent getTask() {
        return new TranslatableComponent(this.questTaskString);
    }

    public List<MutableComponent> getDescription() {
        return this.questTaskDesc.stream().map(s -> new TranslatableComponent(s).withStyle(ChatFormatting.DARK_GREEN)).collect(Collectors.toList());
    }

    public MutableComponent getFormattedWith(ServerPlayer player, Map<String, QuestEntry> resolvedTasks, ChatFormatting... subFormatting) {
        MutableComponent main = new TextComponent("").append(this.getTask().withStyle(ChatFormatting.LIGHT_PURPLE));
        for (MutableComponent tasks : getFormattedTasks(player, resolvedTasks)) {
            if (subFormatting != null)
                main.append("\n").append(tasks.withStyle(subFormatting));
            else
                main.append("\n").append(tasks);
        }
        return main;
    }

    public List<MutableComponent> getFormattedGuiTasks(ServerPlayer player) {
        return List.of();
    }

    public ItemStack getIcon() {
        return this.icon.copy();
    }

    /**
     * For datageneration
     */
    protected void setDelayString(String repeatDelayString) {
        this.repeatDelayString = repeatDelayString;
    }

    public abstract Quest resolveToQuest(ServerPlayer player, int idx);

    @Override
    public String toString() {
        return String.format("[Quest:%s]", this.id);
    }

    @Override
    public int compareTo(@NotNull QuestBase quest) {
        if (this.sortingId == quest.sortingId) {
            if (this.neededParentQuests.isEmpty() && !quest.neededParentQuests.isEmpty())
                return -1;
            if (!this.neededParentQuests.isEmpty() && quest.neededParentQuests.isEmpty())
                return 1;
            return this.id.compareTo(quest.id);
        }
        return Integer.compare(this.sortingId, quest.sortingId);
    }

    public static abstract class BuilderBase<T extends BuilderBase<T>> {

        protected final ResourceLocation id;
        protected QuestCategory category = QuestCategory.DEFAULT_CATEGORY;
        protected final List<ResourceLocation> neededParentQuests = new ArrayList<>();

        protected int repeatDelay, repeatDaily;
        protected String repeatDelayString;

        protected final String questTaskString;
        protected final List<String> questDesc = new ArrayList<>();

        protected boolean redoParent, needsUnlock, isDailyQuest;

        protected int sortingId;

        protected EntityPredicate unlockCondition = EntityPredicate.ANY;

        protected ItemStack icon = new ItemStack(Items.PAPER);

        protected BuilderBase(ResourceLocation id, String task) {
            this.id = id;
            this.questTaskString = task;
        }

        public T addDescription(String desc) {
            this.questDesc.add(desc);
            return this.asThis();
        }

        public T withCategory(QuestCategory category) {
            this.category = category;
            return this.asThis();
        }

        public T addParent(ResourceLocation parent) {
            this.neededParentQuests.add(parent);
            return this.asThis();
        }

        public T setRedoParent() {
            this.redoParent = true;
            return this.asThis();
        }

        public T needsUnlocking() {
            this.needsUnlock = true;
            return this.asThis();
        }

        public T withIcon(ItemStack stack) {
            this.icon = stack;
            return this.asThis();
        }

        public T setRepeatDelay(int delay) {
            this.repeatDelay = delay;
            return this.asThis();
        }

        public T setRepeatDelay(String delay) {
            this.repeatDelayString = delay;
            this.repeatDelay = ParseHelper.tryParseTime(this.repeatDelayString, this.repeatDelayString);
            return this.asThis();
        }

        public T setMaxDaily(int max) {
            this.repeatDaily = max;
            return this.asThis();
        }

        public T withSortingNum(int num) {
            this.sortingId = num;
            return this.asThis();
        }

        public T setDailyQuest() {
            this.isDailyQuest = true;
            return this.asThis();
        }

        public T withUnlockCondition(EntityPredicate unlockCondition) {
            this.unlockCondition = unlockCondition;
            return this.asThis();
        }

        protected abstract T asThis();

        public abstract QuestBase build();
    }
}
