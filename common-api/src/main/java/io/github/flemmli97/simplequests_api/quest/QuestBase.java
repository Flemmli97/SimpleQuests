package io.github.flemmli97.simplequests_api.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for quests. If you define a new quest type register under {@link QuestBaseRegistry#registerSerializer}
 */
public abstract class QuestBase implements Comparable<QuestBase> {

    public static final String TYPE_ID = "type";

    public final ResourceLocation id;
    public final QuestCategory category;
    public final List<ResourceLocation> neededParentQuests;

    public final int repeatDelay, repeatDaily;

    protected final String name;
    protected final List<String> description;

    public final boolean redoParent, needsUnlock, isDailyQuest;

    public final int sortingId;

    private final ItemStack icon;

    private String repeatDelayString;

    protected final EntityPredicate unlockCondition;

    public final Visibility visibility;

    public QuestBase(ResourceLocation id, QuestCategory category, String name, List<String> description, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                     ItemStack icon, int repeatDelay, int repeatDaily, int sortingId,
                     boolean isDailyQuest, EntityPredicate unlockCondition, Visibility visibility) {
        this.id = id;
        this.category = category == null ? QuestCategory.DEFAULT_CATEGORY : category;
        this.name = name;
        this.description = description;
        this.neededParentQuests = parents;
        this.redoParent = redoParent;
        this.needsUnlock = needsUnlock;
        this.repeatDelay = repeatDelay;
        this.repeatDaily = repeatDaily;
        this.sortingId = sortingId;
        this.icon = icon;
        this.isDailyQuest = isDailyQuest;
        this.unlockCondition = unlockCondition;
        this.visibility = visibility;
    }

    public static void runCommand(ServerPlayer player, String command) {
        if (!command.isEmpty())
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
    }

    public static <B extends BuilderBase<B>> B of(Function<String, B> questBuilder,
                                                  QuestCategory category, JsonObject obj) {
        B questbuilder = questBuilder.apply(GsonHelper.getAsString(obj, "name"));
        questbuilder.withCategory(category);
        JsonElement descEl = obj.get("description");
        if (descEl != null) {
            if (descEl.isJsonPrimitive() && !descEl.getAsString().isEmpty())
                questbuilder.addDescription(descEl.getAsString());
            else if (descEl.isJsonArray()) {
                descEl.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        questbuilder.addDescription(ea.getAsString());
                    }
                });
            }
        }
        JsonElement e = obj.get("parent_id");
        if (e != null) {
            if (e.isJsonPrimitive() && !e.getAsString().isEmpty())
                questbuilder.addParent(new ResourceLocation(e.getAsString()));
            else if (e.isJsonArray()) {
                e.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        questbuilder.addParent(new ResourceLocation(ea.getAsString()));
                    }
                });
            }
        }
        if (GsonHelper.getAsBoolean(obj, "redo_parent", false))
            questbuilder.setRedoParent();
        if (GsonHelper.getAsBoolean(obj, "need_unlock", false))
            questbuilder.needsUnlocking();
        questbuilder.withIcon(QuestUtils.icon(obj, "icon", Items.PAPER));
        questbuilder.setRepeatDelay(QuestUtils.tryParseTime(obj, "repeat_delay", 0));
        questbuilder.setMaxDaily(GsonHelper.getAsInt(obj, "repeat_daily", 0));
        questbuilder.withSortingNum(GsonHelper.getAsInt(obj, "sorting_id", 0));
        if (GsonHelper.getAsBoolean(obj, "daily_quest", false))
            questbuilder.setDailyQuest();
        questbuilder.withUnlockCondition(EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "unlock_condition", null)));
        questbuilder.setVisibility(Visibility.valueOf(GsonHelper.getAsString(obj, "visibility", Visibility.DEFAULT.toString())));
        return questbuilder;
    }

    public JsonObject serialize(boolean withId, boolean full) {
        JsonObject obj = new JsonObject();
        if (withId)
            obj.addProperty("id", this.id.toString());
        if (this.category != QuestCategory.DEFAULT_CATEGORY)
            obj.addProperty("category", this.category.id.toString());
        obj.addProperty("name", this.name);
        if (!this.description.isEmpty() || full) {
            if (this.description.size() == 1)
                obj.addProperty("description", this.description.get(0));
            else {
                JsonArray arr = new JsonArray();
                this.description.forEach(arr::add);
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
        QuestUtils.writeItemStackToJson(this.icon, full ? null : Items.PAPER)
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
        if (this.visibility != Visibility.DEFAULT || full)
            obj.addProperty("visibility", this.visibility.toString());
        return obj;
    }

    public boolean isUnlocked(ServerPlayer player) {
        return this.unlockCondition.matches(player, player);
    }

    public Visibility getVisibility() {
        if (QuestsManager.instance().isSubQuest(this.id))
            return Visibility.NEVER;
        if (this.visibility == Visibility.DEFAULT && !this.category.isVisible)
            return Visibility.NEVER;
        return this.visibility;
    }

    public final MutableComponent getName(ServerPlayer player) {
        return this.getName(player, -1);
    }

    /**
     * The quest task to do. Delegates to subquests if possible
     *
     * @param idx If -1 should return itself
     */
    public MutableComponent getName(ServerPlayer player, int idx) {
        QuestBase resolved = this.resolveToQuest(player, idx);
        if (resolved == null)
            return Component.translatable(this.name);
        return resolved.getName(player);
    }

    public final List<MutableComponent> getDescription(ServerPlayer player) {
        return this.getDescription(player, -1);
    }

    /**
     * The quest description. Delegates to subquests if possible
     *
     * @param idx If -1 should return itself
     */
    public List<MutableComponent> getDescription(ServerPlayer player, int idx) {
        QuestBase resolved = this.resolveToQuest(player, idx);
        if (resolved == null)
            return this.description.stream().map(Component::translatable).collect(Collectors.toList());
        return resolved.getDescription(player);
    }

    public List<MutableComponent> getTasks(ServerPlayer player, Style taskStyle) {
        return List.of();
    }

    public ItemStack getIcon() {
        return this.icon.copy();
    }

    public List<ResourceLocation> getSubQuests() {
        return List.of();
    }

    /**
     * For datageneration
     */
    protected void setDelayString(String repeatDelayString) {
        this.repeatDelayString = repeatDelayString;
    }

    /**
     * The trigger required to complete this quest
     */
    public String submissionTrigger(ServerPlayer player, int idx) {
        return "";
    }

    @Nullable
    public abstract QuestBase resolveToQuest(ServerPlayer player, int idx);

    public abstract ResourceLocation getLoot();

    public void onComplete(ServerPlayer player) {
        ResourceLocation lootID = this.getLoot();
        if (lootID != null) {
            LootTable lootTable = player.getServer().getLootData().getLootTable(lootID);
            CriteriaTriggers.GENERATE_LOOT.trigger(player, lootID);
            LootParams params = new LootParams.Builder(player.serverLevel())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, player.damageSources().magic())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.ENTITY);
            List<ItemStack> loot = lootTable.getRandomItems(params);
            loot.forEach(stack -> {
                boolean bl = player.getInventory().add(stack);
                if (!bl || !stack.isEmpty()) {
                    ItemEntity itemEntity = player.drop(stack, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setThrower(player.getUUID());
                    }
                }
            });
        }
    }

    public void onReset(ServerPlayer player) {
    }

    public abstract Map<String, ResolvedQuestTask> resolveTasks(PlayerQuestData data, QuestProgress progress, int questIndex);

    public boolean isDynamic() {
        return false;
    }

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

        protected final String name;
        protected final List<String> description = new ArrayList<>();

        protected boolean redoParent, needsUnlock, isDailyQuest;

        protected int sortingId;

        protected EntityPredicate unlockCondition = EntityPredicate.ANY;

        protected ItemStack icon = new ItemStack(Items.PAPER);

        protected Visibility visibility = Visibility.DEFAULT;

        protected BuilderBase(ResourceLocation id, String name) {
            this.id = id;
            this.name = name;
        }

        public T addDescription(String desc) {
            this.description.add(desc);
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
            this.repeatDelay = QuestUtils.tryParseTime(this.repeatDelayString, this.repeatDelayString);
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

        public T setVisibility(Visibility visibility) {
            this.visibility = visibility;
            return this.asThis();
        }

        protected abstract T asThis();

        public abstract QuestBase build();
    }

    public enum Visibility {
        DEFAULT,
        ALWAYS,
        NEVER
    }
}
