package io.github.flemmli97.simplequests.player;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.ProgressionTrackerKey;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ProgressionTrackerImpl<T, E extends QuestEntry> implements ProgressionTracker<T, E> {

    private final E questEntry;

    public static class FishingTracker extends ProgressionTrackerImpl<Integer, QuestEntryImpls.FishingEntry> {

        public static final String FISHING_PROGRESS = QuestEntryImpls.FishingEntry.ID + ".progress";
        public static final ProgressionTrackerKey<Integer, QuestEntryImpls.FishingEntry> KEY = new ProgressionTrackerKey<>(SimpleQuests.MODID, "fishing_tracker");
        private int value = 0;

        public FishingTracker(QuestEntryImpls.FishingEntry questEntry) {
            super(questEntry);
        }

        @Override
        public boolean isApplicable(Integer value) {
            return true;
        }

        @Override
        public boolean apply(Integer value) {
            this.value += value;
            return this.value >= this.questEntry().amount();
        }

        @Override
        public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
            float perc = this.value / (float) this.questEntry().amount();
            ChatFormatting form = ChatFormatting.DARK_GREEN;
            if (perc <= 0.35) {
                form = ChatFormatting.DARK_RED;
            } else if (perc <= 0.7) {
                form = ChatFormatting.GOLD;
            }
            return new TranslatableComponent(ConfigHandler.LANG.get(player, FISHING_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
        }

        @Override
        public Tag save() {
            return IntTag.valueOf(this.value);
        }

        @Override
        public void load(Tag tag) {
            try {
                this.value = ((NumericTag) tag).getAsInt();
            } catch (ClassCastException ignored) {
            }
        }
    }

    public static class KillTracker extends ProgressionTrackerImpl<Integer, QuestEntryImpls.KillEntry> {

        public static final String KILL_PROGRESS = QuestEntryImpls.KillEntry.ID + ".progress";
        public static final ProgressionTrackerKey<Integer, QuestEntryImpls.KillEntry> KEY = new ProgressionTrackerKey<>(SimpleQuests.MODID, "kill_tracker");

        private int value = 0;

        public KillTracker(QuestEntryImpls.KillEntry questEntry) {
            super(questEntry);
        }

        @Override
        public boolean isApplicable(Integer value) {
            return true;
        }

        @Override
        public boolean apply(Integer value) {
            this.value += value;
            return this.value >= this.questEntry().amount();
        }

        @Override
        public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
            float perc = this.value / (float) this.questEntry().amount();
            ChatFormatting form = ChatFormatting.DARK_GREEN;
            if (perc <= 0.35) {
                form = ChatFormatting.DARK_RED;
            } else if (perc <= 0.7) {
                form = ChatFormatting.GOLD;
            }
            return new TranslatableComponent(ConfigHandler.LANG.get(player, KILL_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
        }

        @Override
        public Tag save() {
            return IntTag.valueOf(this.value);
        }

        @Override
        public void load(Tag tag) {
            try {
                this.value = ((NumericTag) tag).getAsInt();
            } catch (ClassCastException ignored) {
            }
        }
    }

    public static class CraftingTracker extends ProgressionTrackerImpl<Integer, QuestEntryImpls.CraftingEntry> {

        public static final String CRAFTING_PROGRESS = QuestEntryImpls.CraftingEntry.ID + ".progress";
        public static final ProgressionTrackerKey<Integer, QuestEntryImpls.CraftingEntry> KEY = new ProgressionTrackerKey<>(SimpleQuests.MODID, "crafting_tracker");

        private int value = 0;

        public CraftingTracker(QuestEntryImpls.CraftingEntry questEntry) {
            super(questEntry);
        }

        @Override
        public boolean isApplicable(Integer value) {
            return true;
        }

        @Override
        public boolean apply(Integer value) {
            this.value += value;
            return this.value >= this.questEntry().amount();
        }

        @Override
        public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
            float perc = this.value / (float) this.questEntry().amount();
            ChatFormatting form = ChatFormatting.DARK_GREEN;
            if (perc <= 0.35) {
                form = ChatFormatting.DARK_RED;
            } else if (perc <= 0.7) {
                form = ChatFormatting.GOLD;
            }
            return new TranslatableComponent(ConfigHandler.LANG.get(player, CRAFTING_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
        }

        @Override
        public Tag save() {
            return IntTag.valueOf(this.value);
        }

        @Override
        public void load(Tag tag) {
            try {
                this.value = ((NumericTag) tag).getAsInt();
            } catch (ClassCastException ignored) {
            }
        }
    }

    public static class BlockTracker extends ProgressionTrackerImpl<BlockPos, QuestEntryImpls.BlockInteractEntry> {

        public static final String BLOCK_INTERACT_PROGRESS = QuestEntryImpls.BlockInteractEntry.ID + ".progress";
        public static final ProgressionTrackerKey<BlockPos, QuestEntryImpls.BlockInteractEntry> KEY = new ProgressionTrackerKey<>(SimpleQuests.MODID, "block_tracker");

        private final Set<BlockPos> pos = new HashSet<>();
        private int amount;
        private final boolean allowDupes;

        public BlockTracker(QuestEntryImpls.BlockInteractEntry questEntry) {
            super(questEntry);
            this.allowDupes = questEntry.allowDupes();
        }

        @Override
        public boolean isApplicable(BlockPos value) {
            return this.allowDupes || !this.pos.contains(value);
        }

        @Override
        public boolean apply(BlockPos value) {
            if (this.allowDupes || !this.pos.contains(value)) {
                this.pos.add(value);
                this.amount++;
            }
            return this.amount >= this.questEntry().amount();
        }

        @Override
        public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
            float perc = this.amount / (float) this.questEntry().amount();
            ChatFormatting form = ChatFormatting.DARK_GREEN;
            if (perc <= 0.35) {
                form = ChatFormatting.DARK_RED;
            } else if (perc <= 0.7) {
                form = ChatFormatting.GOLD;
            }
            return new TranslatableComponent(ConfigHandler.LANG.get(player, BLOCK_INTERACT_PROGRESS), this.pos.size(), this.questEntry().amount()).withStyle(form);
        }

        @Override
        public Tag save() {
            ListTag list = new ListTag();
            this.pos.forEach(pos -> list.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                    .getOrThrow(false, SimpleQuests.LOGGER::error)));
            return list;
        }

        @Override
        public void load(Tag tag) {
            try {
                ListTag list = (ListTag) tag;
                list.forEach(t -> this.pos.add(BlockPos.CODEC.parse(NbtOps.INSTANCE, t).getOrThrow(true, SimpleQuests.LOGGER::error)));
            } catch (ClassCastException ignored) {
            }
        }
    }

    public static class EntityTracker extends ProgressionTrackerImpl<UUID, QuestEntryImpls.EntityInteractEntry> {

        public static final String ENTITY_INTERACT_PROGRESS = QuestEntryImpls.EntityInteractEntry.ID + ".progress";
        public static final ProgressionTrackerKey<UUID, QuestEntryImpls.EntityInteractEntry> KEY = new ProgressionTrackerKey<>(SimpleQuests.MODID, "entity_tracker");

        private final Set<UUID> entities = new HashSet<>();

        public EntityTracker(QuestEntryImpls.EntityInteractEntry questEntry) {
            super(questEntry);
        }

        @Override
        public boolean isApplicable(UUID value) {
            return !this.entities.contains(value);
        }

        @Override
        public boolean apply(UUID value) {
            this.entities.add(value);
            return this.entities.size() >= this.questEntry().amount();
        }

        @Override
        public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
            float perc = this.entities.size() / (float) this.questEntry().amount();
            ChatFormatting form = ChatFormatting.DARK_GREEN;
            if (perc <= 0.35) {
                form = ChatFormatting.DARK_RED;
            } else if (perc <= 0.7) {
                form = ChatFormatting.GOLD;
            }
            return new TranslatableComponent(ConfigHandler.LANG.get(player, ENTITY_INTERACT_PROGRESS), this.entities.size(), this.questEntry().amount()).withStyle(form);
        }

        @Override
        public Tag save() {
            ListTag list = new ListTag();
            this.entities.forEach(uuid -> list.add(NbtUtils.createUUID(uuid)));
            return list;
        }

        @Override
        public void load(Tag tag) {
            try {
                ListTag list = (ListTag) tag;
                list.forEach(t -> this.entities.add(NbtUtils.loadUUID(t)));
            } catch (ClassCastException ignored) {
            }
        }
    }

    public ProgressionTrackerImpl(E questEntry) {
        this.questEntry = questEntry;
    }

    @Override
    public E questEntry() {
        return this.questEntry;
    }
}
