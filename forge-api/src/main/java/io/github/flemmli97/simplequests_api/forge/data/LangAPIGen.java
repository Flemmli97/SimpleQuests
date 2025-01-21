package io.github.flemmli97.simplequests_api.forge.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.AdvancementEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.ItemEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.KillEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.PositionEntry;
import io.github.flemmli97.simplequests_api.impls.entries.single.XPEntry;
import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.impls.progression.CraftingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.EntityTracker;
import io.github.flemmli97.simplequests_api.impls.progression.FishingTracker;
import io.github.flemmli97.simplequests_api.impls.progression.KillTracker;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class LangAPIGen implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<String, String> data = new LinkedHashMap<>();
    private final DataGenerator gen;
    private final String locale;

    public LangAPIGen(DataGenerator gen) {
        this.gen = gen;
        this.locale = "en_us";
    }

    protected void addTranslations() {
        this.add(ItemEntry.ID + ".single", "Submit %1$s x%2$s");
        this.add(ItemEntry.ID + ".single.keep", "Have %1$s x%2$s");
        this.add(ItemEntry.ID + ".multi", "Submit any of the following x%2$s: %1$s");
        this.add(ItemEntry.ID + ".multi.keep", "Have any of the following x%2$s: %1$s");
        this.add(ItemEntry.ID + ".empty", "<Empty tag/items>");
        this.add(KillEntry.ID.toString(), "Kill %s x%2$s");
        this.add(KillEntry.ID + ".tag", "Kill entities in the tag %s x%2$s");
        this.add(XPEntry.ID.toString(), "Submit Experience: %s lvl");
        this.add("simplequests_api.missing.advancement", "Advancement with id %s missing");
        this.add(AdvancementEntry.ID.toString(), "Obtain the advancement %s");
        this.add(PositionEntry.ID.toString(), "Go to [x:%1$s;y:%2$s;z:%3$s]");

        this.add(KillTracker.KILL_PROGRESS, "Progress: %1$s/%2$s");
        this.add(CraftingTracker.CRAFTING_PROGRESS, "Progress: %1$s/%2$s");
        this.add(BlockTracker.BLOCK_INTERACT_PROGRESS, "Progress: %1$s/%2$s");
        this.add(EntityTracker.ENTITY_INTERACT_PROGRESS, "Progress: %1$s/%2$s");
        this.add(FishingTracker.FISHING_PROGRESS, "Progress: %1$s/%2$s");
    }

    @Override
    public void run(HashCache cache) throws IOException {
        this.addTranslations();
        if (!this.data.isEmpty())
            this.save(cache, this.data, this.gen.getOutputFolder().resolve("data/" + SimpleQuestsAPI.MODID + "/lang/" + this.locale + ".json"));
    }

    @Override
    public String getName() {
        return "Languages: " + this.locale;
    }

    @SuppressWarnings("deprecation")
    private void save(HashCache cache, Object object, Path target) throws IOException {
        String data = GSON.toJson(object);
        data = JavaUnicodeEscaper.outsideOf(0, 0x7f).translate(data); // Escape unicode after the fact so that it's not double escaped by GSON
        String hash = DataProvider.SHA1.hashUnencodedChars(data).toString();
        if (!Objects.equals(cache.getHash(target), hash) || !Files.exists(target)) {
            Files.createDirectories(target.getParent());

            try (BufferedWriter bufferedwriter = Files.newBufferedWriter(target)) {
                bufferedwriter.write(data);
            }
        }

        cache.putNew(target, hash);
    }

    public void add(String key, String value) {
        if (this.data.put(key, value) != null)
            throw new IllegalStateException("Duplicate translation key " + key);
    }
}
