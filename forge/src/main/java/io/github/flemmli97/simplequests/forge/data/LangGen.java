package io.github.flemmli97.simplequests.forge.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.data.PlayerData;
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
        this.add("simplequests.missing.requirements", "Requirements not fullfilled for the quest");
        this.add("simplequests.active", "This quest is already active");
        this.add("simplequests.active.full", "You already have the max amount of active quests");
        this.add("simplequests.accept", "Accepted quest %s");
        this.add("simplequests.finish", "Finished quest [%s]");
        this.add("simplequests.current.no", "No active quest");
        this.add("simplequests.reset", "Reset current quest [%s]");
        this.add("simplequests.reset.confirm", "Are you sure. Submitted items will not be refunded? Type again to confirm");
        this.add("simplequests.reset.notfound", "No active quest with id %s");
        this.add("simplequests.reset.cooldown", "Reset quest cooldowns for %s");
        this.add("simplequests.reset.all", "Reset all progress for %s");
        this.add("simplequests.unlock", "Unlocked quest %2$s for players %1$s");
        this.add("simplequests.unlock.fail", "No such quest %s");

        this.add("simplequests.kill", "Finished kill task %s");
        this.add("simplequests.quest.noexist", "No quest exists with id %s");
        this.add("simplequests.quest.is_selection", "Quest with id %s is a selection quest!");
        this.add("simplequests.quest.composite.noexist", "Quest with id %s is not a selection quest!");
        this.add("simplequests.quest.composite.resolve.none", "Selectionquest with id %1$s has no selectable quest with %2$s!");
        this.add("simplequests.quest.category.noexist", "No quest category exists with id %s");
        this.add("simplequests.task", "Finished task %s");

        this.add("simplequests.interaction.dupe", "You already interacted with this predicate");
        this.add("simplequests.interaction.block.dupe.true", "You already interacted with this block");
        this.add("simplequests.interaction.block.dupe.false", "You already broke this block");

        this.add(PlayerData.AcceptType.REQUIREMENTS.langKey(), "Missing requirements for quest");
        this.add(PlayerData.AcceptType.DAILYFULL.langKey(), "You can't repeat this quest again today");
        this.add(PlayerData.AcceptType.DELAY.langKey(), "Quest on cooldown for %s");
        this.add(PlayerData.AcceptType.ONETIME.langKey(), "This is a onetime quest");
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
    public void run(HashCache cache) throws IOException {
        this.addTranslations();
        if (!this.data.isEmpty())
            this.save(cache, this.data, this.gen.getOutputFolder().resolve("data/" + SimpleQuests.MODID + "/lang/" + this.locale + ".json"));
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
