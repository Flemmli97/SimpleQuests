package io.github.flemmli97.simplequests;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class JsonCodecs {

    // The default BlockPos Codec writes to an array, this writes to a map of x, y, z
    public static Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
                    Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
                    Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
            ).apply(instance, BlockPos::new));

    public static <E> Codec<List<Either<E, Pair<E, String>>>> optionalDescriptiveList(Codec<E> codec, String error) {
        return nonEmptyList(Codec.either(codec, Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(codec, Codec.unit(e)))), error);
    }

    public static <E> Codec<List<Pair<E, String>>> descriptiveList(Codec<E> codec, String error) {
        return nonEmptyList(Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(codec, Codec.unit(e))), error);
    }

    public static <E> Codec<List<E>> nonEmptyList(Codec<E> codec, String error) {
        Function<List<E>, DataResult<List<E>>> function = list -> {
            if (list.isEmpty())
                return DataResult.error(() -> error);
            return DataResult.success(list);
        };
        return codec.listOf().flatXmap(function, function);
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
