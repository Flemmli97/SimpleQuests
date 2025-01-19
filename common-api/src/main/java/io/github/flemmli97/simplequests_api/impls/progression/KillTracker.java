package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.KillEntry;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class KillTracker extends ProgressionTrackerBase<LivingEntity, KillEntry> {

    public static final String KILL_PROGRESS = KillEntry.ID + ".progress";
    public static final ProgressionTrackerKey<LivingEntity, KillEntry> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "kill_tracker",
            KillEntry.ID);

    private int value = 0;

    public KillTracker(KillEntry questEntry) {
        super(questEntry);
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, LivingEntity with) {
        if (this.questEntry().check(player, with)) {
            this.value += 1;
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
        return new TranslatableComponent(KILL_PROGRESS, this.value, this.questEntry().amount()).withStyle(form);
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
