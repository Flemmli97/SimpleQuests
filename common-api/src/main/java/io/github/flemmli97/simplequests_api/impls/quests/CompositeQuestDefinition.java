package io.github.flemmli97.simplequests_api.impls.quests;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionImpl;
import io.github.flemmli97.simplequests_api.quest.QuestHolder;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import io.github.flemmli97.simplequests_api.util.TimeDuration;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A quest containing multiple quests which can be selected
 */
public class CompositeQuestDefinition extends QuestDefinitionImpl {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "composite_quest");

    public static final MapCodec<CompositeQuestDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(true)),
            ExtraCodecs.nonEmptyList(QuestHolder.CODEC.listOf()).fieldOf("quests").forGetter(q -> q.compositeQuests)
    ).apply(instance, (data, quests) -> {
        Builder builder = data.applyTo(Builder::new);
        quests.forEach(builder::addQuest);
        return builder.build();
    }));

    public static final MapCodec<CompositeQuestDefinition> OPT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(false)),
            ExtraCodecs.nonEmptyList(QuestHolder.SIMPLE_CODEC.listOf()).fieldOf("quests").forGetter(q -> q.compositeQuests)
    ).apply(instance, (data, quests) -> {
        Builder builder = data.applyTo(Builder::new);
        quests.forEach(builder::addQuest);
        return builder.build();
    }));

    private final List<QuestHolder> compositeQuests;

    protected CompositeQuestDefinition(QuestCategory category, String name, List<String> description,
                                       List<ResourceLocation> requiredParentQuests, boolean redoParent,
                                       TimeDuration repeatDelay, int repeatDaily, int maxRepeat, boolean isDailyQuest,
                                       boolean needsUnlock, EntityPredicate unlockCondition, int sortingId, ItemStack icon, Visibility visibility,
                                       List<QuestHolder> compositeQuests) {
        super(category, name, description, requiredParentQuests, redoParent, repeatDelay, repeatDaily, maxRepeat, isDailyQuest, needsUnlock, unlockCondition, sortingId, icon, visibility);
        this.compositeQuests = compositeQuests;
    }

    @Override
    public MapCodec<? extends QuestDefinition> codec() {
        return CODEC;
    }

    @Override
    public ResourceLocation typeId() {
        return ID;
    }

    @Override
    public MapCodec<? extends QuestDefinition> simplifiedCodec() {
        return OPT_CODEC;
    }

    @Override
    public List<QuestHolder> getSubQuests() {
        return this.compositeQuests;
    }

    @Override
    public ResolvedQuest resolve(QuestProgress progress) {
        return null;
    }

    @Override
    public ResolvedQuest load(QuestProgress progress, Tag tag, HolderLookup.Provider provider) {
        return null;
    }

    public static class Builder extends BuilderBase<CompositeQuestDefinition, Builder> {

        protected final List<QuestHolder> compositeQuests = new ArrayList<>();

        public Builder(String name) {
            super(name);
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        public Builder addQuest(ResourceLocation quest) {
            return this.addQuest(new QuestHolder(quest));
        }

        public Builder addQuest(QuestDefinition quest) {
            return this.addQuest(new QuestHolder(quest));
        }

        public Builder addQuest(QuestHolder quest) {
            this.compositeQuests.add(quest);
            return this;
        }

        @Override
        public CompositeQuestDefinition build() {
            return new CompositeQuestDefinition(this.category, this.name, this.description,
                    this.requiredParentQuests, this.redoParent,
                    this.repeatDelay, this.repeatDaily, this.maxRepeat,
                    this.isDailyQuest, this.needsUnlock, this.unlockCondition, this.sortingId, this.icon, this.visibility,
                    this.compositeQuests);
        }
    }
}
