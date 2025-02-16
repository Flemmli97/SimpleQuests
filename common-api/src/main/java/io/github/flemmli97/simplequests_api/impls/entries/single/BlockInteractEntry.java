package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.progression.BlockTracker;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Quest entry to check when a player interacts with a block
 *
 * @param use If the player should use (right click) or break the block
 */
public record BlockInteractEntry(ItemPredicate heldItem, BlockPredicate blockPredicate, int amount, boolean use,
                                 boolean consumeItem, boolean allowDupes, String description,
                                 String heldDescription,
                                 String blockDescription,
                                 EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<BlockInteractEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "block_interact"));
    public static final Codec<BlockInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.optionalFieldOf("held_description").forGetter(d -> d.heldDescription.isEmpty() ? Optional.empty() : Optional.of(d.heldDescription)),
                    Codec.STRING.optionalFieldOf("block_description").forGetter(d -> d.blockDescription.isEmpty() ? Optional.empty() : Optional.of(d.blockDescription)),

                    JsonCodecs.BLOCK_PREDICATE_CODEC.optionalFieldOf("block").forGetter(d -> d.blockPredicate == BlockPredicate.ANY ? Optional.empty() : Optional.ofNullable(d.blockPredicate)),
                    Codec.BOOL.fieldOf("consume_item").forGetter(d -> d.consumeItem),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),

                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),
                    JsonCodecs.ITEM_PREDICATE_CODEC.optionalFieldOf("item").forGetter(d -> d.heldItem == ItemPredicate.ANY ? Optional.empty() : Optional.ofNullable(d.heldItem)),

                    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("use").forGetter(d -> d.use),
                    Codec.BOOL.optionalFieldOf("allow_dupes").forGetter(d -> d.allowDupes ? Optional.of(true) : Optional.empty())
            ).apply(instance, (heldDesc, blockDescription, block, consume, desc, player, item, amount, use, allowDupes) -> {
                ItemPredicate itemPredicate = item.orElse(ItemPredicate.ANY);
                BlockPredicate blockPredicate = block.orElse(BlockPredicate.ANY);
                if (itemPredicate == ItemPredicate.ANY && blockPredicate == BlockPredicate.ANY)
                    throw new IllegalStateException("Either item or block has to be defined");
                return new BlockInteractEntry(item.orElse(null), block.orElse(null), amount, use, consume, allowDupes.orElse(false), desc, heldDesc.orElse(""), blockDescription.orElse(""), player.orElse(null));
            }));

    public BlockInteractEntry(ItemPredicate heldItem, BlockPredicate blockPredicate, int amount, boolean use,
                              boolean consumeItem, String description) {
        this(heldItem, blockPredicate, amount, use, false, consumeItem, description, "", "", null);
    }

    @Override
    public boolean submit(ServerPlayer player) {
        return false;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        return new TranslatableComponent(this.description, new TranslatableComponent(this.heldDescription), new TranslatableComponent(this.blockDescription), this.amount);
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
        boolean b = (this.heldItem == null || this.heldItem.matches(player.getMainHandItem())) &&
                (this.blockPredicate == null || this.blockPredicate.matches(player.getLevel(), pos));
        if (b && this.consumeItem && !player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }
        return b;
    }
}
