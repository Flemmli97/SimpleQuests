package io.github.flemmli97.simplequests_api.mixin;

import io.github.flemmli97.simplequests_api.util.PredicateTranslation;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(BlockPredicate.class)
public abstract class BlockPredicateMixin implements PredicateTranslation {

    @Shadow
    @Final
    private StatePropertiesPredicate properties;

    @Shadow
    @Final
    private NbtPredicate nbt;

    @Shadow
    @Final
    @Nullable
    private Set<Block> blocks;
    @Shadow
    @Final
    @Nullable
    private TagKey<Block> tag;
    @Unique
    private List<MutableComponent> simplequests_api$computedTranslation;

    @Override
    public List<MutableComponent> translation(boolean cache) {
        if ((Object) this == BlockPredicate.ANY)
            return List.of(Component.literal(""));
        if ((this.blocks == null && this.tag == null) || this.properties != StatePropertiesPredicate.ANY || this.nbt != NbtPredicate.ANY)
            return null;
        if (this.simplequests_api$computedTranslation != null)
            return this.simplequests_api$computedTranslation;
        List<MutableComponent> formattedItems = new ArrayList<>();
        if (this.blocks != null)
            this.blocks.forEach(i -> formattedItems.add(Component.translatable(i.getDescriptionId())));
        if (this.tag != null) {
            formattedItems.add(QuestUtils.tagsComponent(this.tag, BuiltInRegistries.BLOCK, Block::getName));
        }
        if (!cache)
            return formattedItems;
        this.simplequests_api$computedTranslation = formattedItems;
        return this.simplequests_api$computedTranslation;
    }
}
