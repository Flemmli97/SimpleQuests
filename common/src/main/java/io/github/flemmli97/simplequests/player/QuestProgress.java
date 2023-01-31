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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

    private final Map<String, Integer> killCounter = new HashMap<>();
    private final Map<String, Set<UUID>> interactionCounter = new HashMap<>();
    private final Map<String, Set<BlockPos>> blockInteractionCounter = new HashMap<>();
    private final Map<String, Integer> craftingCounter = new HashMap<>();

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
                int killed = prog.killCounter.compute(name, (res, i) -> i == null ? 1 : ++i);
                return killed >= entry.amount();
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.EntityInteractEntry> createInteractionPredicate(ServerPlayer player, Entity entity) {
        return (name, entry, prog) -> {
            Set<UUID> interacted = prog.interactionCounter.computeIfAbsent(name, s -> new HashSet<>());
            if (interacted.contains(entity.getUUID())) {
                player.sendSystemMessage(Component.translatable(ConfigHandler.lang.get("simplequests.interaction.dupe")).withStyle(ChatFormatting.DARK_RED));
                return false;
            }
            if (entry.check(player, entity)) {
                interacted.add(entity.getUUID());
                int amount = interacted.size();
                return amount >= entry.amount();
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.BlockInteractEntry> createBlockInteractionPredicate(ServerPlayer player, BlockPos pos, boolean use) {
        return (name, entry, prog) -> {
            Set<BlockPos> interacted = prog.blockInteractionCounter.computeIfAbsent(name, s -> new HashSet<>());
            if (interacted.contains(pos)) {
                player.sendSystemMessage(Component.translatable(ConfigHandler.lang.get("simplequests.interaction.block.dupe." + entry.use())).withStyle(ChatFormatting.DARK_RED));
                return false;
            }
            if (entry.check(player, pos, use)) {
                interacted.add(pos);
                int amount = interacted.size();
                return amount >= entry.amount();
            }
            return false;
        };
    }

    public static SimpleQuestAPI.QuestEntryPredicate<QuestEntryImpls.CraftingEntry> createCraftingPredicate(ServerPlayer player, ItemStack stack, int amount) {
        return (name, entry, prog) -> {
            if (entry.check(player, stack)) {
                int counter = prog.craftingCounter.compute(name, (res, i) -> i == null ? amount : i + amount);
                return counter >= entry.amount();
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
        this.killCounter.forEach(kills::putInt);
        tag.put("KillCounter", kills);
        CompoundTag interactions = new CompoundTag();
        this.interactionCounter.forEach((res, i) -> {
            ListTag l = new ListTag();
            i.forEach(uuid -> l.add(NbtUtils.createUUID(uuid)));
            interactions.put(res, l);
        });
        tag.put("Interactions", interactions);
        CompoundTag blockInteractions = new CompoundTag();
        this.blockInteractionCounter.forEach((res, i) -> {
            ListTag l = new ListTag();
            i.forEach(pos -> l.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                    .getOrThrow(false, SimpleQuests.logger::error)));
            blockInteractions.put(res, l);
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
        kills.getAllKeys().forEach(key -> this.killCounter.put(key, kills.getInt(key)));
        CompoundTag interactions = tag.getCompound("Interactions");
        interactions.getAllKeys().forEach(key -> {
            ListTag l = interactions.getList(key, Tag.TAG_INT_ARRAY);
            l.forEach(t -> this.interactionCounter.computeIfAbsent(key, k -> new HashSet<>())
                    .add(NbtUtils.loadUUID(t)));
        });
        CompoundTag blockInteractions = tag.getCompound("BlockInteractions");
        blockInteractions.getAllKeys().forEach(key -> {
            ListTag l = blockInteractions.getList(key, Tag.TAG_INT_ARRAY);
            l.forEach(t -> this.blockInteractionCounter.computeIfAbsent(key, k -> new HashSet<>())
                    .add(BlockPos.CODEC.parse(NbtOps.INSTANCE, t).getOrThrow(true, SimpleQuests.logger::error)));
        });
    }

    enum SubmitType {
        COMPLETE,
        PARTIAL,
        NOTHING
    }
}
