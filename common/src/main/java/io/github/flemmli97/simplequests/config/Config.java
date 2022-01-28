package io.github.flemmli97.simplequests.config;

import io.github.flemmli97.simplequests.SimpleQuests;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {

    private transient File config;

    public String lang = "en_us";
    public boolean fallBackToEnLang = false;

    public Config() {
        File configDir = SimpleQuests.getHandler().getConfigPath().resolve("simplequests").toFile();
        try {
            if (!configDir.exists())
                configDir.mkdirs();
            this.config = new File(configDir, "simplequests.json");
            if (!this.config.exists()) {
                this.config.createNewFile();
                this.save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            FileReader reader = new FileReader(this.config);
            Config obj = ConfigHandler.GSON.fromJson(reader, Config.class);
            reader.close();
            this.lang = obj.lang;
            this.fallBackToEnLang = obj.fallBackToEnLang;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.save();
    }

    private void save() {
        try {
            FileWriter writer = new FileWriter(this.config);
            ConfigHandler.GSON.toJson(this, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
