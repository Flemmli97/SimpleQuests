package io.github.flemmli97.simplequests_api.impls.entries.multi;

import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class QuestEntryUtil {

    public static int getAmount(NumberProvider provider, LootContext ctx, PlayerQuestData data, ResourceLocation quest) {
        if (!(provider instanceof QuestNumberProvider.ContextMultiplierNumberProvider mult))
            return provider.getInt(ctx);
        return Math.round(mult.getFloatWith(ctx, () -> (float) data.getTimesCompleted(quest)));
    }
}
