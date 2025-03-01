package io.github.flemmli97.simplequests_api.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class QuestCategory implements Comparable<QuestCategory> {

    public static final QuestCategory DEFAULT_CATEGORY = new QuestCategory(new ResourceLocation(SimpleQuestsAPI.MODID, "default_category"),
            "Main", List.of(), new ItemStack(Items.WRITTEN_BOOK), false, -1, -1, -1, List.of(), true, false);

    public static final Function<Boolean, Codec<QuestCategory>> CODEC = Util.memoize(full -> RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("is_visible").forGetter(c -> !c.isVisible || full ? Optional.of(c.isVisible) : Optional.empty()),
            Codec.BOOL.optionalFieldOf("is_silent").forGetter(c -> c.isSilent || full ? Optional.of(c.isSilent) : Optional.empty()),

            Codec.INT.optionalFieldOf("sorting_id").forGetter(c -> c.sortingId != 0 || full ? Optional.of(c.sortingId) : Optional.empty()),
            Codec.INT.optionalFieldOf("max_daily").forGetter(c -> c.maxDaily != 0 || full ? Optional.of(c.maxDaily) : Optional.empty()),
            ResourceLocation.CODEC.listOf().optionalFieldOf("required_context").forGetter(c -> !c.requiredContext.isEmpty() || full ? Optional.of(c.requiredContext) : Optional.empty()),

            JsonCodecs.ITEM_STACK_CODEC.optionalFieldOf("icon").forGetter(c -> QuestUtils.defaultChecked(c.getIcon(), full ? null : Items.WRITTEN_BOOK)),
            Codec.BOOL.optionalFieldOf("only_same_category").forGetter(c -> c.sameCategoryOnly || full ? Optional.of(c.sameCategoryOnly) : Optional.empty()),
            Codec.INT.optionalFieldOf("max_concurrent_quests").forGetter(c -> c.maxConcurrentQuests != -1 || full ? Optional.of(c.maxConcurrentQuests) : Optional.empty()),

            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(c -> Optional.empty()), // ID only for deserializing
            Codec.STRING.fieldOf("name").forGetter(c -> c.name),
            Codec.STRING.listOf().optionalFieldOf("description").forGetter(c -> !c.description.isEmpty() || full ? Optional.of(c.description) : Optional.empty())
    ).apply(inst, (visible, silent, sort, daily, select, icon, same, max, id, name, desc) -> new QuestCategory(id.orElseThrow(), name, desc.orElse(List.of()), icon.orElse(new ItemStack(Items.WRITTEN_BOOK)),
            same.orElse(false), max.orElse(-1), sort.orElse(0), daily.orElse(-1), select.orElse(List.of()), visible.orElse(true), silent.orElse(false)))));

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
        return Component.translatable(this.name);
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
