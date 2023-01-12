package io.github.flemmli97.simplequests.datapack;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QuestsManager extends SimplePreparableReloadListener<QuestsManager.ResourceResult> {

    public static final String CATEGORY_LOCATION = "simplequests_categories";
    public static final String QUEST_LOCATION = "simplequests";

    private static final int PATH_SUFFIX_LENGTH = ".json".length();

    private static final Gson GSON = new GsonBuilder().create();
    private static final QuestsManager INSTANCE = new QuestsManager();

    private Map<ResourceLocation, QuestCategory> categories;
    private Map<ResourceLocation, QuestCategory> selectableCategories;
    private List<QuestCategory> categoryView;

    private Map<ResourceLocation, Quest> questMap;
    private Map<QuestCategory, Map<ResourceLocation, Quest>> quests;

    private Map<QuestCategory, Set<Quest>> dailyQuests;

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
        for (ResourceLocation fileRes : resourceManager.listResources(directory, file -> file.endsWith(".json"))) {
            String path = fileRes.getPath();
            ResourceLocation id = new ResourceLocation(fileRes.getNamespace(), path.substring(i, path.length() - PATH_SUFFIX_LENGTH));
            try (Resource resource = resourceManager.getResource(fileRes)) {
                try (InputStream inputStream = resource.getInputStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        JsonElement jsonElement = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                        if (jsonElement != null) {
                            JsonElement jsonElement2 = map.put(id, jsonElement);
                            if (jsonElement2 != null) {
                                throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                            }
                        } else {
                            SimpleQuests.logger.error("Couldn't load data file {} from {} as it's null or empty", id, fileRes);
                        }
                    }
                }
            } catch (IllegalArgumentException | IOException | JsonParseException e) {
                SimpleQuests.logger.error("Couldn't parse data file {} from {}", new Object[]{id, fileRes, e});
            }
        }
        return map;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(ResourceResult result, ResourceManager resourceManager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, QuestCategory> categoryBuilder = new ImmutableMap.Builder<>();
        categoryBuilder.put(QuestCategory.DEFAULT_CATEGORY.id, QuestCategory.DEFAULT_CATEGORY);
        result.categories.forEach((res, el) -> {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.keySet().isEmpty()) {
                    categoryBuilder.put(res, QuestCategory.of(res, obj));
                }
            }
        });
        categoryBuilder.orderEntriesByValue(QuestCategory::compareTo);
        this.categories = categoryBuilder.build();
        this.selectableCategories = this.categories.entrySet().stream().filter(e -> e.getValue().canBeSelected)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        this.categoryView = this.categories.values().stream().toList();

        Map<QuestCategory, ImmutableMap.Builder<ResourceLocation, Quest>> map = new HashMap<>();
        result.quests.forEach((res, el) -> {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.keySet().isEmpty()) {
                    String cat = GsonHelper.getAsString(obj, "category", "");
                    QuestCategory questCategory = QuestCategory.DEFAULT_CATEGORY;
                    if (!cat.isEmpty()) {
                        questCategory = this.categories.get(new ResourceLocation(cat));
                        if (questCategory == null)
                            throw new JsonSyntaxException("Quest category of " + cat + " for quest " + res + " doesn't exist!");
                    }
                    Quest quest = Quest.of(res, questCategory, el.getAsJsonObject());
                    map.computeIfAbsent(questCategory, c -> new ImmutableMap.Builder<>())
                            .put(res, quest);
                }
            }
        });
        map.forEach((category, builder) -> builder.orderEntriesByValue(Quest::compareTo));
        this.quests = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> e.getValue().build()));
        this.questMap = this.quests.values().stream().flatMap(m -> m.entrySet().stream())
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<QuestCategory, Set<Quest>> daily = new HashMap<>();
        this.quests.forEach((cat, q) -> daily.put(cat, q.values().stream().filter(quest -> quest.isDailyQuest)
                .collect(Collectors.toSet())));
        this.dailyQuests = ImmutableMap.copyOf(daily);
    }

    public Map<ResourceLocation, Quest> getAllQuests() {
        return this.questMap;
    }

    public Map<ResourceLocation, Quest> getQuestsForCategoryID(ResourceLocation res) {
        QuestCategory category = this.getQuestCategory(res);
        if (category == null)
            throw new IllegalArgumentException("No such category for " + res);
        return this.getQuestsForCategory(category);
    }

    public Map<ResourceLocation, Quest> getQuestsForCategory(QuestCategory category) {
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

    public Map<ResourceLocation, QuestCategory> getSelectableCategories() {
        return this.selectableCategories;
    }

    public Map<ResourceLocation, QuestCategory> getCategories() {
        return this.categories;
    }

    public List<QuestCategory> categories() {
        return this.categoryView;
    }

    protected record ResourceResult(Map<ResourceLocation, JsonElement> categories,
                                    Map<ResourceLocation, JsonElement> quests) {
    }
}
