package io.github.flemmli97.simplequests_api.util;

import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PredicateTranslation {

    /**
     * During reload cache should be false. Tags are not loaded at that point yet
     */
    @Nullable
    List<MutableComponent> translation(boolean cache);

}
