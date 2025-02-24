package io.github.flemmli97.simplequests_api.quest;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QuestCategory implements Comparable<QuestCategory> {

    public static final QuestCategory DEFAULT_CATEGORY = new QuestCategory(new ResourceLocation(SimpleQuestsAPI.MODID, "default_category"),
            "Main", List.of(), new ItemStack(Items.WRITTEN_BOOK), false, -1, -1, -1, List.of(), true, false);

    public final ResourceLocation id;
    private final String name;
    public final List<String> description;
    private final ItemStack icon;
    public final boolean sameCategoryOnly;
    private final int maxConcurrentQuests;
    public final int sortingId;
    public final int maxDaily;
    public final boolean isVisible, isSilent;

    private final List<ResourceLocation> requiredContext;

    private QuestCategory(ResourceLocation id, String name, List<String> description, ItemStack icon, boolean sameCategoryOnly, int maxConcurrentQuests, int sortingID, int maxDaily, List<ResourceLocation> requiredContext, boolean isVisible, boolean isSilent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.sameCategoryOnly = sameCategoryOnly;
        this.maxConcurrentQuests = maxConcurrentQuests;
        this.sortingId = sortingID;
        this.maxDaily = maxDaily;
        this.requiredContext = requiredContext;
        this.isVisible = isVisible;
        this.isSilent = isSilent;
    }

    /**
     * Check if this category matches the given context.
     * Contexts are simply ids you pass in and only if this category contains that id it passes
     * This is useful if you only want to make a quest accessible under a certain context.
     * SimpleQuest ONLY handles no context quests. Anything else requires a custom implementation of SimpleQuest API
     */
    public boolean matchesContext(@Nullable ResourceLocation context) {
        if (context == null) {
            return this.requiredContext.isEmpty();
        }
        for (ResourceLocation res : this.requiredContext) {
            if (res.equals(context))
                return true;
        }
        return false;
    }

    public int getMaxConcurrentQuests() {
        return this.maxConcurrentQuests;
    }

    public MutableComponent getName() {
        return new TranslatableComponent(this.name);
    }

    public static QuestCategory of(ResourceLocation id, JsonObject obj) {
        ImmutableList.Builder<String> description = new ImmutableList.Builder<>();
        JsonElement e = obj.get("description");
        if (e != null) {
            if (e.isJsonPrimitive() && !e.getAsString().isEmpty())
                description.add(e.getAsString());
            else if (e.isJsonArray()) {
                e.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        description.add(ea.getAsString());
                    }
                });
            }
        }
        ImmutableList.Builder<ResourceLocation> requiredContext = new ImmutableList.Builder<>();
        JsonArray ctxs = obj.getAsJsonArray("required_contexts");
        if (ctxs != null) {
            ctxs.forEach(ea -> {
                if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                    requiredContext.add(new ResourceLocation(ea.getAsString()));
                }
            });
        }
        return new QuestCategory(id,
                GsonHelper.getAsString(obj, "name"),
                description.build(),
                QuestUtils.icon(obj, "icon", Items.WRITTEN_BOOK),
                GsonHelper.getAsBoolean(obj, "only_same_category", false),
                GsonHelper.getAsInt(obj, "max_concurrent_quests", -1),
                GsonHelper.getAsInt(obj, "sorting_id", 0),
                GsonHelper.getAsInt(obj, "max_daily", -1),
                requiredContext.build(),
                GsonHelper.getAsBoolean(obj, "is_visible", true),
                GsonHelper.getAsBoolean(obj, "is_silent", false));
    }

    public JsonObject serialize(boolean full) {
        JsonObject obj = new JsonObject();
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
        QuestUtils.writeItemStackToJson(this.icon, full ? null : Items.WRITTEN_BOOK)
                .ifPresent(icon -> obj.add("icon", icon));
        if (this.sameCategoryOnly || full)
            obj.addProperty("only_same_category", this.sameCategoryOnly);
        if (this.maxConcurrentQuests != -1 || full)
            obj.addProperty("max_concurrent_quests", this.maxConcurrentQuests);
        if (this.sortingId != 0 || full)
            obj.addProperty("sorting_id", this.sortingId);
        if (!this.requiredContext.isEmpty() || full) {
            JsonArray arr = new JsonArray();
            this.requiredContext.forEach(ctx -> arr.add(ctx.toString()));
            obj.add("required_contexts", arr);
        }
        if (!this.isVisible || full)
            obj.addProperty("is_visible", this.isVisible);
        if (this.isSilent || full)
            obj.addProperty("is_silent", this.isSilent);
        return obj;
    }

    public ItemStack getIcon() {
        return this.icon.copy();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof QuestCategory category)
            return this.id.equals(category.id);
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[Category:%s]", this.id);
    }

    @Override
    public int compareTo(@NotNull QuestCategory category) {
        if (this.sortingId == category.sortingId) {
            return this.id.compareTo(category.id);
        }
        return Integer.compare(this.sortingId, category.sortingId);
    }

    public static class Builder {

        private final ResourceLocation id;
        private final String name;
        private final List<String> description = new ArrayList<>();
        private ItemStack icon = new ItemStack(Items.WRITTEN_BOOK);
        private int sortingID;
        private boolean isVisible = true, isSilent;
        private boolean sameCategoryOnly;
        private int maxConcurrentQuests = -1;
        private int maxDaily = -1;

        private final List<ResourceLocation> requiredContext = new ArrayList<>();

        public Builder(ResourceLocation id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder addDescription(String s) {
            this.description.add(s);
            return this;
        }

        public Builder withIcon(ItemStack stack) {
            this.icon = stack;
            return this;
        }

        public Builder needContexts(ResourceLocation... contexts) {
            this.requiredContext.addAll(List.of(contexts));
            return this;
        }

        public Builder withSortingNumber(int num) {
            this.sortingID = num;
            return this;
        }

        public Builder countSameCategoryOnly() {
            this.sameCategoryOnly = true;
            return this;
        }

        public Builder setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrentQuests = maxConcurrent;
            return this;
        }

        public Builder setHidden() {
            this.isVisible = false;
            return this;
        }

        public Builder setSilent() {
            this.isSilent = true;
            return this;
        }

        public Builder setMaxDaily(int maxDaily) {
            this.maxDaily = maxDaily;
            return this;
        }

        public QuestCategory build() {
            return new QuestCategory(this.id, this.name, this.description, this.icon, this.sameCategoryOnly, this.maxConcurrentQuests, this.sortingID, this.maxDaily, this.requiredContext, this.isVisible, this.isSilent);
        }
    }
}
