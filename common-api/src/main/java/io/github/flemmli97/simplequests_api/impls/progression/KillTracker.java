package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.tasks.KillTask;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class KillTracker extends ProgressionTrackerBase<LivingEntity, KillTask.KillTaskResolved> {

    public static final String KILL_PROGRESS = KillTask.ID + ".progress";
    public static final ProgressionTrackerKey<LivingEntity, KillTask.KillTaskResolved> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "kill_tracker",
            KillTask.ID);

    private int value = 0;

    public KillTracker(KillTask.KillTaskResolved questEntry) {
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
        return Component.translatable(KILL_PROGRESS, this.value, this.questEntry().amount())
                .withStyle(ProgressionTrackerBase.of(perc));
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
