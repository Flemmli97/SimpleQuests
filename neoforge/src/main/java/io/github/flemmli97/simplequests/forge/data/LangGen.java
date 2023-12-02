package io.github.flemmli97.simplequests.forge.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.LangManager;
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

public class LangGen implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, String> data = new LinkedHashMap<>();
    private final DataGenerator gen;
    private final String locale;

    public LangGen(DataGenerator gen) {
        this.gen = gen;
        this.locale = "en_us";
    }

    protected void addTranslations() {
        LangManager.getDefaultTranslation().forEach(this::add);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            this.addTranslations();
            if (!this.data.isEmpty()) {
                try {
                    this.save(cache, this.gen.getPackOutput().getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(SimpleQuests.MODID).resolve("lang").resolve(this.locale + ".json"));
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
