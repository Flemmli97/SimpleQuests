package io.github.flemmli97.simplequests.player;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.SimpleQuestAPI;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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

    private final Map<String, ProgressionTracker<Integer, QuestEntryImpls.KillEntry>> killCounter = new HashMap<>();
    private final Map<String, ProgressionTracker<Integer, QuestEntryImpls.CraftingEntry>> craftingCounter = new HashMap<>();
    private final Map<String, ProgressionTracker<UUID, QuestEntryImpls.EntityInteractEntry>> interactionCounter = new HashMap<>();
    private final Map<String, ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry>> blockInteractionCounter = new HashMap<>();

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

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.KillEntry> createKillPredicate(ServerPlayer player, LivingEntity entity) {
        return (name, entry, prog) -> {
            if (entry.predicate().matches(player, entity)) {
                return prog.killCounter.computeIfAbsent(name, (res) -> ProgressionTrackerImpl.createKillTracker(entry)).apply(1);
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.EntityInteractEntry> createInteractionPredicate(ServerPlayer player, Entity entity) {
        return (name, entry, prog) -> {
            ProgressionTracker<UUID, QuestEntryImpls.EntityInteractEntry> interacted = prog.interactionCounter.computeIfAbsent(name, s -> ProgressionTrackerImpl.createEntityInteractTracker(entry));
            if (!interacted.isApplicable(entity.getUUID())) {
                player.sendMessage(new TranslatableComponent(ConfigHandler.lang.get("simplequests.interaction.dupe")).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                return false;
            }
            if (entry.check(player, entity)) {
                return interacted.apply(entity.getUUID());
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.BlockInteractEntry> createBlockInteractionPredicate(ServerPlayer player, BlockPos pos, boolean use) {
        return (name, entry, prog) -> {
            ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry> interacted = prog.blockInteractionCounter.computeIfAbsent(name, s -> ProgressionTrackerImpl.createBlockInteractTracker(entry));
            if (!interacted.isApplicable(pos)) {
                player.sendMessage(new TranslatableComponent(ConfigHandler.lang.get("simplequests.interaction.block.dupe." + entry.use())).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                return false;
            }
            if (entry.check(player, pos, use)) {
                return interacted.apply(pos);
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.CraftingEntry> createCraftingPredicate(ServerPlayer player, ItemStack stack, int amount) {
        return (name, entry, prog) -> {
            if (entry.check(player, stack)) {
                return prog.craftingCounter.computeIfAbsent(name, (res) -> ProgressionTrackerImpl.createCraftingTracker(entry)).apply(amount);
            }
            return false;
        };
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

    public SubmitType submit(ServerPlayer player, String trigger) {
        boolean any = false;
        for (Map.Entry<String, QuestEntry> entry : this.quest.entries.entrySet()) {
            if (this.entries.contains(entry.getKey()))
                continue;
            if (entry.getValue().submit(player)) {
                this.entries.add(entry.getKey());
                any = true;
            }
        }
        boolean b = this.isCompleted(trigger);
        return b ? SubmitType.COMPLETE : any ? SubmitType.PARTIAL : SubmitType.NOTHING;
    }

    @SuppressWarnings("unchecked")
    public <T extends QuestEntry> Set<Pair<String, T>> tryFullFill(Class<T> clss, SimpleQuestAPI.QuestEntryPredicate<T> pred) {
        Set<Pair<String, T>> fullfilled = new HashSet<>();
        for (Map.Entry<String, QuestEntry> e : this.quest.entries.entrySet()) {
            if (this.entries.contains(e.getKey()))
                continue;
            if (clss.isInstance(e.getValue())) {
                T entry = (T) e.getValue();
                if (pred.matches(e.getKey(), entry, this)) {
                    fullfilled.add(Pair.of(e.getKey(), entry));
                    this.entries.add(e.getKey());
                }
            }
        }
        return fullfilled;
    }

    public boolean isCompleted(String trigger) {
        return this.quest.questSubmissionTrigger.equals(trigger) && this.entries.containsAll(this.quest.entries.keySet());
    }

    public List<String> finishedTasks() {
        return ImmutableList.copyOf(this.entries);
    }

    public MutableComponent killProgress(ServerPlayer player, String entry) {
        ProgressionTracker<Integer, QuestEntryImpls.KillEntry> tracker = this.killCounter.get(entry);
        if (tracker == null)
            return null;
        return tracker.formattedProgress(player, this);
    }

    public MutableComponent craftingProgress(ServerPlayer player, String entry) {
        ProgressionTracker<Integer, QuestEntryImpls.CraftingEntry> tracker = this.craftingCounter.get(entry);
        if (tracker == null)
            return null;
        return tracker.formattedProgress(player, this);
    }

    public MutableComponent interactProgress(ServerPlayer player, String entry) {
        ProgressionTracker<UUID, QuestEntryImpls.EntityInteractEntry> tracker = this.interactionCounter.get(entry);
        if (tracker == null)
            return null;
        return tracker.formattedProgress(player, this);
    }

    public MutableComponent blockInteractProgress(ServerPlayer player, String entry) {
        ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry> tracker = this.blockInteractionCounter.get(entry);
        if (tracker == null)
            return null;
        return tracker.formattedProgress(player, this);
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
        this.killCounter.forEach((k, v) -> kills.put(k, v.save()));
        tag.put("KillCounter", kills);
        CompoundTag crafting = new CompoundTag();
        this.craftingCounter.forEach((k, v) -> crafting.put(k, v.save()));
        tag.put("CraftingCounter", crafting);
        CompoundTag interactions = new CompoundTag();
        this.interactionCounter.forEach((res, i) -> {
            interactions.put(res, i.save());
        });
        tag.put("Interactions", interactions);
        CompoundTag blockInteractions = new CompoundTag();
        this.blockInteractionCounter.forEach((res, i) -> {
            blockInteractions.put(res, i.save());
        });
        tag.put("BlockInteractions", blockInteractions);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.quest = QuestsManager.instance().getAllQuests().get(new ResourceLocation(tag.getString("Quest")));
        if (this.quest == null) {
            SimpleQuests.logger.error("Cant find quest with id " + tag.getString("Quest") + ". Skipping");
            throw new IllegalStateException();
        }
        ListTag list = tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.entries.add(t.getAsString()));

        CompoundTag kills = tag.getCompound("KillCounter");
        kills.getAllKeys().forEach(key -> this.loadTracker(ProgressionTrackerImpl::createKillTracker, this.killCounter, key, kills));
        CompoundTag crafting = tag.getCompound("CraftingCounter");
        crafting.getAllKeys().forEach(key -> this.loadTracker(ProgressionTrackerImpl::createCraftingTracker, this.craftingCounter, key, crafting));
        CompoundTag interactions = tag.getCompound("Interactions");
        interactions.getAllKeys().forEach(key -> this.loadTracker(ProgressionTrackerImpl::createEntityInteractTracker, this.interactionCounter, key, interactions));
        CompoundTag blockInteractions = tag.getCompound("BlockInteractions");
        blockInteractions.getAllKeys().forEach(key -> this.loadTracker(ProgressionTrackerImpl::createBlockInteractTracker, this.blockInteractionCounter, key, blockInteractions));
    }

    @SuppressWarnings("unchecked")
    private <E, F extends QuestEntry, T extends ProgressionTracker<E, F>> void loadTracker(Function<F, T> func, Map<String, T> map, String name, CompoundTag tag) {
        if (this.quest == null) {
            SimpleQuests.logger.error("Quest not set. This shouldn't be!");
            throw new IllegalStateException();
        }
        try {
            T entry = func.apply((F) this.quest.entries.get(name));
            entry.load(tag.get(name));
            map.putIfAbsent(name, entry);
        } catch (ClassCastException e) {
            SimpleQuests.logger.error("Couldn't find quest entry for tracker {}", name);
        }
    }

    enum SubmitType {
        COMPLETE,
        PARTIAL,
        NOTHING
    }
}
