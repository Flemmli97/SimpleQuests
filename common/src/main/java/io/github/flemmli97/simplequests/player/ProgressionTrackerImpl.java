package io.github.flemmli97.simplequests.player;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class ProgressionTrackerImpl<T, E extends QuestEntry> implements ProgressionTracker<T, E> {

    private final E questEntry;

    public static final String FISHING_PROGRESS = QuestEntryImpls.FishingEntry.ID + ".progress";

    public static ProgressionTracker<Integer, QuestEntryImpls.FishingEntry> createFishingTracker(QuestEntryImpls.FishingEntry entry) {
        return new ProgressionTrackerImpl<>(entry) {
            private int value = 0;

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
                return Component.translatable(ConfigHandler.LANG.get(player, FISHING_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
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
        };
    }

    public static final String KILL_PROGRESS = QuestEntryImpls.KillEntry.ID + ".progress";

    public static ProgressionTracker<Integer, QuestEntryImpls.KillEntry> createKillTracker(QuestEntryImpls.KillEntry entry) {
        return new ProgressionTrackerImpl<>(entry) {

            private int value = 0;

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
                return Component.translatable(ConfigHandler.LANG.get(player, KILL_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
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
        };
    }

    public static final String CRAFTING_PROGRESS = QuestEntryImpls.CraftingEntry.ID + ".progress";

    public static ProgressionTracker<Integer, QuestEntryImpls.CraftingEntry> createCraftingTracker(QuestEntryImpls.CraftingEntry entry) {
        return new ProgressionTrackerImpl<>(entry) {
            private int value = 0;

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
                return Component.translatable(ConfigHandler.LANG.get(player, CRAFTING_PROGRESS), this.value, this.questEntry().amount()).withStyle(form);
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
        };
    }

    public static final String BLOCK_INTERACT_PROGRESS = QuestEntryImpls.BlockInteractEntry.ID + ".progress";

    public static ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry> createBlockInteractTracker(QuestEntryImpls.BlockInteractEntry entry) {
        return createBlockInteractTracker(entry, false);
    }

    public static ProgressionTracker<BlockPos, QuestEntryImpls.BlockInteractEntry> createBlockInteractTracker(QuestEntryImpls.BlockInteractEntry entry, boolean allowDupe) {
        return new ProgressionTrackerImpl<>(entry) {

            private final Set<BlockPos> pos = new HashSet<>();
            private int amount;
            private boolean allowDupes = allowDupe;

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
                return Component.translatable(ConfigHandler.LANG.get(player, BLOCK_INTERACT_PROGRESS), this.pos.size(), this.questEntry().amount()).withStyle(form);
            }

            @Override
            public Tag save() {
                CompoundTag tag = new CompoundTag();
                ListTag list = new ListTag();
                this.pos.forEach(pos -> list.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                        .getOrThrow(false, SimpleQuests.LOGGER::error)));
                tag.put("Pos", list);
                tag.putBoolean("Dupes", this.allowDupes);
                return list;
            }

            @Override
            public void load(Tag tag) {
                try {
                    CompoundTag compoundTag = (CompoundTag) tag;
                    ListTag list = compoundTag.getList("Pos", Tag.TAG_INT_ARRAY);
                    list.forEach(t -> this.pos.add(BlockPos.CODEC.parse(NbtOps.INSTANCE, t).getOrThrow(true, SimpleQuests.LOGGER::error)));
                    this.allowDupes = compoundTag.getBoolean("Dupes");
                } catch (ClassCastException ignored) {
                }
            }
        };
    }

    public static final String ENTITY_INTERACT_PROGRESS = QuestEntryImpls.EntityInteractEntry.ID + ".progress";

    public static ProgressionTracker<UUID, QuestEntryImpls.EntityInteractEntry> createEntityInteractTracker(QuestEntryImpls.EntityInteractEntry entry) {
        return new ProgressionTrackerImpl<>(entry) {

            private final Set<UUID> entities = new HashSet<>();

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
                return Component.translatable(ConfigHandler.LANG.get(player, ENTITY_INTERACT_PROGRESS), this.entities.size(), this.questEntry().amount()).withStyle(form);
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
        };
    }

    public ProgressionTrackerImpl(E questEntry) {
        this.questEntry = questEntry;
    }

    @Override
    public E questEntry() {
        return this.questEntry;
    }
}
