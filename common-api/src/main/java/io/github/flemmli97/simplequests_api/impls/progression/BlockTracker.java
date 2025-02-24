package io.github.flemmli97.simplequests_api.impls.progression;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.tasks.BlockInteractTask;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public class BlockTracker extends ProgressionTrackerBase<Pair<BlockPos, Boolean>, BlockInteractTask.BlockInteractTaskResolved> {

    public static final String BLOCK_INTERACT_PROGRESS = BlockInteractTask.ID + ".progress";
    public static final ProgressionTrackerKey<Pair<BlockPos, Boolean>, BlockInteractTask.BlockInteractTaskResolved> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "block_tracker",
            BlockInteractTask.ID);

    private final Set<BlockPos> pos = new HashSet<>();
    private int amount;
    private final boolean allowDupes;

    public BlockTracker(BlockInteractTask.BlockInteractTaskResolved questEntry) {
        super(questEntry);
        this.allowDupes = questEntry.allowDupes();
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, Pair<BlockPos, Boolean> with) {
        if (this.questEntry().check(player, with.getFirst(), with.getSecond())) {
            if (!this.allowDupes && this.pos.contains(with.getFirst())) {
                if (!prog.getQuest().category.isSilent)
                    player.sendSystemMessage(Component.translatable(BlockInteractTask.ID + "block.dupe." + this.questEntry().use()).withStyle(ChatFormatting.DARK_RED));
                return false;
            }
            this.pos.add(with.getFirst());
            this.amount++;
            return this.amount >= this.questEntry().amount();
        }
        return false;
    }

    @Override
    public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
        float perc = this.amount / (float) this.questEntry().amount();
        return Component.translatable(BLOCK_INTERACT_PROGRESS, this.amount, this.questEntry().amount())
                .withStyle(ProgressionTrackerBase.of(perc));
    }

    @Override
    public Tag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        this.pos.forEach(pos -> list.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .getOrThrow(false, SimpleQuestsAPI.LOGGER::error)));
        tag.put("interacted", list);
        tag.putInt("amount", this.amount);
        return list;
    }

    @Override
    public void load(Tag tag) {
        try {
            CompoundTag compound = (CompoundTag) tag;
            ListTag list = compound.getList("interacted", Tag.TAG_INT_ARRAY);
            list.forEach(t -> this.pos.add(BlockPos.CODEC.parse(NbtOps.INSTANCE, t).getOrThrow(true, SimpleQuestsAPI.LOGGER::error)));
            this.amount = compound.getInt("amount");
        } catch (ClassCastException ignored) {
        }
    }
}
