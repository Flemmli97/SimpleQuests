package io.github.flemmli97.simplequests;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;

import java.util.function.Function;
import java.util.stream.Stream;

public class JsonCodecs {

    public static Codec<ItemPredicate> ITEM_PREDICATE_CODEC = jsonCodecBuilder(ItemPredicate::serializeToJson, ItemPredicate::fromJson, "ItemPredicate");
    public static Codec<EntityPredicate> ENTITY_PREDICATE_CODEC = jsonCodecBuilder(EntityPredicate::serializeToJson, EntityPredicate::fromJson, "EntityPredicate");
    public static Codec<BlockPredicate> BLOCK_PREDICATE_CODEC = jsonCodecBuilder(BlockPredicate::serializeToJson, BlockPredicate::fromJson, "BlockPredicate");
    public static Codec<LocationPredicate> LOCATION_PREDICATE_CODEC = jsonCodecBuilder(LocationPredicate::serializeToJson, LocationPredicate::fromJson, "LocationPredicate");

    public static <E> Codec<E> jsonCodecBuilder(Function<E, JsonElement> encode, Function<JsonElement, E> decode, String name) {
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                JsonElement e = encode.apply(input);
                if (e instanceof JsonObject) {
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
