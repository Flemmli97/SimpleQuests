package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;

import java.util.Map;

public class Quest {

    public final Map<String, QuestEntry> entries;

    public final ResourceLocation id;
    public final ResourceLocation neededParentQuest;
    public final ResourceLocation loot;

    public final int repeatDelay, repeatDaily;

    public final String questTaskString;

    private Quest(ResourceLocation id, String questTaskString, ResourceLocation parent, ResourceLocation loot, int repeatDelay, int repeatDaily, Map<String, QuestEntry> entries) {
        this.id = id;
        this.questTaskString = questTaskString;
        this.neededParentQuest = parent;
        this.repeatDelay = repeatDelay;
        this.repeatDaily = repeatDaily;
        this.entries = entries;
        this.loot = loot;
    }

    public Component getFormatted(MinecraftServer server) {
        return new TextComponent(this.questTaskString).append("\n").append(this.getFormattedTasks(server));
    }

    public Component getFormattedTasks(MinecraftServer server) {
        BaseComponent tasks = new TextComponent(" - ");
        boolean start = true;
        for (Map.Entry<String, QuestEntry> e : this.entries.entrySet()) {
            if (!start) {
                tasks.append("\n - ");
            }
            tasks.append(e.getValue().translation(server));
            start = false;
        }
        return tasks;
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        if (this.neededParentQuest != null)
            obj.addProperty("parent_id", this.neededParentQuest.toString());
        obj.addProperty("loot_table", this.loot.toString());
        obj.addProperty("repeat_delay", this.repeatDelay);
        obj.addProperty("repeat_daily", this.repeatDaily);
        obj.addProperty("task", this.questTaskString);
        JsonObject entries = new JsonObject();
        this.entries.forEach((res, entry) -> {
            JsonObject val = entry.serialize();
            val.addProperty("id", entry.getId().toString());
            entries.add(res, val);
        });
        obj.add("entries", entries);
        return obj;
    }

    public static Quest of(ResourceLocation id, JsonObject obj) {
        ImmutableMap.Builder<String, QuestEntry> builder = new ImmutableMap.Builder<>();
        JsonObject entries = GsonHelper.getAsJsonObject(obj, "entries");
        entries.entrySet().forEach(ent -> {
            if (!ent.getValue().isJsonObject())
                throw new JsonSyntaxException("Expected JsonObject for " + ent.getKey() + " but was " + ent.getValue());
            ResourceLocation entryID = new ResourceLocation(GsonHelper.getAsString(ent.getValue().getAsJsonObject(), "id"));
            builder.put(ent.getKey(), QuestEntryRegistry.deserialize(entryID, ent.getValue().getAsJsonObject()));
        });
        return new Quest(id,
                GsonHelper.getAsString(obj, "task"),
                obj.has("parent") ? new ResourceLocation(GsonHelper.getAsString(obj, "parent_id")) : null,
                new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")),
                GsonHelper.getAsInt(obj, "repeat_delay", 0),
                GsonHelper.getAsInt(obj, "repeat_daily", 0),
                builder.build());
    }
}
