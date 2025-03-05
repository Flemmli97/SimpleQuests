package io.github.flemmli97.simplequests.forge.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.data.PlayerData;
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

    private final Map<String, String> data = new LinkedHashMap<>();
    private final DataGenerator gen;
    private final String locale;

    public LangGen(DataGenerator gen) {
        this.gen = gen;
        this.locale = "en_us";
    }

    protected void addTranslations() {
        this.add("simplequests.missing.requirements", "Requirements not fullfilled for the quest");
        this.add("simplequests.active", "This quest is already active");
        this.add("simplequests.active.full", "You already have the max amount of active quests");
        this.add("simplequests.accept", "Accepted quest %s");
        this.add("simplequests.finish", "Finished quest %s");
        this.add("simplequests.finish.sub", "Finished sub-quest %s");
        this.add("simplequests.current.no", "No active quest");
        this.add("simplequests.reset", "Reset current quest %s");
        this.add("simplequests.reset.confirm", "Are you sure? Submitted items will not be refunded! Type again to confirm");
        this.add("simplequests.reset.notfound", "No active quest with id %s");
        this.add("simplequests.reset.cooldown", "Reset quest cooldowns for %s");
        this.add("simplequests.reset.all", "Reset all progress for %s");
        this.add("simplequests.unlock", "Unlocked quest %2$s for players %1$s");
        this.add("simplequests.unlock.fail", "No such quest %s");
        this.add("simplequests.adminMode", "Admin mode: %s");

        this.add("simplequests.quest.noexist", "No quest exists with id %s");
        this.add("simplequests.quest.is_selection", "Quest with id %s is a selection-quest!");
        this.add("simplequests.quest.composite.noexist", "Quest with id %s is not a selection quest!");
        this.add("simplequests.quest.composite.resolve.none", "Selection-quest with id %1$s has no selectable quest with %2$s!");
        this.add("simplequests.quest.category.noexist", "No quest category exists with id %s");
        this.add("simplequests.task.complete", "Finished task [%s]");
        this.add("simplequests.task.chat_format", "▶ %s");

        this.add(PlayerData.AcceptType.REQUIREMENTS.langKey(), "Missing requirements for quest");
        this.add(PlayerData.AcceptType.DAILYFULL.langKey(), "You can't repeat this quest again today");
        this.add(PlayerData.AcceptType.DELAY.langKey(), "Quest on cooldown for %s");
        this.add(PlayerData.AcceptType.ONETIME.langKey(), "This is a onetime quest");
        this.add(PlayerData.AcceptType.MAX.langKey(), "You cannot repeat this quest anymore");
        this.add(PlayerData.AcceptType.ACCEPT.langKey(), "Quest acceptable");
        this.add(PlayerData.AcceptType.LOCKED.langKey(), "You can't accept this quest");

        this.add("simplequests.gui.main", "Quests");
        this.add("simplequests.gui.composite.quest", "Select Quest");
        this.add("simplequests.gui.confirm", "Accept this quest?");
        this.add("simplequests.gui.reset", "Reset this quest? No refunds!");
        this.add("simplequests.gui.yes", "Yes");
        this.add("simplequests.gui.no", "No");
        this.add("simplequests.gui.quest.current", "Active Quests");

        this.add("simplequests.gui.next", "Next Page");
        this.add("simplequests.gui.previous", "Previous Page");
        this.add("simplequests.gui.button.main", "Back");

        this.add("simplequests.reload", "Reloading configs");
        this.add("simplequest.quest.progress", "%1$s - %2$s");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.runAsync(() -> {
            this.addTranslations();
            if (!this.data.isEmpty()) {
                try {
                    this.save(cache, this.gen.getPackOutput().getOutputFolder(PackOutput.Target.DATA_PACK).resolve(SimpleQuests.MODID).resolve("lang").resolve(this.locale + ".json"));
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
