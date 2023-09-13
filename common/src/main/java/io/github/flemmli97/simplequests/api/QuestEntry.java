package io.github.flemmli97.simplequests.api;

import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.function.Function;

public interface QuestEntry {

    boolean submit(ServerPlayer player);

    JsonObject serialize();

    ResourceLocation getId();

    MutableComponent translation(ServerPlayer player);

    default @Nullable
    MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
        return null;
    }

    default void onAccept(PlayerData data) {
    }

    default Function<PlayerData, Boolean> tickable() {
        return null;
    }

    interface Deserializer<T extends QuestEntry> {
        T deserialize(JsonObject obj);
    }
}
