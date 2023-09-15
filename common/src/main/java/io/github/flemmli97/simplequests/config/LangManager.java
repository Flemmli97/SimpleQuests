package io.github.flemmli97.simplequests.config;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.ProgressionTrackerImpl;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LangManager {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @SuppressWarnings({"UnstableApiUsage"})
    private static final Type mapType = new TypeToken<Map<String, String>>() {
    }.getType();

    private static final Map<String, String> DEFAULT_TRANSLATION = new LinkedHashMap<>();

    static {
        DEFAULT_TRANSLATION.put("simplequests.missing.requirements", "Requirements not fullfilled for the quest");
        DEFAULT_TRANSLATION.put("simplequests.active", "This quest is already active");
        DEFAULT_TRANSLATION.put("simplequests.active.full", "You already have the max amount of active quests");
        DEFAULT_TRANSLATION.put("simplequests.accept", "Accepted quest %s");
        DEFAULT_TRANSLATION.put("simplequests.finish", "Finished quest [%s]");
        DEFAULT_TRANSLATION.put("simplequests.current", "Current quest [%s]");
        DEFAULT_TRANSLATION.put("simplequests.current.no", "No active quest");
        DEFAULT_TRANSLATION.put("simplequests.reset", "Reset current quest [%s]");
        DEFAULT_TRANSLATION.put("simplequests.reset.confirm", "Are you sure. Submitted items will not be refunded? Type again to confirm");
        DEFAULT_TRANSLATION.put("simplequests.reset.notfound", "No active quest with id %s");
        DEFAULT_TRANSLATION.put("simplequests.reset.cooldown", "Reset quest cooldowns for %s");
        DEFAULT_TRANSLATION.put("simplequests.reset.all", "Reset all progress for %s");
        DEFAULT_TRANSLATION.put("simplequests.unlock", "Unlocked quest %2$s for players %1$s");
        DEFAULT_TRANSLATION.put("simplequests.unlock.fail", "No such quest %s");

        DEFAULT_TRANSLATION.put("simplequests.missing.advancement", "Advancement with id %s missing");
        DEFAULT_TRANSLATION.put("simplequests.kill", "Finished kill task %s");
        DEFAULT_TRANSLATION.put("simplequests.quest.noexist", "No quest exists with id %s");
        DEFAULT_TRANSLATION.put("simplequests.quest.category.noexist", "No quest category exists with id %s");
        DEFAULT_TRANSLATION.put("simplequests.task", "Finished task %s");
        DEFAULT_TRANSLATION.put("simplequests.interaction.dupe", "You already interacted with this predicate");
        DEFAULT_TRANSLATION.put("simplequests.interaction.block.dupe.true", "You already interacted with this block");
        DEFAULT_TRANSLATION.put("simplequests.interaction.block.dupe.false", "You already broke this block");

        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.REQUIREMENTS.langKey(), "Missing requirements for quest");
        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.DAILYFULL.langKey(), "You can't repeat this quest again today");
        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.DELAY.langKey(), "Quest on cooldown for %s");
        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.ONETIME.langKey(), "This is a onetime quest");
        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.ACCEPT.langKey(), "Quest acceptable");
        DEFAULT_TRANSLATION.put(PlayerData.AcceptType.LOCKED.langKey(), "You can't accept this quest");

        DEFAULT_TRANSLATION.put("simplequests.gui.main", "Quests");
        DEFAULT_TRANSLATION.put("simplequests.gui.composite.quest", "Select Quest");
        DEFAULT_TRANSLATION.put("simplequests.gui.confirm", "Accept this quest?");
        DEFAULT_TRANSLATION.put("simplequests.gui.reset", "Reset this quest? No refunds!");
        DEFAULT_TRANSLATION.put("simplequests.gui.yes", "Yes");
        DEFAULT_TRANSLATION.put("simplequests.gui.no", "No");

        DEFAULT_TRANSLATION.put("simplequests.gui.next", "Next Page");
        DEFAULT_TRANSLATION.put("simplequests.gui.prev", "Previous Page");
        DEFAULT_TRANSLATION.put("simplequests.gui.button.main", "Back");

        DEFAULT_TRANSLATION.put("simplequests.reload", "Reloading configs");

        DEFAULT_TRANSLATION.put(QuestEntryImpls.ItemEntry.ID + ".single", "Give %1$s x%2$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.ItemEntry.ID + ".single.keep", "Have %1$s x%2$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.ItemEntry.ID + ".multi", "Provide any of the following x%2$s: %1$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.ItemEntry.ID + ".multi.keep", "Have any of the following x%2$s: %1$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.ItemEntry.ID + ".empty", "<Empty tag/items>");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.KillEntry.ID.toString(), "Kill %s x%2$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.KillEntry.ID + ".tag", "Kill entities in the tag %s x%2$s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.XPEntry.ID.toString(), "Submit Experience: %s lvl");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.AdvancementEntry.ID.toString(), "Advancement %s");
        DEFAULT_TRANSLATION.put(QuestEntryImpls.PositionEntry.ID.toString(), "Go to [x:%1$s;y:%2$s;z:%3$s]");

        DEFAULT_TRANSLATION.put("quest.progress", "%1$s - %2$s");
        DEFAULT_TRANSLATION.put(ProgressionTrackerImpl.KILL_PROGRESS, "Killed: %1$s/%2$s");
        DEFAULT_TRANSLATION.put(ProgressionTrackerImpl.CRAFTING_PROGRESS, "Crafted: %1$s/%2$s");
        DEFAULT_TRANSLATION.put(ProgressionTrackerImpl.BLOCK_INTERACT_PROGRESS, "%1$s/%2$s");
        DEFAULT_TRANSLATION.put(ProgressionTrackerImpl.ENTITY_INTERACT_PROGRESS, "%1$s/%2$s");
        DEFAULT_TRANSLATION.put(ProgressionTrackerImpl.FISHING_PROGRESS, "%1$s/%2$s");
    }

    private Map<String, String> translation = new HashMap<>();

    private final Path confDir;

    public LangManager() {
        Path configDir = SimpleQuests.getHandler().getConfigPath().resolve("simplequests").resolve("lang");
        this.confDir = configDir;
        try {
            File dir = configDir.toFile();
            if (!dir.exists())
                dir.mkdirs();
            URL url = LangManager.class.getClassLoader().getResource("data/simplequests/lang");
            if (url != null) {
                URI uri = LangManager.class.getClassLoader().getResource("data/simplequests/lang").toURI();
                try {
                    FileSystems.newFileSystem(uri, Collections.emptyMap());
                } catch (FileSystemAlreadyExistsException | IllegalArgumentException ignored) {
                }
                Files.walk(Path.of(uri))
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                InputStream s = Files.newInputStream(p, StandardOpenOption.READ);
                                File target = configDir.resolve(p.getFileName().toString()).toFile();
                                if (!target.exists())
                                    target.createNewFile();
                                OutputStream o = new FileOutputStream(target);
                                s.transferTo(o);
                                s.close();
                                o.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
            File def = configDir.resolve("en_us.json").toFile();
            if (!def.exists()) {
                def.createNewFile();
                saveTo(def, DEFAULT_TRANSLATION);
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        this.reload(ConfigHandler.config.lang);
    }

    public void reload(String lang) {
        try {
            FileReader reader = new FileReader(this.confDir.resolve(lang + ".json").toFile());
            this.translation = GSON.fromJson(reader, mapType);
            reader.close();
            //en_us is basically used as a default modifiable file
            if (lang.equals("en_us")) {
                Map<String, String> ordered = new LinkedHashMap<>();
                DEFAULT_TRANSLATION.forEach((key, t) -> ordered.put(key, this.translation.getOrDefault(key, t)));
                saveTo(this.confDir.resolve("en_us.json").toFile(), ordered);
            }
        } catch (IOException e) {
            if (lang.equals("en_us"))
                e.printStackTrace();
            else
                this.reload("en_us");
        }
    }

    public String get(String key) {
        return this.translation.getOrDefault(key, ConfigHandler.config.fallBackToEnLang ? DEFAULT_TRANSLATION.getOrDefault(key, key) : key);
    }

    private static void saveTo(File file, Map<String, String> translation) {
        try {
            FileWriter writer = new FileWriter(file);
            GSON.toJson(translation, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
