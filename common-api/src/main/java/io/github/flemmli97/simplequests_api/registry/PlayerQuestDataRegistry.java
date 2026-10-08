package io.github.flemmli97.simplequests_api.registry;

import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerQuestDataRegistry {

    private static final Map<ResourceLocation, Function<ServerPlayer, PlayerQuestData>> REGISTRY = new HashMap<>();

    public static synchronized void registerFetcher(ResourceLocation id, Function<ServerPlayer, PlayerQuestData> fetcher) {
        if (REGISTRY.containsKey(id))
            throw new IllegalStateException("Fetcher for " + id + " already registered");
        REGISTRY.put(id, fetcher);
    }

    public static Optional<PlayerQuestData> get(ResourceLocation id, ServerPlayer player) {
        Function<ServerPlayer, PlayerQuestData> func = REGISTRY.get(id);
        if (func != null)
            return Optional.ofNullable(func.apply(player));
        return Optional.empty();
    }

    public static void applyAll(ServerPlayer player, Consumer<PlayerQuestData> app) {
        for (Function<ServerPlayer, PlayerQuestData> fetch : REGISTRY.values()) {
            app.accept(fetch.apply(player));
        }
    }
}
