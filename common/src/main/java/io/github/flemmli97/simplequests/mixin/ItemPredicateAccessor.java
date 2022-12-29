package io.github.flemmli97.simplequests.mixin;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ItemPredicate.class)
public interface ItemPredicateAccessor {

    @Accessor("tag")
    TagKey<Item> getTag();

    @Accessor("items")
    Set<Item> getItems();

}
