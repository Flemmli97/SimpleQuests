package io.github.flemmli97.simplequests_api.impls.progression;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.tasks.CraftingTask;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class CraftingTracker extends ProgressionTrackerBase<Pair<ItemStack, Integer>, CraftingTask.CraftingTaskResolved> {

    public static final String CRAFTING_PROGRESS = CraftingTask.ID + ".progress";
    public static final ProgressionTrackerKey<Pair<ItemStack, Integer>, CraftingTask.CraftingTaskResolved> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "crafting_tracker",
            CraftingTask.ID);

    private int value = 0;

    public CraftingTracker(CraftingTask.CraftingTaskResolved questEntry) {
        super(questEntry);
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, Pair<ItemStack, Integer> with) {
        if (this.questEntry().check(player, with.getFirst())) {
            this.value += with.getSecond();
            return this.value >= this.questEntry().amount();
        }
        return false;
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
        return Component.translatable(CRAFTING_PROGRESS, this.value, this.questEntry().amount()).withStyle(form);
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
