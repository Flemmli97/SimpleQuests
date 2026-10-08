package io.github.flemmli97.simplequests_api.quest;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.KeyCompressor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.RecordBuilder;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestDefinitionRegistry;
import io.github.flemmli97.simplequests_api.util.TimeDuration;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A QuestDefinition as the name suggests defines all necessary data for creating a quest.
 * Think of it as a template.
 * If you define a new QuestDefinition you must register it under {@link QuestDefinitionRegistry#register}
 */
public interface QuestDefinition extends Comparable<QuestDefinition> {

    Encoder<QuestDefinition> SIMPLIFIED_ENCODER = new MapEncoder<QuestDefinition>() {

        private static final String TYPE = "type";
        private static final String VALUE = "value";
        private final Map<DynamicOps<?>, KeyCompressor<?>> compressors = new Object2ObjectArrayMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <T> RecordBuilder<T> encode(QuestDefinition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            MapEncoder<QuestDefinition> elementEncoder = (MapEncoder<QuestDefinition>) input.simplifiedCodec();
            if (ops.compressMaps()) {
                return prefix
                        .add(TYPE, ResourceLocation.CODEC.encodeStart(ops, input.typeId()))
                        .add(VALUE, elementEncoder.encoder().encodeStart(ops, input));
            }

            return elementEncoder.encode(input, ops, prefix)
                    .add(TYPE, ResourceLocation.CODEC.encodeStart(ops, input.typeId()));
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(TYPE, VALUE).map(ops::createString);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> KeyCompressor<T> compressor(final DynamicOps<T> ops) {
            return (KeyCompressor<T>) this.compressors.computeIfAbsent(ops, k -> new KeyCompressor<>(ops, this.keys(ops)));
        }
    }.encoder();

    MapCodec<? extends QuestDefinition> codec();

    ResourceLocation typeId();

    /**
     * This is used for datagen to make smaller JSON files. E.g. values not differing from the default do not get written
     */
    default MapCodec<? extends QuestDefinition> simplifiedCodec() {
        return this.codec();
    }

    /**
     * @return The id for this quest. Note that internal ids are not registered to the {@link QuestsManager}.
     * The only way to obtain a quest via its id that way would be via its parent
     */
    default ResourceLocation id() {
        return QuestsManager.instance().getId(this);
    }

    QuestCategory category();

    MutableComponent name(ServerPlayer player);

    List<MutableComponent> description(ServerPlayer player);

    default List<MutableComponent> tasksDescription(ServerPlayer player, Style taskStyle) {
        return List.of();
    }

    default Map<String, QuestTask<?>> unresolvedTasks() {
        return Map.of();
    }

    List<ResourceLocation> requiredParentQuests();

    boolean redoParent();

    TimeDuration repeatDelay();

    int repeatDaily();

    int maxRepeat();

    boolean isDailyQuest();

    boolean needsUnlock();

    boolean isUnlocked(ServerPlayer player);

    int sortingId();

    ItemStack getIcon();

    Visibility visibility();

    default List<QuestHolder> getSubQuests() {
        return List.of();
    }

    default Optional<QuestDefinition> getSubQuest(ResourceLocation id) {
        return this.getSubQuests().stream().filter(h -> h.getId().equals(id))
                .map(QuestHolder::getDefinition).findFirst();
    }

    default void visit(Consumer<QuestDefinition> visitor) {
        this.visit(new HashSet<>(), visitor);
    }

    default void visit(Set<ResourceLocation> visited, Consumer<QuestDefinition> visitor) {
        visitor.accept(this);
        this.getSubQuests().forEach(h -> {
            if (visited.add(h.getId())) {
                h.getDefinition().visit(visited, visitor);
            }
        });
    }

    default boolean advanceable() {
        return false;
    }

    default boolean isValidAsRoot() {
        return this.advanceable() || this.getSubQuests().isEmpty();
    }

    @Nullable
    ResolvedQuest resolve(QuestProgress progress);

    @Nullable
    ResolvedQuest load(QuestProgress progress, Tag tag, HolderLookup.Provider provider);

    @Override
    default int compareTo(@NotNull QuestDefinition quest) {
        if (this.sortingId() == quest.sortingId()) {
            if (this.requiredParentQuests().isEmpty() && !quest.requiredParentQuests().isEmpty())
                return -1;
            if (!this.requiredParentQuests().isEmpty() && quest.requiredParentQuests().isEmpty())
                return 1;
            return this.id().compareTo(quest.id());
        }
        return Integer.compare(this.sortingId(), quest.sortingId());
    }

    enum Visibility {
        DEFAULT,
        ALWAYS,
        NEVER
    }
}
