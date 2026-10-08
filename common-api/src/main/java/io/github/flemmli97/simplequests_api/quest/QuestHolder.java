package io.github.flemmli97.simplequests_api.quest;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.registry.QuestDefinitionRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

/**
 * Leaning on vanillas {@link net.minecraft.core.Holder} this contains either an id lazy resolvable to a QuestDefinition
 * or of a QuestDefinition itself to allow nested definitions withing quests.
 */
public class QuestHolder {

    private final Either<ResourceLocation, QuestDefinition> quest;

    public static final Codec<QuestHolder> CODEC = Codec.either(ResourceLocation.CODEC,
            QuestDefinitionRegistry.CODEC).xmap(QuestHolder::new, h -> h.quest);

    public static final Codec<QuestHolder> SIMPLE_CODEC = Codec.either(ResourceLocation.CODEC,
            Codec.of(QuestDefinition.SIMPLIFIED_ENCODER, QuestDefinitionRegistry.CODEC)).xmap(QuestHolder::new, h -> h.quest);

    public QuestHolder(ResourceLocation quest) {
        this(Either.left(quest));
    }

    public QuestHolder(QuestDefinition quest) {
        this(Either.right(quest));
    }

    private QuestHolder(Either<ResourceLocation, QuestDefinition> quest) {
        this.quest = quest;
    }

    public QuestDefinition getDefinition() {
        return this.quest.map(id -> QuestsManager.instance().getQuest(id), Function.identity());
    }

    public ResourceLocation getId() {
        return this.quest.map(Function.identity(), QuestDefinition::id);
    }

    public Optional<ResourceLocation> registeredQuestId() {
        return this.quest.left();
    }

    void setId(ResourceLocation id) {
        this.quest.ifRight(q -> {
            if (q instanceof IdSetter setter) {
                setter.setId(id);
            }
            int i = 1;
            for (QuestHolder holder : q.getSubQuests()) {
                holder.setId(this.getId().withSuffix("_" + i++));
            }
        });
    }

    @Override
    public String toString() {
        return this.getId().toString();
    }
}
