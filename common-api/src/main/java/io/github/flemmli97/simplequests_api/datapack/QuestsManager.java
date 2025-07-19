package io.github.flemmli97.simplequests_api.datapack;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main manager for all quests. Fetch all quests here
 */
public class QuestsManager extends SimplePreparableReloadListener<QuestsManager.ResourceResult> {

    public static final String CATEGORY_LOCATION = "simplequests_categories";
    public static final String QUEST_LOCATION = "simplequests";

    private static final int PATH_SUFFIX_LENGTH = ".json".length();

    private static final Gson GSON = new GsonBuilder().create();
    public static QuestsManager INSTANCE;

    private final HolderLookup.Provider provider;
    private Map<ResourceLocation, QuestCategory> categories;

    private Map<ResourceLocation, QuestBase> questMap;
    private Map<QuestCategory, Map<ResourceLocation, QuestBase>> quests;
    private Set<ResourceLocation> subQuests;

    private Map<QuestCategory, Set<Quest>> dailyQuests;

    public QuestsManager(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    public static QuestsManager instance() {
        return INSTANCE;
    }

    @Override
    protected ResourceResult prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new ResourceResult(this.readFiles(resourceManager, CATEGORY_LOCATION),
                this.readFiles(resourceManager, QUEST_LOCATION));
    }

    private Map<ResourceLocation, JsonElement> readFiles(ResourceManager resourceManager, String directory) {
        int i = directory.length() + 1;
        Map<ResourceLocation, JsonElement> map = Maps.newHashMap();
        resourceManager.listResources(directory, file -> file.getPath().endsWith(".json")).forEach((fileRes, resource) -> {
            String path = fileRes.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRes.getNamespace(), path.substring(i, path.length() - PATH_SUFFIX_LENGTH));
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (map.put(id, jsonElement) != null) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                }
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                SimpleQuestsAPI.LOGGER.error("Couldn't parse data file {} from {}", id, fileRes, e);
            }
        });
        return map;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(ResourceResult result, ResourceManager resourceManager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, QuestCategory> categoryBuilder = new ImmutableMap.Builder<>();
        categoryBuilder.put(QuestCategory.DEFAULT_CATEGORY.id, QuestCategory.DEFAULT_CATEGORY);
        DynamicOps<JsonElement> ops = this.provider.createSerializationContext(JsonOps.INSTANCE);
        result.categories.forEach((res, el) -> {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.keySet().isEmpty()) {
                    obj.addProperty("id", res.toString());
                    categoryBuilder.put(res, QuestCategory.CODEC.apply(true).parse(ops, obj).getOrThrow());
                }
            }
        });
        categoryBuilder.orderEntriesByValue(QuestCategory::compareTo);
        this.categories = categoryBuilder.build();

        Map<QuestCategory, ImmutableMap.Builder<ResourceLocation, QuestBase>> map = new HashMap<>();
        result.quests.forEach((res, el) -> {
            if (el.isJsonObject()) {
                try {
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.keySet().isEmpty()) {
                        obj.addProperty(QuestBase.ID_FIELD, res.toString());
                        QuestBase base = QuestBaseRegistry.CODEC.apply(QuestBaseRegistry.DATA_LOAD)
                                .parse(ops, obj).getOrThrow();
                        map.computeIfAbsent(base.category, c -> new ImmutableMap.Builder<>())
                                .put(res, base);
                    }
                } catch (Exception e) {
                    SimpleQuestsAPI.LOGGER.error("Unable to load quest {} {}", res, e);
                }
            }
        });
        // Post processing
        ImmutableMap.Builder<QuestCategory, Map<ResourceLocation, QuestBase>> questsCats = new ImmutableMap.Builder<>();
        ImmutableMap.Builder<ResourceLocation, QuestBase> quests = new ImmutableMap.Builder<>();
        ImmutableMap.Builder<QuestCategory, Set<Quest>> daily = new ImmutableMap.Builder<>();
        ImmutableSet.Builder<ResourceLocation> subQuests = new ImmutableSet.Builder<>();
        map.forEach((category, builder) -> {
            ImmutableMap<ResourceLocation, QuestBase> catQuests = builder.orderEntriesByValue(QuestBase::compareTo).build();
            questsCats.put(category, catQuests);
            catQuests.forEach((id, quest) -> {
                quests.put(id, quest);
                subQuests.addAll(quest.getSubQuests());
            });
            daily.put(category, catQuests.values().stream().filter(quest -> quest.isDailyQuest && quest instanceof Quest)
                    .map(quest -> (Quest) quest)
                    .collect(ImmutableSet.toImmutableSet()));
        });
        this.quests = questsCats.build();
        this.questMap = quests.build();
        this.dailyQuests = daily.build();
        this.subQuests = subQuests.build();
    }

    /**
     * Gets all quests registered ignoring contexts
     * See {@link QuestCategory#matchesContext(ResourceLocation)} for more info
     */
    public Map<ResourceLocation, QuestBase> getAllQuests() {
        return this.questMap;
    }

    /**
     * Gets the given quest registered ignoring contexts
     * See {@link QuestCategory#matchesContext(ResourceLocation)} for more info
     */
    public QuestBase getQuest(ResourceLocation id) {
        return this.questMap.get(id);
    }

    /**
     * Gets the given quest registered under the given context
     * See {@link QuestCategory#matchesContext(ResourceLocation)} for more info
     */
    public QuestBase getQuest(ResourceLocation id, @Nullable ResourceLocation context) {
        QuestBase base = this.getQuest(id);
        if (base == null || !base.category.matchesContext(context))
            return null;
        return base;
    }

    public Quest getActualQuest(ResourceLocation id, @Nullable ResourceLocation context) {
        QuestBase base = this.getQuest(id, context);
        if (base instanceof Quest quest)
            return quest;
        return null;
    }

    public boolean isSubQuest(ResourceLocation quest) {
        return this.subQuests.contains(quest);
    }

    public Map<ResourceLocation, QuestBase> getQuestsForCategory(ResourceLocation res, @Nullable ResourceLocation context) {
        QuestCategory category = this.getQuestCategory(res, context);
        if (category == null)
            throw new IllegalArgumentException("No such category for " + res);
        return this.getQuestsForCategory(category);
    }

    public Map<ResourceLocation, QuestBase> getQuestsForCategory(QuestCategory category) {
        return this.quests.getOrDefault(category, Map.of());
    }

    public Set<Quest> getDailyQuests() {
        return this.getDailyQuests(QuestCategory.DEFAULT_CATEGORY);
    }

    public Set<Quest> getDailyQuests(QuestCategory category) {
        return this.dailyQuests.getOrDefault(category, Set.of());
    }

    public QuestCategory getQuestCategory(ResourceLocation res) {
        if (res.equals(QuestCategory.DEFAULT_CATEGORY.id))
            return QuestCategory.DEFAULT_CATEGORY;
        return this.categories.get(res);
    }

    public QuestCategory getQuestCategory(ResourceLocation res, @Nullable ResourceLocation context) {
        QuestCategory category = this.getQuestCategory(res);
        if (category == null || !category.matchesContext(context))
            return null;
        return category;
    }

    public Map<ResourceLocation, QuestCategory> getSelectableCategories(@Nullable ResourceLocation context) {
        return this.categories.entrySet().stream().filter(e -> e.getValue().matchesContext(context) && e.getValue().isVisible)
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<ResourceLocation, QuestCategory> getCategories(@Nullable ResourceLocation context) {
        return this.categories.entrySet().stream().filter(e -> e.getValue().matchesContext(context))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<QuestCategory> categories(@Nullable ResourceLocation context) {
        return this.categories.values().stream().filter(cat -> cat.matchesContext(context)).toList();
    }

    protected record ResourceResult(Map<ResourceLocation, JsonElement> categories,
                                    Map<ResourceLocation, JsonElement> quests) {
    }
}
