package io.github.flemmli97.simplequests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigHandler {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Config CONFIG;

    public static void init() {
        CONFIG = new Config();
        reloadConfigs();
    }

    public static void reloadConfigs() {
        CONFIG.load();
    }
}
