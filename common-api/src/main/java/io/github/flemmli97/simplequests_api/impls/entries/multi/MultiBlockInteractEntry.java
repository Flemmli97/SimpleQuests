package io.github.flemmli97.simplequests_api.impls.entries.multi;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.entries.single.BlockInteractEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.quest.MultiQuestEntryBase;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class MultiBlockInteractEntry extends MultiQuestEntryBase {

    public static final QuestEntryKey<MultiBlockInteractEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "multi_block_interaction"));
    public static final Codec<MultiBlockInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                    Codec.BOOL.optionalFieldOf("allowDupes").forGetter(d -> d.allowDupes ? Optional.of(true) : Optional.empty()),
                    Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                    Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),
                    Codec.BOOL.fieldOf("use").forGetter(d -> d.use),

                    Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(JsonCodecs.ITEM_PREDICATE_CODEC, Codec.unit(e))).listOf()
                            .optionalFieldOf("itemPredicates").forGetter(d -> d.heldItems.isEmpty() ? Optional.empty() : Optional.of(d.heldItems)),
                    Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(JsonCodecs.BLOCK_PREDICATE_CODEC, Codec.unit(e))).listOf()
                            .optionalFieldOf("blockPredicates").forGetter(d -> d.blockPredicates.isEmpty() ? Optional.empty() : Optional.of(d.blockPredicates)),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
            ).apply(instance, (consume, allowDupes, desc, taskDescription, player, use, item, pred, amount) -> new MultiBlockInteractEntry(item.orElse(List.of()), pred.orElse(List.of()), amount, use, consume, allowDupes.orElse(false), desc, taskDescription, player.orElse(null))));

    private final List<Pair<ItemPredicate, String>> heldItems;
    private final List<Pair<BlockPredicate, String>> blockPredicates;
    private final NumberProvider amount;
    private final boolean use, consume, allowDupes;
    private final String taskDescription;
    private final EntityPredicate playerPredicate;

    public MultiBlockInteractEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<BlockPredicate, String>> blockPredicates, NumberProvider amount, boolean use, boolean consume, boolean allowDupes, String description, String taskDescription, EntityPredicate playerPredicate) {
        super(description);
        List<Pair<ItemPredicate, String>> held = heldItems.stream().filter(p -> p.getFirst() != ItemPredicate.ANY).toList();
        List<Pair<BlockPredicate, String>> block = blockPredicates.stream().filter(p -> p.getFirst() != BlockPredicate.ANY).toList();
        if (held.isEmpty() && block.isEmpty())
            throw new IllegalStateException("Either item or block has to be defined");
        this.heldItems = heldItems;
        this.blockPredicates = blockPredicates;
        this.amount = amount;
        this.use = use;
        this.consume = consume;
        this.allowDupes = allowDupes;
        this.taskDescription = taskDescription;
        this.playerPredicate = playerPredicate;
    }

    @Override
    public QuestEntryKey<?> getId() {
        return ID;
    }

    @Override
    public QuestEntry resolve(PlayerQuestData data, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
        Pair<BlockPredicate, String> entity = this.blockPredicates.isEmpty() ? Pair.of(BlockPredicate.ANY, "") : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
        while (val.getFirst() == ItemPredicate.ANY && entity.getFirst() == BlockPredicate.ANY) {
            val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
            entity = this.blockPredicates.isEmpty() ? Pair.of(BlockPredicate.ANY, "") : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
        }
        return new BlockInteractEntry(val.getFirst(), entity.getFirst(), QuestEntryUtil.getAmount(this.amount, ctx, data, base.id), this.use, this.consume, this.allowDupes, this.taskDescription, val.getSecond(), entity.getSecond(), this.playerPredicate);
    }
}
