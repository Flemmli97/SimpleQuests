package io.github.flemmli97.simplequests.quest.types;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.quest.ParseHelper;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
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

public abstract class QuestBase implements Comparable<QuestBase> {

    public static final String TYPE_ID = "type";

    public final ResourceLocation id;
    public final QuestCategory category;
    public final List<ResourceLocation> neededParentQuests;

    public final int repeatDelay, repeatDaily;

    protected final String questTaskString;
    protected final List<String> questTaskDesc;

    public final boolean redoParent, needsUnlock, isDailyQuest;

    public final int sortingId;

    private final ItemStack icon;

    private String repeatDelayString;

    protected final EntityPredicate unlockCondition;

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
            list.add(Component.literal(" - ").append(e.getValue().translation(player)));
        }
        return list;
    }

    public static void runCommand(ServerPlayer player, String command) {
        if (!command.isEmpty())
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
    }

    public static <B extends BuilderBase<B>> B of(Function<String, B> questBuilder,
                                                  QuestCategory category, JsonObject obj) {
        B questbuilder = questBuilder.apply(GsonHelper.getAsString(obj, "task"));
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
        questbuilder.withIcon(ParseHelper.icon(obj, "icon", Items.PAPER));
        questbuilder.setRepeatDelay(ParseHelper.tryParseTime(obj, "repeat_delay", 0));
        questbuilder.setMaxDaily(GsonHelper.getAsInt(obj, "repeat_daily", 0));
        questbuilder.withSortingNum(GsonHelper.getAsInt(obj, "sorting_id", 0));
        if (GsonHelper.getAsBoolean(obj, "daily_quest", false))
            questbuilder.setDailyQuest();
        questbuilder.withUnlockCondition(EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "unlock_condition", null)));
        return questbuilder;
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

    public boolean isUnlocked(ServerPlayer player) {
        return this.unlockCondition.matches(player, player);
    }

    public final MutableComponent getTask(ServerPlayer player) {
        return this.getTask(player, -1);
    }

    /**
     * The quest task to do. Delegates to subquests if possible
     *
     * @param idx If -1 should return itself
     */
    public MutableComponent getTask(ServerPlayer player, int idx) {
        QuestBase resolved = this.resolveToQuest(player, idx);
        if (resolved == null)
            return Component.translatable(this.questTaskString);
        return resolved.getTask(player);
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
            return this.questTaskDesc.stream().map(s -> Component.translatable(s).withStyle(ChatFormatting.DARK_GREEN)).collect(Collectors.toList());
        return resolved.getDescription(player);
    }

    /**
     * The formatted quest with the given tasks. Delegates to subquests if possible
     *
     * @param idx If -1 should return itself
     */
    public MutableComponent getFormattedWith(ServerPlayer player, int idx, Map<String, QuestEntry> resolvedTasks, ChatFormatting... subFormatting) {
        QuestBase resolved = this.resolveToQuest(player, idx);
        if (resolved != null)
            return this.getFormattedWith(player, -1, resolvedTasks, subFormatting);
        MutableComponent main = this.getTask(player, idx).withStyle(ChatFormatting.LIGHT_PURPLE);
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
            LootTable lootTable = player.getServer().getLootTables().get(lootID);
            CriteriaTriggers.GENERATE_LOOT.trigger(player, lootID);
            LootContext.Builder builder = new LootContext.Builder(player.getLevel())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, player.damageSources().magic())
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck());
            List<ItemStack> loot = lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY));
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

    public abstract Map<String, QuestEntry> resolveTasks(ServerPlayer player, int questIndex);

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
