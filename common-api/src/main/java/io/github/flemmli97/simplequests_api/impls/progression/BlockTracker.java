package io.github.flemmli97.simplequests_api.impls.progression;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.BlockInteractEntry;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public class BlockTracker extends ProgressionTrackerBase<Pair<BlockPos, Boolean>, BlockInteractEntry> {

    public static final String BLOCK_INTERACT_PROGRESS = BlockInteractEntry.ID + ".progress";
    public static final ProgressionTrackerKey<Pair<BlockPos, Boolean>, BlockInteractEntry> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "block_tracker",
            BlockInteractEntry.ID);

    private final Set<BlockPos> pos = new HashSet<>();
    private int amount;
    private final boolean allowDupes;

    public BlockTracker(BlockInteractEntry questEntry) {
        super(questEntry);
        this.allowDupes = questEntry.allowDupes();
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, Pair<BlockPos, Boolean> with) {
        if (this.questEntry().check(player, with.getFirst(), with.getSecond())) {
            if (!this.allowDupes && this.pos.contains(with.getFirst())) {
                if (!prog.getQuest().category.isSilent)
                    player.sendMessage(new TranslatableComponent("simplequests.interaction.block.dupe." + this.questEntry().use()).withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
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
        ChatFormatting form = ChatFormatting.DARK_GREEN;
        if (perc <= 0.35) {
            form = ChatFormatting.DARK_RED;
        } else if (perc <= 0.7) {
            form = ChatFormatting.GOLD;
        }
        return new TranslatableComponent(BLOCK_INTERACT_PROGRESS, this.pos.size(), this.questEntry().amount()).withStyle(form);
    }

    @Override
    public Tag save() {
        ListTag list = new ListTag();
        this.pos.forEach(pos -> list.add(BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .getOrThrow(false, SimpleQuestsAPI.LOGGER::error)));
        return list;
    }

    @Override
    public void load(Tag tag) {
        try {
            ListTag list = (ListTag) tag;
            list.forEach(t -> this.pos.add(BlockPos.CODEC.parse(NbtOps.INSTANCE, t).getOrThrow(true, SimpleQuestsAPI.LOGGER::error)));
        } catch (ClassCastException ignored) {
        }
    }
}
