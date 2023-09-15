package io.github.flemmli97.simplequests.datapack.provider;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.QuestBase;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class QuestProvider implements DataProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, QuestCategory> categories = new HashMap<>();
    private final Map<ResourceLocation, QuestBase> quests = new HashMap<>();

    protected final PackOutput output;
    protected final boolean full;

    /**
     * Datagenerator for quests
     *
     * @param full If true will output all values for category and quests.
     *             Some values are optional and can be left out in the final json.
     */
    public QuestProvider(PackOutput output, boolean full) {
        this.output = output;
        this.full = full;
    }

    protected abstract void add();

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.add();
        return CompletableFuture.allOf(
                CompletableFuture.allOf(this.categories.entrySet().stream().map(entry -> {
                    Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(entry.getKey().getNamespace() + "/" + QuestsManager.CATEGORY_LOCATION + "/" + entry.getKey().getPath() + ".json");
                    JsonElement obj = entry.getValue().serialize(this.full);
                    return saveStable(cache, obj, path);
                }).toArray(CompletableFuture<?>[]::new)),
                CompletableFuture.allOf(this.quests.entrySet().stream().map(entry -> {
                    Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(entry.getKey().getNamespace() + "/" + QuestsManager.QUEST_LOCATION + "/" + entry.getKey().getPath() + ".json");
                    JsonElement obj = entry.getValue().serialize(false, this.full);
                    return saveStable(cache, obj, path);
                }).toArray(CompletableFuture<?>[]::new))
        );
    }

    @Override
    public String getName() {
        return "Quests";
    }

    public void addQuest(QuestBase.BuilderBase<?> builder) {
        QuestBase quest = builder.build();
        if (quest.category != QuestCategory.DEFAULT_CATEGORY) {
            QuestCategory prev = this.categories.get(quest.category.id);
            if (prev != null && prev != quest.category)
                throw new IllegalStateException("Category with " + quest.category.id + " already registered. Try reusing the category instead of creating a new one.");
            this.categories.put(quest.category.id, quest.category);
        }
        QuestBase prev = this.quests.get(quest.id);
        if (prev != null)
            throw new IllegalStateException("Quest with " + quest.id + " already registered");
        this.quests.put(quest.id, quest);
    }

    @SuppressWarnings("UnstableApiUsage")
    static CompletableFuture<?> saveStable(CachedOutput output, JsonElement json, Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha256(), byteArrayOutputStream);
                JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8));

                try {
                    jsonWriter.setSerializeNulls(false);
                    jsonWriter.setIndent("  ");
                    GSON.toJson(json, jsonWriter);
                } catch (Throwable var9) {
                    try {
                        jsonWriter.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                jsonWriter.close();
                output.writeIfNeeded(path, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
            } catch (IOException var10) {
                LOGGER.error("Failed to save file to {}", path, var10);
            }

        }, Util.backgroundExecutor());
    }
}

