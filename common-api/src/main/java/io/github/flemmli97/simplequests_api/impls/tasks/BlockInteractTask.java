package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.util.DescriptiveValue;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Quest entry to check when a player interacts with a block
 */
public class BlockInteractTask implements QuestTask<BlockInteractTask.BlockInteractTaskResolved> {

    public static final QuestEntryKey<BlockInteractTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "block_interact"));
    public static final Codec<BlockInteractTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.BOOL.fieldOf("use").forGetter(d -> d.use),
                    Codec.BOOL.optionalFieldOf("allow_dupes").forGetter(d -> d.allowDupes ? Optional.of(true) : Optional.empty()),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),

                    DescriptiveValue.withTranslation(JsonCodecs.BLOCK_PREDICATE_CODEC).listOf()
                            .fieldOf("block_predicates").forGetter(d -> d.blockPredicates),
                    DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).listOf()
                            .optionalFieldOf("item_predicates").forGetter(d -> d.itemPredicates.isEmpty() ? Optional.empty() : Optional.of(d.itemPredicates)),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume)
            ).apply(instance, (use, allowDupes, desc, player, blocks, items, amount, consume) ->
                    new BlockInteractTask(blocks, items.orElse(List.of()), amount, use, consume, allowDupes.orElse(false),
                            desc, player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<BlockPredicate>> blockPredicates;
    private final List<DescriptiveValue<ItemPredicate>> itemPredicates;
    private final NumberProvider amount;
    private final boolean use, consume, allowDupes;
    private final EntityPredicate playerPredicate;

    public BlockInteractTask(List<DescriptiveValue<BlockPredicate>> blockPredicates, List<DescriptiveValue<ItemPredicate>> itemPredicates,
                             NumberProvider amount, boolean use, boolean consume, boolean allowDupes, String description,
                             EntityPredicate playerPredicate) {
        this.description = description;
        this.blockPredicates = blockPredicates;
        this.itemPredicates = itemPredicates;
        this.amount = amount;
        this.use = use;
        this.consume = consume;
        this.allowDupes = allowDupes;
        this.playerPredicate = playerPredicate;
        if (this.description.isEmpty() && !this.simple())
            throw new IllegalStateException("Description is required");
    }

    private boolean simple() {
        if (this.blockPredicates.size() == 1)
            return this.itemPredicates.size() <= 1 && this.amount instanceof ConstantValue;
        if (this.itemPredicates.size() == 1)
            return this.blockPredicates.size() <= 1 && this.amount instanceof ConstantValue;
        return false;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.simple()) {
            if (this.blockPredicates.size() == 1) {
                DescriptiveValue<BlockPredicate> pred = this.blockPredicates.get(0);
                if (this.itemPredicates.size() == 1)
                    return pred.getTranslation(this.getId().toString() + ".block_and_item" + (this.use ? ".use" : ""), this.itemPredicates.get(0).getTranslation(), this.amount.getInt(null));
                return pred.getTranslation(this.getId().toString() + (this.use ? ".use" : ""), this.amount.getInt(null));
            }
            return this.itemPredicates.get(0).getTranslation(this.getId().toString() + ".item" + (this.use ? ".use" : ""), this.amount.getInt(null));
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<BlockInteractTask> getId() {
        return ID;
    }

    @Override
    public BlockInteractTaskResolved resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<ItemPredicate> val = this.itemPredicates.isEmpty() ? null : this.itemPredicates.get(ctx.getRandom().nextInt(this.itemPredicates.size()));
        DescriptiveValue<BlockPredicate> block = this.blockPredicates.isEmpty() ? null : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
        while (val == null && block == null) {
            val = this.itemPredicates.isEmpty() ? null : this.itemPredicates.get(ctx.getRandom().nextInt(this.itemPredicates.size()));
            block = this.blockPredicates.isEmpty() ? null : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
        }
        return new BlockInteractTaskResolved(val, block, QuestUtils.getAmount(this.amount, ctx, data, base.id), this.use, this.consume, this.allowDupes, this.playerPredicate);
    }

    public record BlockInteractTaskResolved(DescriptiveValue<ItemPredicate> heldItem,
                                            DescriptiveValue<BlockPredicate> blockPredicate, int amount,
                                            boolean use, boolean consumeItem, boolean allowDupes,
                                            EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<BlockInteractTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(DescriptiveValue.withTranslation(JsonCodecs.BLOCK_PREDICATE_CODEC).optionalFieldOf("block").forGetter(d -> Optional.ofNullable(d.blockPredicate)),
                        Codec.BOOL.fieldOf("consume_item").forGetter(d -> d.consumeItem),

                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),
                        DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).optionalFieldOf("item").forGetter(d -> Optional.ofNullable(d.heldItem)),

                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.BOOL.fieldOf("use").forGetter(d -> d.use),
                        Codec.BOOL.optionalFieldOf("allow_dupes").forGetter(d -> d.allowDupes ? Optional.of(true) : Optional.empty())
                ).apply(instance, (block, consume, player, item, amount, use, allowDupes) ->
                        new BlockInteractTaskResolved(item.orElse(null), block.orElse(null), amount, use,
                                consume, allowDupes.orElse(false), player.orElse(null))));

        public BlockInteractTaskResolved {
            if (heldItem == null && blockPredicate == null)
                throw new IllegalStateException("Either block or item predicate needs to be defined");
        }

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public QuestEntryKey<BlockInteractTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            if (this.blockPredicate != null) {
                if (this.heldItem != null)
                    return this.blockPredicate.getTranslation(this.getId().toString() + ".block_and_item" + (this.use ? ".use" : ""), this.heldItem.getTranslation(), this.amount);
                return this.blockPredicate.getTranslation(this.getId().toString() + (this.use ? ".use" : ""), this.amount);
            }
            return this.heldItem.getTranslation(this.getId().toString() + ".item" + (this.use ? ".use" : ""), this.amount);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.progressComponent(player, BlockTracker.KEY, id);
        }

        public boolean check(ServerPlayer player, BlockPos pos, boolean use) {
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            if (use != this.use)
                return false;
            boolean b = (this.heldItem == null || this.heldItem.value().matches(player.getMainHandItem())) &&
                    (this.blockPredicate == null || this.blockPredicate.value().matches(player.getLevel(), pos));
            if (b && this.consumeItem && !player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }
            return b;
        }
    }
}
