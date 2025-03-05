package io.github.flemmli97.simplequests_api.impls.quests;

import com.mojang.serialization.Codec;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A quest containing multiple quests which can be selected
 */
public class CompositeQuest extends QuestBase {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuestsAPI.MODID, "composite_quest");

    public static final Function<QuestBaseRegistry.CodecContext, Codec<CompositeQuest>> CODEC = ctx ->
            QuestBase.buildCodec(ExtraCodecs.nonEmptyList(ResourceLocation.CODEC.listOf()).fieldOf("quests")
                    .forGetter(q -> q.compositeQuests), ctx, (id, task, quests) -> {
                Builder builder = new Builder(id, task);
                quests.forEach(builder::addQuest);
                return builder;
            });

    private final List<ResourceLocation> compositeQuests;

    protected CompositeQuest(ResourceLocation id, QuestCategory category, String questTaskString, List<String> questTaskDesc, List<ResourceLocation> parents, boolean redoParent, boolean needsUnlock,
                             ItemStack icon, int repeatDelay, int repeatDaily, int maxRepeat,
                             int sortingId, boolean isDailyQuest, EntityPredicate unlockCondition,
                             List<ResourceLocation> compositeQuests, Visibility visibility) {
        super(id, category, questTaskString, questTaskDesc, parents, redoParent, needsUnlock, icon, repeatDelay, repeatDaily, maxRepeat, sortingId, isDailyQuest, unlockCondition, visibility);
        this.compositeQuests = compositeQuests;
    }

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public List<ResourceLocation> getSubQuests() {
        return this.compositeQuests;
    }

    @Override
    @Nullable
    public QuestBase resolveToQuest(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return null;
        return QuestsManager.instance().getQuest(this.compositeQuests.get(idx));
    }

    @Override
    public ResourceLocation getLoot() {
        return null;
    }

    @Override
    public String submissionTrigger(ServerPlayer player, int idx) {
        if (idx < 0 || idx >= this.compositeQuests.size())
            return super.submissionTrigger(player, idx);
        QuestBase base = QuestsManager.instance().getQuest(this.compositeQuests.get(idx));
        return base.submissionTrigger(player, idx);
    }

    @Override
    public Map<String, ResolvedQuestTask> resolveTasks(PlayerQuestData data, QuestProgress progress, int idx) {
        QuestBase base = this.resolveToQuest(data.getPlayer(), idx);
        return base == null ? Map.of() : base.resolveTasks(data, progress, 0);
    }

    public static class Builder extends BuilderBase<CompositeQuest, Builder> {

        protected final List<ResourceLocation> compositeQuests = new ArrayList<>();

        public Builder(ResourceLocation id, String task) {
            super(id, task);
        }

        @Override
        protected Builder asThis() {
            return this;
        }

        public Builder addQuest(ResourceLocation quest) {
            this.compositeQuests.add(quest);
            return this;
        }

        @Override
        public CompositeQuest build() {
            CompositeQuest quest = new CompositeQuest(this.id, this.category, this.name, this.description, this.neededParentQuests, this.redoParent, this.needsUnlock,
                    this.icon, this.repeatDelay, this.repeatDaily, this.maxRepeat, this.sortingId, this.isDailyQuest,
                    this.unlockCondition, this.compositeQuests, this.visibility);
            quest.setDelayString(this.repeatDelayString);
            return quest;
        }
    }
}
