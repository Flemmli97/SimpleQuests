package io.github.flemmli97.simplequests_api.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DescriptiveValue<T> {

    private final T value;
    private final String description;
    private final List<MutableComponent> translations;

    private DescriptiveValue(T value) {
        this(value, "", null);
    }

    public DescriptiveValue(T value, String description) {
        this(value, description, null);
    }

    public DescriptiveValue(T value, String description, @Nullable Function<T, List<MutableComponent>> translation) {
        this.value = value;
        this.description = description;
        this.translations = translation == null ? null : translation.apply(this.value);
    }

    public static <T> DescriptiveValue<T> of(T val) {
        return of(val, "");
    }

    public static <T> DescriptiveValue<T> of(T val, String description) {
        return new DescriptiveValue<>(val, description, null);
    }

    public static <T> ListBuilder<T> list(T val) {
        return new ListBuilder<>(val, "");
    }

    public static <T> ListBuilder<T> list(T val, String description) {
        return new ListBuilder<>(val, description);
    }

    public static <T> Codec<DescriptiveValue<T>> codec(Codec<T> codec) {
        return Codec.either(Codec.mapPair(codec.fieldOf("value"), Codec.STRING.fieldOf("description")).codec(), codec)
                .xmap(e -> e.map(v -> new DescriptiveValue<>(v.getFirst(), v.getSecond(), null),
                                v -> new DescriptiveValue<>(v, "", null)),
                        v -> {
                            String desc = v.description;
                            if (desc.isEmpty())
                                return Either.right(v.value());
                            return Either.left(Pair.of(v.value(), desc));
                        });
    }

    public static <T> Codec<DescriptiveValue<T>> codecDesc(Codec<T> codec) {
        return Codec.mapPair(codec.fieldOf("value"), Codec.STRING.fieldOf("description")).codec()
                .flatXmap(e -> {
                            if (e.getSecond().isEmpty())
                                return DataResult.error(() -> "Description required");
                            return DataResult.success(new DescriptiveValue<>(e.getFirst(), e.getSecond(), null));
                        },
                        v -> {
                            if (v.description.isEmpty())
                                return DataResult.error(() -> "Description required");
                            return DataResult.success(Pair.of(v.value(), v.description));
                        });
    }

    public static <T> Codec<DescriptiveValue<T>> withTranslation(Codec<T> codec) {
        Function<T, List<MutableComponent>> translation = PredicateTranslation::translation;
        return Codec.either(Codec.mapPair(codec.fieldOf("value"), Codec.STRING.fieldOf("description")).codec(), codec)
                .flatXmap(e -> e.map(v -> DataResult.success(new DescriptiveValue<>(v.getFirst(), v.getSecond(), translation)),
                                v -> {
                                    if (translation.apply(v) == null)
                                        return DataResult.error(() -> "Description required. Element too complicated for default description");
                                    return DataResult.success(new DescriptiveValue<>(v, "", translation));
                                }),
                        v -> {
                            String desc = v.description;
                            if (translation.apply(v.value()) == null && v.description.isEmpty())
                                return DataResult.error(() -> "Description required. Element too complicated for default description");
                            if (desc.isEmpty())
                                return DataResult.success(Either.right(v.value()));
                            return DataResult.success(Either.left(Pair.of(v.value(), desc)));
                        });
    }

    public T value() {
        return this.value;
    }

    public MutableComponent getTranslation(Object... args) {
        return this.getTranslation("", args);
    }

    public MutableComponent getTranslation(String alt, Object... args) {
        String key = !this.description.isEmpty() ? this.description : alt;
        MutableComponent translation = null;
        if (this.translations != null && !this.translations.isEmpty()) {
            if (this.translations.size() == 1)
                translation = this.translations.get(0);
            else {
                MutableComponent items = null;
                for (MutableComponent c : this.translations) {
                    if (items == null)
                        items = Component.literal("[").append(c);
                    else
                        items.append(", ").append(c);
                }
                items.append("]");
                translation = items.withStyle(ChatFormatting.AQUA);
            }
        }
        if (translation == null) {
            return Component.translatable(key, args);
        }
        if (key.isEmpty() && args.length == 0)
            return translation;
        List<Object> argList = new ArrayList<>();
        argList.add(translation);
        argList.addAll(List.of(args));
        return Component.translatable(key, argList.toArray());
    }

    public static class ListBuilder<T> {
        private final List<DescriptiveValue<T>> list = new ArrayList<>();

        public ListBuilder(T val, String description) {
            this.list.add(DescriptiveValue.of(val, description));
        }

        public ListBuilder<T> add(T val) {
            this.list.add(DescriptiveValue.of(val, ""));
            return this;
        }

        public ListBuilder<T> add(T val, String description) {
            this.list.add(DescriptiveValue.of(val, description));
            return this;
        }

        public List<DescriptiveValue<T>> build() {
            return this.list;
        }
    }
}
