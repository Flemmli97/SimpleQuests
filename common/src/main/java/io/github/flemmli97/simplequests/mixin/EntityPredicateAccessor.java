package io.github.flemmli97.simplequests.mixin;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPredicate.class)
public interface EntityPredicateAccessor {

    @Accessor("entityType")
    EntityTypePredicate getEntityType();

}
