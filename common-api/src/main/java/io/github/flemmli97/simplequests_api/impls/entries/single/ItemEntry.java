package io.github.flemmli97.simplequests_api.impls.entries.single;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.util.JsonCodecs;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.mixin.ItemPredicateAccessor;
import io.github.flemmli97.simplequests_api.quest.QuestEntry;
import io.github.flemmli97.simplequests_api.quest.QuestEntryKey;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record ItemEntry(ItemPredicate predicate, int amount,
                        String description, boolean consumeItems,
                        EntityPredicate playerPredicate) implements QuestEntry {

    public static final QuestEntryKey<ItemEntry> ID = new QuestEntryKey<>(new ResourceLocation(SimpleQuestsAPI.MODID, "item"));
    public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(JsonCodecs.ITEM_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.predicate),
                    Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                    ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                    Codec.BOOL.fieldOf("consumeItems").forGetter(d -> d.consumeItems),
                    JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
            ).apply(instance, (pred, desc, amount, consume, player) -> new ItemEntry(pred, amount, desc.orElse(""), consume, player.orElse(null))));

    @Override
    public boolean submit(ServerPlayer player) {
        if (this.playerPredicate != null && !this.playerPredicate.matches(player, player))
            return false;
        List<ItemStack> matching = new ArrayList<>();
        int i = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (this.predicate.matches(stack)) {
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
    public QuestEntryKey<ItemEntry> getId() {
        return ID;
    }

    @Override
    public MutableComponent translation(ServerPlayer player) {
        Function<String, String> key = s -> !this.description.isEmpty() ? this.description : this.getId().toString() + s;
        List<MutableComponent> formattedItems = itemComponents(this.predicate);
        if (formattedItems.isEmpty())
            return new TranslatableComponent(key.apply(".empty"));
        if (formattedItems.size() == 1) {
            return new TranslatableComponent(key.apply(".single" + (this.consumeItems ? "" : ".keep")), formattedItems.get(0).withStyle(ChatFormatting.AQUA), this.amount);
        }
        MutableComponent items = null;
        for (MutableComponent c : formattedItems) {
            if (items == null)
                items = new TextComponent("[").append(c);
            else
                items.append(new TextComponent(", ")).append(c);
        }
        items.append("]");
        return new TranslatableComponent(key.apply(".multi" + (this.consumeItems ? "" : ".keep")), items.withStyle(ChatFormatting.AQUA), this.amount);
    }

    public static List<MutableComponent> itemComponents(ItemPredicate predicate) {
        ItemPredicateAccessor acc = (ItemPredicateAccessor) predicate;
        List<MutableComponent> formattedItems = new ArrayList<>();
        if (acc.getItems() != null)
            acc.getItems().forEach(i -> formattedItems.add(new TranslatableComponent(i.getDescriptionId())));
        if (acc.getTag() != null)
            Registry.ITEM.getTag(acc.getTag()).ifPresent(n -> n.forEach(h -> formattedItems.add(new TranslatableComponent(h.value().getDescriptionId()))));
        return formattedItems;
    }
}
