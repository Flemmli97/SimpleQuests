package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.FishingEntry;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public class FishingTracker extends ProgressionTrackerBase<Collection<ItemStack>, FishingEntry> {

    public static final String FISHING_PROGRESS = FishingEntry.ID + ".progress";
    public static final ProgressionTrackerKey<Collection<ItemStack>, FishingEntry> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "fishing_tracker",
            FishingEntry.ID);

    private int value = 0;

    public FishingTracker(FishingEntry questEntry) {
        super(questEntry);
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, Collection<ItemStack> with) {
        int amount = 0;
        for (ItemStack stack : with) {
            if (this.questEntry().check(player, stack)) {
                amount += stack.getCount();
            }
        }
        this.value += amount;
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
        return new TranslatableComponent(FISHING_PROGRESS, this.value, this.questEntry().amount()).withStyle(form);
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
