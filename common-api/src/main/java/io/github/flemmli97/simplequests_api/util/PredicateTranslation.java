package io.github.flemmli97.simplequests_api.util;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class PredicateTranslation {

    public static List<MutableComponent> translation(Object obj) {
        if (obj instanceof ItemPredicate predicate) {
            if (predicate.items().isEmpty() || !predicate.count().isAny()
                    || !predicate.components().alwaysMatches() || !predicate.subPredicates().isEmpty())
                return null;
            HolderSet<Item> holders = predicate.items().get();
            List<MutableComponent> formattedItems = new ArrayList<>();
            if (holders instanceof HolderSet.Named<Item> named) {
                formattedItems.add(QuestUtils.tagsComponent(named, i -> Component.translatable(i.getDescriptionId())));
            } else {
                holders.forEach(i -> formattedItems.add(Component.translatable(i.value().getDescriptionId())));
            }
            return formattedItems;
        }
        if (obj instanceof EntityPredicate predicate) {
            if (predicate.entityType().isEmpty()
                    || predicate.distanceToPlayer().isPresent() || predicate.movement().isPresent()
                    || predicate.location().located().isPresent() || predicate.location().steppingOn().isPresent() || predicate.location().affectsMovement().isPresent()
                    || predicate.effects().isPresent() || predicate.nbt().isPresent() || predicate.flags().isPresent()
                    || predicate.equipment().isPresent() || predicate.subPredicate().isPresent()
                    || predicate.periodicTick().isPresent() || predicate.vehicle().isPresent()
                    || predicate.passenger().isPresent() || predicate.targetedEntity().isPresent() || predicate.team().isPresent() || predicate.slots().isPresent())
                return null;
            HolderSet<EntityType<?>> holders = predicate.entityType().get().types();
            List<MutableComponent> formattedItems = new ArrayList<>();
            if (holders instanceof HolderSet.Named<EntityType<?>> named) {
                formattedItems.add(QuestUtils.tagsComponent(named, e -> Component.translatable(e.getDescriptionId())));
            } else {
                holders.forEach(i -> formattedItems.add(Component.translatable(i.value().getDescriptionId())));
            }
            return formattedItems;
        }
        if (obj instanceof BlockPredicate predicate) {
            if (predicate.blocks().isEmpty() || predicate.properties().isPresent() || predicate.nbt().isPresent())
                return null;
            HolderSet<Block> holders = predicate.blocks().get();
            List<MutableComponent> formattedItems = new ArrayList<>();
            if (holders instanceof HolderSet.Named<Block> named) {
                formattedItems.add(QuestUtils.tagsComponent(named, Block::getName));
            } else {
                holders.forEach(i -> formattedItems.add(Component.translatable(i.value().getDescriptionId())));
            }
            return formattedItems;
        }
        return null;
    }

}
