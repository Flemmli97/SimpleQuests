package io.github.flemmli97.simplequests_api.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import io.github.flemmli97.simplequests_api.util.TimeDuration;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A basic implementation of a {@link QuestDefinition} containing all required fields and codecs
 */
public abstract class QuestDefinitionImpl implements QuestDefinition, IdSetter {

    private final QuestCategory category;
    private final String name;
    private final List<String> description;

    private final List<ResourceLocation> requiredParentQuests;
    private final boolean redoParent;

    private final TimeDuration repeatDelay;
    private final int repeatDaily;
    private final int maxRepeat;
    private final boolean isDailyQuest;

    private final boolean needsUnlock;
    private final EntityPredicate unlockCondition;

    private final int sortingId;

    private final ItemStack icon;

    private final Visibility visibility;

    private ResourceLocation id;

    protected QuestDefinitionImpl(QuestCategory category, String name, List<String> description, List<ResourceLocation> requiredParentQuests, boolean redoParent, TimeDuration repeatDelay, int repeatDaily, int maxRepeat, boolean isDailyQuest, boolean needsUnlock, EntityPredicate unlockCondition, int sortingId, ItemStack icon, Visibility visibility) {
        this.category = category == null ? QuestCategory.DEFAULT_CATEGORY : category;
        this.name = name;
        this.description = description;
        this.requiredParentQuests = requiredParentQuests;
        this.redoParent = redoParent;
        this.repeatDelay = repeatDelay;
        this.repeatDaily = repeatDaily;
        this.maxRepeat = maxRepeat;
        this.isDailyQuest = isDailyQuest;
        this.needsUnlock = needsUnlock;
        this.unlockCondition = unlockCondition;
        this.sortingId = sortingId;
        this.icon = icon;
        this.visibility = visibility;
    }

    public static void runCommand(ServerPlayer player, String command) {
        if (!command.isEmpty())
            player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public void setId(ResourceLocation id) {
        if (this.id != null) {
            throw new IllegalStateException("Id already set!");
        }
        this.id = id;
        ResourceLocation root = this.id();
        int i = 0;
        for (QuestHolder holder : this.getSubQuests()) {
            holder.setId(root.withSuffix("_" + i++));
        }
    }

    @Override
    public boolean isUnlocked(ServerPlayer player) {
        return this.unlockCondition == null || this.unlockCondition.matches(player, player);
    }

    @Override
    public Visibility visibility() {
        if (QuestsManager.instance().isSubQuest(this.id()))
            return Visibility.NEVER;
        if (this.visibility == Visibility.DEFAULT && !this.category().isVisible)
            return Visibility.NEVER;
        return this.visibility;
    }

    @Override
    public MutableComponent name(ServerPlayer player) {
        return Component.translatable(this.name);
    }

    @Override
    public final List<MutableComponent> description(ServerPlayer player) {
        return this.description.stream().map(Component::translatable).collect(Collectors.toList());
    }

    @Override
    public ItemStack getIcon() {
        return this.icon.copy();
    }

    @Override
    public QuestCategory category() {
        return this.category;
    }

    @Override
    public List<ResourceLocation> requiredParentQuests() {
        return this.requiredParentQuests;
    }

    @Override
    public TimeDuration repeatDelay() {
        return this.repeatDelay;
    }

    @Override
    public int repeatDaily() {
        return this.repeatDaily;
    }

    @Override
    public int maxRepeat() {
        return this.maxRepeat;
    }

    @Override
    public boolean redoParent() {
        return this.redoParent;
    }

    @Override
    public boolean needsUnlock() {
        return this.needsUnlock;
    }

    @Override
    public boolean isDailyQuest() {
        return this.isDailyQuest;
    }

    @Override
    public int sortingId() {
        return this.sortingId;
    }

    protected BaseData get(boolean full) {
        return new BaseData(this.category != QuestCategory.DEFAULT_CATEGORY ? Optional.of(this.category().id) : Optional.empty(),
                this.name,
                !this.description.isEmpty() || full ? Optional.of(this.description) : Optional.empty(),
                !this.requiredParentQuests().isEmpty() || full ? Optional.of(this.requiredParentQuests()) : Optional.empty(),
                this.redoParent() || full ? Optional.of(this.redoParent()) : Optional.empty(),
                this.repeatDelay().time() != 0 || full ? Optional.of(this.repeatDelay()) : Optional.empty(),
                this.repeatDaily() != 0 || full ? Optional.of(this.repeatDaily()) : Optional.empty(),
                this.maxRepeat() != 0 || full ? Optional.of(this.maxRepeat()) : Optional.empty(),
                this.isDailyQuest() || full ? Optional.of(this.isDailyQuest()) : Optional.empty(),
                this.needsUnlock() || full ? Optional.of(this.needsUnlock()) : Optional.empty(),
                Optional.ofNullable(this.unlockCondition),
                this.sortingId() != 0 || full ? Optional.of(this.sortingId()) : Optional.empty(),
                QuestUtils.defaultChecked(this.getIcon(), full ? null : Items.PAPER),
                this.visibility != Visibility.DEFAULT || full ? Optional.of(this.visibility) : Optional.empty()
        );
    }

    @Override
    public String toString() {
        return String.format("[Quest:%s]", this.id());
    }

    public record BaseData(Optional<ResourceLocation> category,
                           String name,
                           Optional<List<String>> description,
                           Optional<List<ResourceLocation>> parents,
                           Optional<Boolean> redoParent,
                           Optional<TimeDuration> repeatDelay,
                           Optional<Integer> repeatDaily,
                           Optional<Integer> maxRepeat,
                           Optional<Boolean> isDailyQuest,
                           Optional<Boolean> needsUnlock,
                           Optional<EntityPredicate> unlockCondition,
                           Optional<Integer> sortingId,
                           Optional<ItemStack> icon,
                           Optional<Visibility> visibility) {

        public static final MapCodec<BaseData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("category").forGetter(BaseData::category),
                Codec.STRING.fieldOf("name").forGetter(BaseData::name),
                JsonCodecs.listOrInline(Codec.STRING).optionalFieldOf("description").forGetter(BaseData::description),
                JsonCodecs.listOrInline(ResourceLocation.CODEC).optionalFieldOf("parent_id").forGetter(BaseData::parents),
                Codec.BOOL.optionalFieldOf("redo_parent").forGetter(BaseData::redoParent),
                TimeDuration.CODEC.optionalFieldOf("repeat_delay").forGetter(BaseData::repeatDelay),
                Codec.INT.optionalFieldOf("repeat_daily").forGetter(BaseData::repeatDaily),
                Codec.INT.optionalFieldOf("max_repeat").forGetter(BaseData::maxRepeat),
                Codec.BOOL.optionalFieldOf("daily_quest").forGetter(BaseData::isDailyQuest),
                Codec.BOOL.optionalFieldOf("need_unlock").forGetter(BaseData::needsUnlock),
                EntityPredicate.CODEC.optionalFieldOf("unlock_condition").forGetter(BaseData::unlockCondition),
                Codec.INT.optionalFieldOf("sorting_id").forGetter(BaseData::sortingId),
                JsonCodecs.ITEM_STACK_CODEC.optionalFieldOf("icon").forGetter(BaseData::icon),
                Codec.STRING.optionalFieldOf("visibility")
                        .xmap(opt -> opt.map(Visibility::valueOf), vis -> vis.map(Visibility::toString)).forGetter(BaseData::visibility)
        ).apply(instance, BaseData::new));

        public <Q extends QuestDefinitionImpl, T extends BuilderBase<Q, T>> T applyTo(Function<String, T> factory) {
            T builder = factory.apply(this.name());
            builder.withCategory(this.category.map(c -> QuestsManager.instance().getQuestCategory(c)).orElse(QuestCategory.DEFAULT_CATEGORY));
            this.description.orElse(List.of())
                    .forEach(builder::addDescription);
            this.parents.orElse(List.of())
                    .forEach(builder::addParent);
            if (this.redoParent.orElse(false))
                builder.setRedoParent();
            this.repeatDelay.ifPresent(builder::setRepeatDelay);
            this.repeatDaily.ifPresent(builder::setMaxDaily);
            this.maxRepeat.ifPresent(builder::setMaxRepeat);
            if (this.isDailyQuest.orElse(false))
                builder.setDailyQuest();
            if (this.needsUnlock.orElse(false))
                builder.needsUnlocking();
            this.unlockCondition.ifPresent(builder::withUnlockCondition);
            this.sortingId.ifPresent(builder::withSortingNum);
            builder.withIcon(this.icon.orElse(new ItemStack(Items.PAPER)));
            this.visibility.ifPresent(builder::setVisibility);
            return builder;
        }
    }

    public static abstract class BuilderBase<Q extends QuestDefinitionImpl, T extends BuilderBase<Q, T>> {

        protected QuestCategory category = QuestCategory.DEFAULT_CATEGORY;
        protected final String name;
        protected final List<String> description = new ArrayList<>();

        protected final List<ResourceLocation> requiredParentQuests = new ArrayList<>();
        protected boolean redoParent;

        protected TimeDuration repeatDelay = TimeDuration.NONE;
        protected int repeatDaily;
        protected int maxRepeat;
        protected boolean isDailyQuest;

        protected boolean needsUnlock;
        protected EntityPredicate unlockCondition = null;

        protected int sortingId;

        protected ItemStack icon = new ItemStack(Items.PAPER);

        protected Visibility visibility = Visibility.DEFAULT;

        protected BuilderBase(String name) {
            this.name = name;
        }

        public T addDescription(String desc) {
            this.description.add(desc);
            return this.asThis();
        }

        public T withCategory(QuestCategory category) {
            this.category = category;
            return this.asThis();
        }

        public T addParent(ResourceLocation parent) {
            this.requiredParentQuests.add(parent);
            return this.asThis();
        }

        public T setRedoParent() {
            this.redoParent = true;
            return this.asThis();
        }

        public T needsUnlocking() {
            this.needsUnlock = true;
            return this.asThis();
        }

        public T withIcon(ItemStack stack) {
            this.icon = stack;
            return this.asThis();
        }

        public T setRepeatDelay(int delay) {
            return this.setRepeatDelay(TimeDuration.of(delay));
        }

        public T setRepeatDelay(String delay) {
            return this.setRepeatDelay(TimeDuration.of(delay));
        }

        public T setRepeatDelay(TimeDuration delay) {
            this.repeatDelay = delay;
            return this.asThis();
        }

        public T setMaxDaily(int max) {
            this.repeatDaily = max;
            return this.asThis();
        }

        public T setMaxRepeat(int max) {
            this.maxRepeat = max;
            return this.asThis();
        }

        public T withSortingNum(int num) {
            this.sortingId = num;
            return this.asThis();
        }

        public T setDailyQuest() {
            this.isDailyQuest = true;
            return this.asThis();
        }

        public T withUnlockCondition(EntityPredicate unlockCondition) {
            this.unlockCondition = unlockCondition;
            return this.asThis();
        }

        public T setVisibility(Visibility visibility) {
            this.visibility = visibility;
            return this.asThis();
        }

        protected abstract T asThis();

        public abstract Q build();
    }
}
