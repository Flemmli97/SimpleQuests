package io.github.flemmli97.simplequests_api.impls.tasks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.entry.QuestEntryKey;
import io.github.flemmli97.simplequests_api.quest.entry.QuestTask;
import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;
import io.github.flemmli97.simplequests_api.util.DescriptiveValue;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.util.PredicateTranslation;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemTask implements QuestTask<ItemTask.ItemTaskResolved> {

    public static final QuestEntryKey<ItemTask> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "item"));
    public static final Codec<ItemTask> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.optionalFieldOf("description").forGetter(d -> QuestUtils.optStr(d.description)),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate)),

                    JsonCodecs.nonEmptyList(DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC), "predicates cant' be empty").fieldOf("predicates").forGetter(d -> d.predicates),
                    JsonCodecs.NUMBER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("consume_items").forGetter(d -> d.consume)
            ).apply(instance, (desc, player, pred, amount, consume) -> new ItemTask(pred, amount, desc.orElse(""), consume, player.orElse(null))));

    private final String description;

    private final List<DescriptiveValue<ItemPredicate>> predicates;
    private final NumberProvider amount;
    private final boolean consume;
    @Nullable
    private final EntityPredicate playerPredicate;

    public ItemTask(List<DescriptiveValue<ItemPredicate>> predicates, NumberProvider amount, String description, boolean consume, @Nullable EntityPredicate playerPredicate) {
        this.description = description;
        this.predicates = predicates;
        this.amount = amount;
        this.consume = consume;
        this.playerPredicate = playerPredicate;
        if (this.description.isEmpty() && !this.simple())
            throw new IllegalStateException("Description is required");
    }

    private boolean simple() {
        return this.predicates.size() == 1 && this.amount instanceof ConstantValue;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        if (this.description.isEmpty() && this.simple()) {
            DescriptiveValue<ItemPredicate> predicate = this.predicates.get(0);
            return predicate.getTranslation(ItemTaskResolved.key(this.getId().toString(), predicate.value(), this.consume), this.amount.getInt(null));
        }
        return new TranslatableComponent(this.description);
    }

    @Override
    public QuestEntryKey<ItemTask> getId() {
        return ID;
    }

    @Override
    public ItemTaskResolved resolve(PlayerQuestData data, QuestProgress progress, QuestBase base) {
        LootContext ctx = SimpleQuestsAPI.createContext(data, base.id);
        DescriptiveValue<ItemPredicate> val = this.predicates.get(ctx.getRandom().nextInt(this.predicates.size()));
        return new ItemTaskResolved(val, QuestUtils.getAmount(this.amount, ctx, data, base.id), this.consume, this.playerPredicate);
    }

    public record ItemTaskResolved(DescriptiveValue<ItemPredicate> predicate, int amount, boolean consumeItems,
                                   @Nullable EntityPredicate playerPredicate) implements ResolvedQuestTask {

        public static final Codec<ItemTaskResolved> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(DescriptiveValue.withTranslation(JsonCodecs.ITEM_PREDICATE_CODEC).fieldOf("predicate").forGetter(d -> d.predicate),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.BOOL.fieldOf("consume_items").forGetter(d -> d.consumeItems),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("player_predicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (pred, amount, consume, player) -> new ItemTaskResolved(pred, amount, consume, player.orElse(null))));

        public static String key(String base, ItemPredicate pred, boolean consume) {
            List<MutableComponent> formattedItems = ((PredicateTranslation) pred).translation(true);
            if (formattedItems == null || formattedItems.isEmpty())
                base += ".empty";
            else if (formattedItems.size() == 1) {
                base += ".single" + (consume ? "" : ".keep");
            } else {
                base += ".multi" + (consume ? "" : ".keep");
            }
            return base;
        }

        @Override
        public boolean submit(ServerPlayer player) {
            if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
                return false;
            List<ItemStack> matching = new ArrayList<>();
            int i = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (this.predicate.value().matches(stack)) {
                    if (stack.isDamageableItem()) {
                        if (stack.getDamageValue() != 0) {
                            continue;
                        }
                    }
                    //Ignore "special" items
                    if (!this.isJustRenamedItem(stack)) {
                        continue;
                    }
                    matching.add(stack);
                    i += stack.getCount();
                }
            }
            if (i < this.amount)
                return false;
            if (this.consumeItems) {
                i = this.amount;
                for (ItemStack stack : matching) {
                    if (i > stack.getCount()) {
                        int count = stack.getCount();
                        stack.setCount(0);
                        i -= count;
                    } else {
                        stack.shrink(i);
                        break;
                    }
                }
            }
            return true;
        }

        private boolean isJustRenamedItem(ItemStack stack) {
            if (!stack.hasTag())
                return true;
            if (stack.getTag().getAllKeys()
                    .stream().allMatch(s -> s.equals("Damage") || s.equals("RepairCost") || s.equals("display"))) {
                CompoundTag tag = stack.getTag().getCompound("display");
                return tag.contains("Name") && tag.size() == 1;
            }
            return true;
        }

        @Override
        public QuestEntryKey<ItemTask> getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return this.predicate.getTranslation(key(this.getId().toString(), this.predicate.value(), this.consumeItems), this.amount);
        }
    }
}
