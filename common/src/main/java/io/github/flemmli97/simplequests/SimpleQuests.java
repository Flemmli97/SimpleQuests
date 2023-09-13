package io.github.flemmli97.simplequests;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;
import java.util.stream.Stream;

public class SimpleQuests {

    public static final String MODID = "simplequests";

    public static final Logger logger = LogManager.getLogger("simplequests");

    private static LoaderHandler handler;

    public static boolean ftbRanks;
    public static boolean permissionAPI;

    public static void updateLoaderImpl(LoaderHandler impl) {
        handler = impl;
    }

    public static LoaderHandler getHandler() {
        return handler;
    }

    public static void onInteractEntity(ServerPlayer player, Entity entity, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND)
            PlayerData.get(player).onInteractWith(entity);
    }

    public static <E> Codec<E> jsonCodecBuilder(Function<E, JsonElement> encode, Function<JsonElement, E> decode, String name) {
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                JsonElement e = encode.apply(input);
                if (e != null) {
                    DataResult<Stream<Pair<JsonPrimitive, JsonElement>>> mapLike = DataResult.success(e.getAsJsonObject().entrySet().stream()
                            .filter(entry -> !(entry.getValue() instanceof JsonNull))
                            .map(entry -> Pair.of(new JsonPrimitive(entry.getKey()), entry.getValue())));
                    return ops.mergeToPrimitive(prefix, ops.createMap(mapLike.result().orElse(Stream.empty()).map(entry ->
                            Pair.of(JsonOps.INSTANCE.convertTo(ops, entry.getFirst()), JsonOps.INSTANCE.convertTo(ops, entry.getSecond()))
                    )));
                }
                return DataResult.error("Couldn't encode value " + input);
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
                JsonElement element = ops.convertTo(JsonOps.INSTANCE, input);
                try {
                    E result = decode.apply(element);
                    return DataResult.success(Pair.of(result, input));
                } catch (JsonParseException err) {
                    return DataResult.error("Couldn't decode value " + err);
                }
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
