package io.github.flemmli97.simplequests_api.forge.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.impls.progression.CraftingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
import io.github.flemmli97.simplequests_api.impls.tasks.AdvancementTask;
import io.github.flemmli97.simplequests_api.impls.tasks.BlockInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.CraftingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.EntityInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.FishingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.ItemTask;
import io.github.flemmli97.simplequests_api.impls.tasks.KillTask;
import io.github.flemmli97.simplequests_api.impls.tasks.XPTask;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.util.GsonHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LangAPIGen implements DataProvider {

    private final Map<String, String> data = new LinkedHashMap<>();
    private final DataGenerator gen;
    private final String locale;

    public LangAPIGen(DataGenerator gen) {
        this.gen = gen;
        this.locale = "en_us";
    }

    protected void addTranslations() {
        this.add(AdvancementTask.ID.toString(), "Obtain the advancement %s");
        this.add(AdvancementTask.MISSING_ADVANCEMENT, "Advancement with id %s missing");
        this.add(BlockInteractTask.ID.toString(), "Break %1$s x%2$s");
        this.add(BlockInteractTask.ID + ".use", "Interact with %1$s x%2$s");
        this.add(BlockInteractTask.ID + ".block_and_item", "Interact with %1$s using %2$s x%3$s");
        this.add(BlockInteractTask.ID + ".block_and_item.use", "Interact with %1$s using %2$s x%3$s");
        this.add(BlockInteractTask.ID + ".item", "Break blocks using %1$s x%2$s");
        this.add(BlockInteractTask.ID + ".item.use", "Use %1$s on blocks x%2$s");
        this.add(BlockInteractTask.ID + ".block.dupe.true", "You already interacted with this block");
        this.add(BlockInteractTask.ID + ".block.dupe.false", "You already broke this block");
        this.add(CraftingTask.ID.toString(), "Craft %1$s x%2$s");
        this.add(EntityInteractTask.ID.toString(), "Interact with %1$s x%2$s");
        this.add(EntityInteractTask.ID + ".item", "Use %2$s on %1$s x%3$s");
        this.add(EntityInteractTask.ID + ".dupe", "You already interacted with this!");
        this.add(FishingTask.ID.toString(), "Fish %1$s x%2$s");
        this.add(ItemTask.ID + ".single", "Submit %1$s x%2$s");
        this.add(ItemTask.ID + ".single.keep", "Have %1$s x%2$s");
        this.add(ItemTask.ID + ".multi", "Submit any of the following x%2$s: %1$s");
        this.add(ItemTask.ID + ".multi.keep", "Have any of the following x%2$s: %1$s");
        this.add(KillTask.ID.toString(), "Kill %s x%2$s");
        this.add(XPTask.ID.toString(), "Submit Experience: %s lvl");
        this.add(SimpleQuestsAPI.MODID + ".empty_tag", "<Empty tag!>");

        this.add(KillTracker.KILL_PROGRESS, "Progress: %1$s/%2$s");
        this.add(CraftingTracker.CRAFTING_PROGRESS, "Progress: %1$s/%2$s");
        this.add(BlockTracker.BLOCK_INTERACT_PROGRESS, "Progress: %1$s/%2$s");
        this.add(EntityTracker.ENTITY_INTERACT_PROGRESS, "Progress: %1$s/%2$s");
        this.add(FishingTracker.FISHING_PROGRESS, "Progress: %1$s/%2$s");

        this.add(SimpleQuestsAPI.MODID + ".task.formatter", " ▶ %s");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            this.addTranslations();
            if (!this.data.isEmpty()) {
                try {
                    this.save(cache, this.gen.getPackOutput().getOutputFolder(PackOutput.Target.DATA_PACK).resolve(SimpleQuestsAPI.MODID).resolve("lang").resolve(this.locale + ".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public String getName() {
        return "Languages: " + this.locale;
    }

    private void save(CachedOutput cache, Path target) throws IOException {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, String> pair : this.data.entrySet()) {
            json.addProperty(pair.getKey(), pair.getValue());
        }
        saveTo(cache, json, target);
    }

    public void add(String key, String value) {
        if (this.data.put(key, value) != null)
            throw new IllegalStateException("Duplicate translation key " + key);
    }

    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    private static void saveTo(CachedOutput cachedOutput, JsonElement jsonElement, Path path) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
        OutputStreamWriter writer = new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8);
        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setSerializeNulls(false);
        jsonWriter.setIndent("  ");
        GsonHelper.writeValue(jsonWriter, jsonElement, null);
        jsonWriter.close();
        cachedOutput.writeIfNeeded(path, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
    }
}
