package io.github.flemmli97.simplequests.player;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class QuestProgress {

    private final List<String> entries = new ArrayList<>();

    private final Map<String, Integer> killCounter = new HashMap<>();
    private final Map<String, Set<UUID>> interactionCounter = new HashMap<>();

    private final Map<String, Function<PlayerData, Boolean>> tickables = new HashMap<>();

    private Quest quest;

    public QuestProgress(Quest quest, PlayerData data) {
        this.quest = quest;
        this.setup(data);
        if (!this.tickables.isEmpty())
            data.addTickableProgress(this);
    }

    public QuestProgress(CompoundTag tag, PlayerData data) {
        this.load(tag);
        this.setup(data);
        if (!this.tickables.isEmpty())
            data.addTickableProgress(this);
    }

    private void setup(PlayerData data) {
        this.quest.entries.forEach((s, e) -> {
            e.onAccept(data);
            if (!this.entries.contains(s)) {
                Function<PlayerData, Boolean> ticker = e.tickable();
                if (ticker != null)
                    this.tickables.put(s, ticker);
            }
        });
    }

    public Quest getQuest() {
        return this.quest;
    }

    public SubmitType submit(ServerPlayer player) {
        boolean any = false;
        for (Map.Entry<String, QuestEntry> entry : this.quest.entries.entrySet()) {
            if (this.entries.contains(entry.getKey()))
                continue;
            if (entry.getValue().submit(player)) {
                this.entries.add(entry.getKey());
                any = true;
            }
        }
        boolean b = this.isCompleted();
        return b ? SubmitType.COMPLETE : any ? SubmitType.PARTIAL : SubmitType.NOTHING;
    }

    public Set<QuestEntry> onKill(ServerPlayer player, LivingEntity entity) {
        Set<QuestEntry> fullfilled = new HashSet<>();
        for (Map.Entry<String, QuestEntry> e : this.quest.entries.entrySet()) {
            if (e.getValue() instanceof QuestEntryImpls.KillEntry killEntry) {
                if (this.entries.contains(e.getKey()))
                    continue;
                if (killEntry.predicate().matches(player, entity)) {
                    int killed = this.killCounter.compute(e.getKey(), (res, i) -> i == null ? 1 : ++i);
                    if (killed >= killEntry.amount()) {
                        fullfilled.add(killEntry);
                        this.entries.add(e.getKey());
                    }
                }
            }
        }
        return fullfilled;
    }

    public Set<QuestEntry> onInteractWith(ServerPlayer player, Entity entity) {
        Set<QuestEntry> fullfilled = new HashSet<>();
        for (Map.Entry<String, QuestEntry> e : this.quest.entries.entrySet()) {
            if (e.getValue() instanceof QuestEntryImpls.EntityInteractEntry entry) {
                Set<UUID> interacted = this.interactionCounter.computeIfAbsent(e.getKey(), s -> new HashSet<>());
                if (this.entries.contains(e.getKey()))
                    continue;
                if (interacted.contains(entity.getUUID())) {
                    player.sendMessage(new TranslatableComponent(ConfigHandler.lang.get("simplequests.interaction.dupe")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                    continue;
                }
                if (entry.check(player, entity)) {
                    interacted.add(entity.getUUID());
                    int amount = interacted.size();
                    if (amount >= entry.amount()) {
                        fullfilled.add(entry);
                        this.entries.add(e.getKey());
                    }
                }
            }
        }
        return fullfilled;
    }

    public boolean isCompleted() {
        return this.entries.containsAll(this.quest.entries.keySet());
    }

    public int killedOf(ResourceLocation loc) {
        return this.killCounter.getOrDefault(loc, 0);
    }

    public List<String> finishedTasks() {
        return ImmutableList.copyOf(this.entries);
    }

    public Pair<Boolean, Set<QuestEntry>> tickProgress(PlayerData data) {
        Set<QuestEntry> fullfilled = new HashSet<>();
        this.tickables.entrySet().removeIf(e -> {
            if (e.getValue().apply(data)) {
                fullfilled.add(this.quest.entries.get(e.getKey()));
                this.entries.add(e.getKey());
                return true;
            }
            return false;
        });
        return Pair.of(this.tickables.isEmpty(), fullfilled);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Quest", this.quest.id.toString());
        ListTag list = new ListTag();
        this.entries.forEach(res -> list.add(StringTag.valueOf(res)));
        tag.put("FinishedEntries", list);
        CompoundTag kills = new CompoundTag();
        this.killCounter.forEach((res, i) -> kills.putInt(res.toString(), i));
        tag.put("KillCounter", kills);
        CompoundTag interactions = new CompoundTag();
        this.interactionCounter.forEach((res, i) -> {
            ListTag l = new ListTag();
            i.forEach(uuid -> l.add(NbtUtils.createUUID(uuid)));
            interactions.put(res, l);
        });
        tag.put("Interactions", interactions);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.quest = QuestsManager.instance().getQuests().get(new ResourceLocation(tag.getString("Quest")));
        if (this.quest == null)
            SimpleQuests.logger.error("Cant find quest with id " + tag.getString("Quest"));
        ListTag list = tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.entries.add(t.getAsString()));
        CompoundTag kills = tag.getCompound("KillCounter");
        kills.getAllKeys().forEach(key -> this.killCounter.put(key, kills.getInt(key)));
        CompoundTag interactions = tag.getCompound("Interactions");
        interactions.getAllKeys().forEach(key -> {
            ListTag l = interactions.getList(key, Tag.TAG_INT_ARRAY);
            l.forEach(t -> this.interactionCounter.computeIfAbsent(key, k -> new HashSet<>())
                    .add(NbtUtils.loadUUID(t)));
        });
    }

    enum SubmitType {
        COMPLETE,
        PARTIAL,
        NOTHING
    }
}
