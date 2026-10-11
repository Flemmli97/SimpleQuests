package io.github.flemmli97.simplequests_api.datapack.provider;

import com.google.gson.JsonElement;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionImpl;
import io.github.flemmli97.simplequests_api.registry.QuestDefinitionRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class QuestProvider implements DataProvider {

    private final Map<ResourceLocation, QuestCategory> categories = new HashMap<>();
    private final Map<ResourceLocation, QuestDefinition> quests = new HashMap<>();

    protected final PackOutput output;
    protected final boolean full;
    private final CompletableFuture<HolderLookup.Provider> lookup;

    /**
     * Datagenerator for quests
     *
     * @param full If true will output all values for category and quests.
     *             Some values are optional and can be left out in the final JSON.
     */
    public QuestProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, boolean full) {
        this.output = output;
        this.lookup = lookup;
        this.full = full;
    }

    protected abstract void add(HolderLookup.Provider provider);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.lookup.thenApply(provider -> {
            this.add(provider);
            return provider;
        }).thenCompose(provider -> CompletableFuture.allOf(
                CompletableFuture.allOf(this.categories.entrySet().stream().map(entry -> {
                    Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(entry.getKey().getNamespace() + "/" + QuestsManager.CATEGORY_DIRECTORY + "/" + entry.getKey().getPath() + ".json");
                    JsonElement obj = QuestCategory.CODEC.apply(this.full)
                            .encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), entry.getValue())
                            .getOrThrow();
                    return DataProvider.saveStable(cache, obj, path);
                }).toArray(CompletableFuture<?>[]::new)),
                CompletableFuture.allOf(this.quests.entrySet().stream().map(entry -> {
                    Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(entry.getKey().getNamespace() + "/" + QuestsManager.QUEST_DIRECTORY + "/" + entry.getKey().getPath() + ".json");
                    Encoder<QuestDefinition> codec = this.full ? QuestDefinitionRegistry.CODEC : QuestDefinition.SIMPLIFIED_ENCODER;
                    JsonElement obj = codec
                            .encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), entry.getValue())
                            .getOrThrow();
                    return DataProvider.saveStable(cache, obj, path);
                }).toArray(CompletableFuture<?>[]::new))
        ));
    }

    @Override
    public String getName() {
        return "Quests";
    }

    public void addQuest(ResourceLocation id, QuestDefinitionImpl.BuilderBase<?, ?> builder) {
        this.addQuest(id, builder.build());
    }

    public void addQuest(ResourceLocation id, QuestDefinitionImpl quest) {
        if (quest.category() != QuestCategory.DEFAULT_CATEGORY) {
            QuestCategory prev = this.categories.get(quest.category().id);
            if (prev != null && prev != quest.category())
                throw new IllegalStateException("Category with " + quest.category().id + " already registered. Try reusing the category instead of creating a new one.");
            this.categories.put(quest.category().id, quest.category());
        }
        QuestDefinition prev = this.quests.get(id);
        if (prev != null)
            throw new IllegalStateException("Quest with " + id + " already registered");
        this.quests.put(id, quest);
    }
}

