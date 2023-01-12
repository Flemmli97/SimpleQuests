package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.SimpleQuests;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class QuestCategory implements Comparable<QuestCategory> {

    public static final QuestCategory DEFAULT_CATEGORY = new QuestCategory(new ResourceLocation(SimpleQuests.MODID, "default_category"),
            "Main", List.of(), new ItemStack(Items.WRITTEN_BOOK), -1, true);

    public final ResourceLocation id;
    public final String name;
    public final List<String> description;
    private final ItemStack icon;
    public final int sortingId;
    public boolean canBeSelected;

    private QuestCategory(ResourceLocation id, String name, List<String> description, ItemStack icon, int sortingID, boolean canBeSelected) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.sortingId = sortingID;
        this.canBeSelected = canBeSelected;
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
        return new QuestCategory(id,
                GsonHelper.getAsString(obj, "name"),
                description.build(),
                ParseHelper.icon(obj, "icon", Items.WRITTEN_BOOK),
                GsonHelper.getAsInt(obj, "sorting_id", 0),
                GsonHelper.getAsBoolean(obj, "selectable", true));
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
        ParseHelper.writeItemStackToJson(this.icon, full ? null : Items.WRITTEN_BOOK)
                .ifPresent(icon -> obj.add("icon", icon));
        if (this.sortingId != 0 || full)
            obj.addProperty("sorting_id", this.sortingId);
        if (!this.canBeSelected || full)
            obj.addProperty("selectable", this.canBeSelected);
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
        private boolean canBeSelected = true;

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

        public Builder unselectable() {
            this.canBeSelected = false;
            return this;
        }

        public Builder withSortingNumber(int num) {
            this.sortingID = num;
            return this;
        }

        public QuestCategory build() {
            return new QuestCategory(this.id, this.name, this.description, this.icon, this.sortingID, this.canBeSelected);
        }
    }
}
