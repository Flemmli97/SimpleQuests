package io.github.flemmli97.simplequests_api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class DescriptiveValue<T> {

    private final T value;
    private final String description;
    private final Function<T, List<MutableComponent>> translation;

    public DescriptiveValue(T value, String description, @Nullable Function<T, List<MutableComponent>> translation) {
        this.value = value;
        this.description = description;
        this.translation = translation == null ? t -> null : translation;
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
        return Codec.mapPair(codec.fieldOf("value"), Codec.STRING.optionalFieldOf("description")).codec()
                .xmap(v -> new DescriptiveValue<>(v.getFirst(), v.getSecond().orElse(""), null),
                        v -> Pair.of(v.value(), v.description.isEmpty() ? Optional.empty() : Optional.of(v.description)));
    }

    public static <T> Codec<DescriptiveValue<T>> codecDesc(Codec<T> codec) {
        return Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(codec, Codec.unit(e)))
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
        Function<T, List<MutableComponent>> translation = i -> {
            if (i instanceof PredicateTranslation t)
                return t.translation(true);
            return null;
        };
        // Selfnote: Do not use either codec!
        // If a structs that has all optional fields (e.g. EntityPredicate) fails it will create an empty one and
        // throw a missing description error (since all field there are optional)
        // The real error gets swallowed
        return Codec.mapPair(codec.fieldOf("value"), Codec.STRING.optionalFieldOf("description")).codec()
                .flatXmap(v -> {
                            String desc = v.getSecond().orElse("");
                            if (desc.isEmpty() && v.getFirst() instanceof PredicateTranslation t && t.translation(false) == null)
                                return DataResult.error(() -> "Description required. Element too complicated for default description");
                            return DataResult.success(new DescriptiveValue<>(v.getFirst(), desc, translation));
                        },
                        v -> {
                            String desc = v.description;
                            if (v.description.isEmpty() && v instanceof PredicateTranslation t && t.translation(false) == null)
                                return DataResult.error(() -> "Description required. Element too complicated for default description");
                            return DataResult.success(Pair.of(v.value(), desc.isEmpty() ? Optional.empty() : Optional.of(desc)));
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
        List<MutableComponent> translations = this.translation.apply(this.value);
        if (translations != null && !translations.isEmpty()) {
            if (translations.size() == 1)
                translation = translations.get(0);
            else {
                MutableComponent items = null;
                for (MutableComponent c : translations) {
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
