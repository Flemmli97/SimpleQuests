package io.github.flemmli97.simplequests;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class JsonCodecs {

    private static final Gson GSON = Deserializers.createConditionSerializer().create();

    public static Codec<ItemPredicate> ITEM_PREDICATE_CODEC = jsonCodecBuilder(ItemPredicate::serializeToJson, ItemPredicate::fromJson, "ItemPredicate");
    public static Codec<EntityPredicate> ENTITY_PREDICATE_CODEC = jsonCodecBuilder(EntityPredicate::serializeToJson, EntityPredicate::fromJson, "EntityPredicate");
    public static Codec<BlockPredicate> BLOCK_PREDICATE_CODEC = jsonCodecBuilder(BlockPredicate::serializeToJson, BlockPredicate::fromJson, "BlockPredicate");
    public static Codec<LocationPredicate> LOCATION_PREDICATE_CODEC = jsonCodecBuilder(LocationPredicate::serializeToJson, LocationPredicate::fromJson, "LocationPredicate");

    public static Codec<NumberProvider> NUMER_PROVIDER_CODEC = JsonCodecs.jsonCodecBuilder(GSON::toJsonTree, e -> GSON.fromJson(e, NumberProvider.class), "NumberProvider");

    public static <E> Codec<List<Either<E, Pair<E, String>>>> optionalDescriptiveList(Codec<E> codec, String error) {
        return nonEmptyList(Codec.either(codec, Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(codec, Codec.STRING))), error);
    }

    public static <E> Codec<List<Pair<E, String>>> descriptiveList(Codec<E> codec, String error) {
        return nonEmptyList(Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(codec, Codec.STRING)), error);
    }

    public static <E> Codec<List<E>> nonEmptyList(Codec<E> codec, String error) {
        Function<List<E>, DataResult<List<E>>> function = list -> {
            if (list.isEmpty())
                return DataResult.error(error);
            return DataResult.success(list);
        };
        return codec.listOf().flatXmap(function, function);
    }

    public static <E> Codec<E> jsonCodecBuilder(Function<E, JsonElement> encode, Function<JsonElement, E> decode, String name) {
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
                try {
                    JsonElement e = encode.apply(input);
                    return DataResult.success(NullableJsonOps.INSTANCE.convertTo(ops, e));
                } catch (JsonParseException err) {
                    return DataResult.error("Couldn't encode value " + input + " error: " + err);
                }
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

    public static class NullableJsonOps extends JsonOps {

        public static final JsonOps INSTANCE = new NullableJsonOps(false);

        protected NullableJsonOps(boolean compressed) {
            super(compressed);
        }

        @Override
        public <U> U convertMap(final DynamicOps<U> ops, JsonElement e) {
            DataResult<Stream<Pair<JsonPrimitive, JsonElement>>> mapLike = DataResult.success(e.getAsJsonObject().entrySet().stream()
                    .filter(entry -> !(entry.getValue() instanceof JsonNull))
                    .map(entry -> Pair.of(new JsonPrimitive(entry.getKey()), entry.getValue())));
            return ops.createMap(mapLike.result().orElse(Stream.empty()).map(entry ->
                    Pair.of(this.convertTo(ops, entry.getFirst()),
                            this.convertTo(ops, entry.getSecond()))
            ));
        }
    }
}
