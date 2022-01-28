package io.github.flemmli97.simplequests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigHandler {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Config config;
    public static LangManager lang;

    public static void init() {
        config = new Config();
        lang = new LangManager();
        reloadConfigs();
    }

    public static void reloadConfigs() {
        config.load();
        lang.reload(ConfigHandler.config.lang);
    }
}
