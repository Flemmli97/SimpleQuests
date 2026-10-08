package io.github.flemmli97.simplequests_api.impls.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.QuestDefinition;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionImpl;
import io.github.flemmli97.simplequests_api.quest.QuestDefinitionRef;
import io.github.flemmli97.simplequests_api.quest.QuestHolder;
import io.github.flemmli97.simplequests_api.quest.ResolvedQuest;
import io.github.flemmli97.simplequests_api.util.TimeDuration;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A quest containing multiple quests that need to completed in order
 */
public class SequentialQuestDefinition extends QuestDefinitionImpl {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "sequential_quest");

    public static final MapCodec<SequentialQuestDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(true)),
            ExtraCodecs.nonEmptyList(QuestHolder.CODEC.listOf()).fieldOf("quests").forGetter(d -> d.quests),
            ResourceLocation.CODEC.fieldOf("loot_table").forGetter(d -> d.loot.location()),
            Codec.STRING.optionalFieldOf("command").forGetter(d -> Optional.of(d.command))
    ).apply(instance, (data, quests, loot, command) -> {
        Builder builder = data.applyTo(name -> new Builder(name, loot));
        quests.forEach(builder::addQuest);
        command.ifPresent(builder::withCommand);
        return builder.build();
    }));

    public static final MapCodec<SequentialQuestDefinition> OPT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BaseData.CODEC.forGetter(q -> q.get(false)),
            ExtraCodecs.nonEmptyList(QuestHolder.SIMPLE_CODEC.listOf()).fieldOf("quests").forGetter(d -> d.quests),
            ResourceLocation.CODEC.fieldOf("loot_table").forGetter(d -> d.loot.location()),
            Codec.STRING.optionalFieldOf("command").forGetter(d -> !d.command.isEmpty() ? Optional.of(d.command) : Optional.empty())
    ).apply(instance, (data, quests, loot, command) -> {
        Builder builder = data.applyTo(name -> new Builder(name, loot));
        quests.forEach(builder::addQuest);
        command.ifPresent(builder::withCommand);
        return builder.build();
    }));

    private final List<QuestHolder> quests;

    private final ResourceKey<LootTable> loot;
    private final String command;

    protected SequentialQuestDefinition(QuestCategory category, String name, List<String> description,
                                        List<ResourceLocation> requiredParentQuests, boolean redoParent,
                                        TimeDuration repeatDelay, int repeatDaily, int maxRepeat, boolean isDailyQuest,
                                        boolean needsUnlock, EntityPredicate unlockCondition, int sortingId, ItemStack icon, Visibility visibility,
                                        List<QuestHolder> quests, ResourceLocation loot, String command) {
        super(category, name, description, requiredParentQuests, redoParent, repeatDelay, repeatDaily, maxRepeat, isDailyQuest, needsUnlock, unlockCondition, sortingId, icon, visibility);
        this.quests = quests;
        this.loot = ResourceKey.create(Registries.LOOT_TABLE, loot);
        this.command = command;
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
        return this.quests;
    }

    @Override
    public boolean advanceable() {
        return true;
    }

    @Override
    public ResolvedQuest resolve(QuestProgress progress) {
        return new SequentialQuestDefinitionRef(this);
    }

    @Override
    public ResolvedQuest load(QuestProgress progress, Tag tag, HolderLookup.Provider provider) {
        return new SequentialQuestDefinitionRef(this);
    }

    public static class SequentialQuestDefinitionRef extends QuestDefinitionRef<SequentialQuestDefinition> {

        public SequentialQuestDefinitionRef(SequentialQuestDefinition definition) {
            super(definition);
        }

        @Override
        public void onComplete(ServerPlayer player) {
            ResolvedQuest.dropLoot(player, this.source().loot);
            ResolvedQuest.runCommand(player, this.source().command);
        }
    }

    public static class Builder extends BuilderBase<SequentialQuestDefinition, Builder> {

        protected final List<QuestHolder> quests = new ArrayList<>();
        protected final ResourceLocation loot;
        protected String command = "";

        public Builder(String name, ResourceLocation loot) {
            super(name);
            this.loot = loot;
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
            this.quests.add(quest);
            return this;
        }

        public Builder withCommand(String command) {
            this.command = command;
            return this;
        }

        @Override
        public SequentialQuestDefinition build() {
            return new SequentialQuestDefinition(this.category, this.name, this.description,
                    this.requiredParentQuests, this.redoParent,
                    this.repeatDelay, this.repeatDaily, this.maxRepeat,
                    this.isDailyQuest, this.needsUnlock, this.unlockCondition, this.sortingId, this.icon, this.visibility,
                    this.quests, this.loot, this.command);
        }
    }
}
