package io.github.flemmli97.simplequests.datapack;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.github.flemmli97.simplequests.quest.Quest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class QuestsManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final QuestsManager INSTANCE = new QuestsManager();
    private Map<ResourceLocation, Quest> quests;

    public static QuestsManager instance() {
        return INSTANCE;
    }

    protected QuestsManager() {
        super(GSON, "simplequests");
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        ImmutableMap.Builder<ResourceLocation, Quest> builder = new ImmutableMap.Builder<>();
        object.forEach((res, el) -> {
            if (el.isJsonObject() && !el.getAsJsonObject().keySet().isEmpty()) {
                builder.put(res, Quest.of(res, el.getAsJsonObject()));
            }
        });
        builder.orderEntriesByValue(Quest::compareTo);
        this.quests = builder.build();
    }

    public Map<ResourceLocation, Quest> getQuests() {
        return this.quests;
    }

}
