package io.github.flemmli97.simplequests_api.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class JsonCodecs {

    private static final Gson GSON = Deserializers.createConditionSerializer().create();

    public static Codec<ItemPredicate> ITEM_PREDICATE_CODEC = jsonCodecBuilder(nullToObj(ItemPredicate::serializeToJson), ItemPredicate::fromJson, "ItemPredicate");
    public static Codec<EntityPredicate> ENTITY_PREDICATE_CODEC = jsonCodecBuilder(nullToObj(EntityPredicate::serializeToJson), EntityPredicate::fromJson, "EntityPredicate");
    public static Codec<BlockPredicate> BLOCK_PREDICATE_CODEC = jsonCodecBuilder(nullToObj(BlockPredicate::serializeToJson), BlockPredicate::fromJson, "BlockPredicate");

    public static Codec<NumberProvider> NUMBER_PROVIDER_CODEC = JsonCodecs.jsonCodecBuilder(GSON::toJsonTree, e -> GSON.fromJson(e, NumberProvider.class), "NumberProvider");

    /**
     * Custom ItemStack Codec that tries to minimize data saved
     */
    public static final Codec<ItemStack> ITEM_STACK_CODEC = tryCodec(Registry.ITEM.byNameCodec()
                    .flatXmap(h -> DataResult.success(new ItemStack(h)),
                            s -> s.getTag() == null && s.getCount() == 1 ? DataResult.success(s.getItem()) : DataResult.error("Not default itemstack")),
            RecordCodecBuilder.create(inst -> inst.group(
                    Registry.ITEM.byNameCodec().fieldOf("id").forGetter(ItemStack::getItem),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("count").forGetter(stack -> stack.getCount() == 1 ? Optional.empty() : Optional.of(stack.getCount())),
                    CompoundTag.CODEC.optionalFieldOf("tag").forGetter((stack) -> Optional.ofNullable(stack.getTag()))
            ).apply(inst, (s, count, tag) -> {
                ItemStack stack = new ItemStack(s, count.orElse(1));
                tag.ifPresent(stack::setTag);
                return stack;
            })));

    public static <E> Codec<List<E>> nonEmptyList(Codec<E> codec, String error) {
        Function<List<E>, DataResult<List<E>>> function = list -> {
            if (list.isEmpty())
                return DataResult.error(error);
            return DataResult.success(list);
        };
        return codec.listOf().flatXmap(function, function);
    }

    private static <E> Function<E, JsonElement> nullToObj(Function<E, JsonElement> encode) {
        return v -> {
            JsonElement e = encode.apply(v);
            if (e.isJsonNull())
                return new JsonObject();
            return e;
        };
    }

    public static <E> Codec<List<E>> listOrInline(Codec<E> codec) {
        Codec<List<E>> listCodec = codec.listOf();
        return Codec.either(listCodec, codec).xmap(either -> either.map(list -> list, List::of),
                list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));
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
                JsonElement element = input == null ? JsonNull.INSTANCE : ops.convertTo(JsonOps.INSTANCE, input);
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
                    .filter(entry -> !entry.getValue().isJsonNull())
                    .map(entry -> Pair.of(new JsonPrimitive(entry.getKey()), entry.getValue())));
            return ops.createMap(mapLike.result().orElse(Stream.empty()).map(entry ->
                    Pair.of(this.convertTo(ops, entry.getFirst()),
                            this.convertTo(ops, entry.getSecond()))
            ));
        }
    }

    public static <F> Codec<F> tryCodec(Codec<F> first, Codec<F> second) {
        return new TryCodec<>(first, second);
    }

    private record TryCodec<F>(Codec<F> first, Codec<F> second) implements Codec<F> {

        @Override
        public <T> DataResult<Pair<F, T>> decode(final DynamicOps<T> ops, final T input) {
            final DataResult<Pair<F, T>> first = this.first.decode(ops, input);
            if (first.result().isPresent()) {
                return first;
            }
            final DataResult<Pair<F, T>> second = this.second.decode(ops, input);
            if (second.result().isPresent()) {
                return second;
            }
            return first.apply2((f, s) -> s, second);
        }

        @Override
        public <T> DataResult<T> encode(F input, final DynamicOps<T> ops, final T prefix) {
            DataResult<T> first = this.first.encode(input, ops, prefix);
            if (first.result().isPresent()) {
                return first;
            }
            DataResult<T> second = this.second.encode(input, ops, prefix);
            if (second.result().isPresent()) {
                return second;
            }
            return first.apply2((f, s) -> s, second);
        }
    }
}
