package io.github.flemmli97.simplequests.datapack.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class QuestProvider implements DataProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, QuestCategory> categories = new HashMap<>();
    private final Map<ResourceLocation, Quest> quests = new HashMap<>();

    protected final DataGenerator gen;
    protected final boolean full;

    /**
     * Datagenerator for quests
     *
     * @param full If true will output all values for category and quests.
     *             Some values are optional and can be left out in the final json.
     */
    public QuestProvider(DataGenerator gen, boolean full) {
        this.gen = gen;
        this.full = full;
    }

    protected abstract void add();

    @Override
    public void run(HashCache cache) {
        this.add();
        this.categories.forEach((res, builder) -> {
            Path path = this.gen.getOutputFolder().resolve("data/" + res.getNamespace() + "/" + QuestsManager.CATEGORY_LOCATION + "/" + res.getPath() + ".json");
            try {
                JsonElement obj = builder.serialize(this.full);
                DataProvider.save(GSON, cache, obj, path);
            } catch (IOException e) {
                LOGGER.error("Couldn't save quest category {}", path, e);
            }
        });
        this.quests.forEach((res, builder) -> {
            Path path = this.gen.getOutputFolder().resolve("data/" + res.getNamespace() + "/" + QuestsManager.QUEST_LOCATION + "/" + res.getPath() + ".json");
            try {
                JsonElement obj = builder.serialize(false, this.full);
                DataProvider.save(GSON, cache, obj, path);
            } catch (IOException e) {
                LOGGER.error("Couldn't save quest {}", path, e);
            }
        });
    }

    @Override
    public String getName() {
        return "Quests";
    }

    public void addQuest(Quest quest) {
        if (quest.category != QuestCategory.DEFAULT_CATEGORY) {
            QuestCategory prev = this.categories.get(quest.category.id);
            if (prev != null && prev != quest.category)
                throw new IllegalStateException("Category with " + quest.category.id + " already registered. Try reusing the category instead of creating a new one.");
            this.categories.put(quest.category.id, quest.category);
        }
        Quest prev = this.quests.get(quest.id);
        if (prev != null)
            throw new IllegalStateException("Quest with " + quest.id + " already registered");
        this.quests.put(quest.id, quest);
    }
}

