package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.QuestEntryRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Quest implements Comparable<Quest> {

    public final Map<String, QuestEntry> entries;

    public final ResourceLocation id;
    public final List<ResourceLocation> neededParentQuests;
    public final ResourceLocation loot;

    public final int repeatDelay, repeatDaily;

    public final String questTaskString;

    public final boolean redoParent, needsUnlock, isDailyQuest;

    public final int sortingId;

    private final ItemStack icon;

    private Quest(ResourceLocation id, String questTaskString, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock, ResourceLocation loot, ItemStack icon, int repeatDelay, int repeatDaily, int sortingId, Map<String, QuestEntry> entries, boolean isDailyQuest) {
        this.id = id;
        this.questTaskString = questTaskString;
        this.neededParentQuests = parents;
        this.redoParent = redoParent;
        this.needsUnlock = needsUnlock;
        this.repeatDelay = repeatDelay;
        this.repeatDaily = repeatDaily;
        this.entries = entries;
        this.loot = loot;
        this.sortingId = sortingId;
        this.icon = icon;
        this.isDailyQuest = isDailyQuest;
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
        ImmutableList.Builder<ResourceLocation> parents = new ImmutableList.Builder<>();
        JsonElement e = obj.get("parent_id");
        if (e != null) {
            if (e.isJsonPrimitive() && !e.getAsString().isEmpty())
                parents.add(new ResourceLocation(e.getAsString()));
            else if (e.isJsonArray()) {
                ImmutableList.Builder<ResourceLocation> list = new ImmutableList.Builder<>();
                e.getAsJsonArray().forEach(ea -> {
                    if (ea.isJsonPrimitive() && !ea.getAsString().isEmpty()) {
                        list.add(new ResourceLocation(ea.getAsString()));
                    }
                });
            }
        }
        return new Quest(id,
                GsonHelper.getAsString(obj, "task"),
                parents.build(),
                GsonHelper.getAsBoolean(obj, "redo_parent", false),
                GsonHelper.getAsBoolean(obj, "need_unlock", false),
                new ResourceLocation(GsonHelper.getAsString(obj, "loot_table")),
                ParseHelper.icon(obj, "icon", Items.PAPER),
                ParseHelper.tryParseTime(obj, "repeat_delay", 0),
                GsonHelper.getAsInt(obj, "repeat_daily", 0),
                GsonHelper.getAsInt(obj, "sorting_id", 0),
                builder.build(),
                GsonHelper.getAsBoolean(obj, "daily_quest", false));
    }

    public JsonObject serialize(boolean withId, boolean full) {
        JsonObject obj = new JsonObject();
        if (withId)
            obj.addProperty("id", this.id.toString());
        obj.addProperty("task", this.questTaskString);
        if (!this.neededParentQuests.isEmpty() || full) {
            if (this.neededParentQuests.size() == 1)
                obj.addProperty("parent_id", this.neededParentQuests.get(0).toString());
            else {
                JsonArray arr = new JsonArray();
                this.neededParentQuests.forEach(r -> arr.add(r.toString()));
                obj.add("parent_id", arr);
            }
        }
        if (this.redoParent || full)
            obj.addProperty("redo_parent", this.redoParent);
        if (this.needsUnlock || full)
            obj.addProperty("need_unlock", this.needsUnlock);
        obj.addProperty("loot_table", this.loot.toString());
        ParseHelper.writeItemStackToJson(this.icon, full ? null : Items.PAPER)
                .ifPresent(icon -> obj.add("icon", icon));
        if (this.repeatDelay != 0 || full)
            obj.addProperty("repeat_delay", this.repeatDelay);
        if (this.repeatDaily != 0 || full)
            obj.addProperty("repeat_daily", this.repeatDaily);
        if (this.sortingId != 0 || full)
            obj.addProperty("sorting_id", this.sortingId);
        if (this.isDailyQuest || full)
            obj.addProperty("daily_quest", this.isDailyQuest);
        JsonObject entries = new JsonObject();
        this.entries.forEach((res, entry) -> {
            JsonObject val = entry.serialize();
            val.addProperty("id", entry.getId().toString());
            entries.add(res, val);
        });
        obj.add("entries", entries);
        return obj;
    }

    public MutableComponent getFormatted(MinecraftServer server, ChatFormatting... subFormatting) {
        MutableComponent main = new TextComponent("").append(new TextComponent(this.questTaskString).withStyle(ChatFormatting.LIGHT_PURPLE));
        for (MutableComponent tasks : this.getFormattedTasks(server)) {
            if (subFormatting != null)
                main.append("\n").append(tasks.withStyle(subFormatting));
            else
                main.append("\n").append(tasks);
        }
        return main;
    }

    public List<MutableComponent> getFormattedTasks(MinecraftServer server) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestEntry> e : this.entries.entrySet()) {
            list.add(new TextComponent(" - ").append(e.getValue().translation(server)));
        }
        return list;
    }

    public List<MutableComponent> getFormattedGuiTasks(ServerPlayer player) {
        List<MutableComponent> list = new ArrayList<>();
        for (Map.Entry<String, QuestEntry> e : this.entries.entrySet()) {
            if (!(e.getValue() instanceof QuestEntryImpls.ItemEntry ing))
                list.add(new TextComponent(" - ").append(e.getValue().translation(player.getServer())));
            else {
                List<MutableComponent> wrapped = SimpleQuests.getHandler().wrapForGui(player, ing);
                boolean start = true;
                for (MutableComponent comp : wrapped) {
                    if (start) {
                        list.add(new TextComponent(" - ").append(comp));
                        start = false;
                    } else
                        list.add(new TextComponent("   ").append(comp));
                }
            }
        }
        return list;
    }

    public ItemStack getIcon() {
        return this.icon.copy();
    }

    @Override
    public int compareTo(@NotNull Quest quest) {
        if (this.sortingId == quest.sortingId) {
            if (this.neededParentQuests.isEmpty() && !quest.neededParentQuests.isEmpty())
                return -1;
            if (!this.neededParentQuests.isEmpty() && quest.neededParentQuests.isEmpty())
                return 1;
            return this.id.compareTo(quest.id);
        }
        return Integer.compare(this.sortingId, quest.sortingId);
    }
}
