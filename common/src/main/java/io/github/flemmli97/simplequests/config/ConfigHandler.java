package io.github.flemmli97.simplequests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigHandler {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final Config CONFIG = new Config();

    public static void init() {
        reloadConfigs();
    }

    public static void reloadConfigs() {
        CONFIG.load();
    }
}
