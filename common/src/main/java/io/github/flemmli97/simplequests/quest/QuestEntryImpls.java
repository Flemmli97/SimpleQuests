package io.github.flemmli97.simplequests.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests.JsonCodecs;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.mixin.EntityPredicateAccessor;
import io.github.flemmli97.simplequests.mixin.ItemPredicateAccessor;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class QuestEntryImpls {

    public record ItemEntry(ItemPredicate predicate, int amount,
                            String description, boolean consumeItems) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "item");

        public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.ITEM_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.predicate),
                        Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description)),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.BOOL.fieldOf("consumeItems").forGetter(d -> d.consumeItems)
                ).apply(instance, (pred, desc, amount, consume) -> new ItemEntry(pred, amount, desc.orElse(""), consume)));

        @Override
        public boolean submit(ServerPlayer player) {
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
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            Function<String, String> key = s -> !this.description.isEmpty() ? this.description : ConfigHandler.lang.get(this.getId().toString() + s);
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

    public record KillEntry(EntityPredicate predicate, int amount, String description) implements QuestEntry {

        public static final Codec<KillEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.ENTITY_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.predicate),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description))
                ).apply(instance, (pred, amount, desc) -> new KillEntry(pred, amount, desc.orElse(""))));

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "entity");

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            EntityPredicateAccessor acc = (EntityPredicateAccessor) this.predicate;
            String s = acc.getEntityType().serializeToJson().getAsString();
            if (s.startsWith("#")) {
                return new TranslatableComponent(!this.description.isEmpty() ? this.description : ConfigHandler.lang.get(this.getId().toString() + ".tag"), new TextComponent(s).withStyle(ChatFormatting.AQUA), this.amount);
            }
            return new TranslatableComponent(!this.description.isEmpty() ? this.description : ConfigHandler.lang.get(this.getId().toString()), new TranslatableComponent(Util.makeDescriptionId("entity", new ResourceLocation(s))).withStyle(ChatFormatting.AQUA), this.amount);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.killProgress(player, id);
        }
    }

    public record XPEntry(int amount) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "xp");
        public static final Codec<XPEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount)).apply(instance, XPEntry::new));

        @Override
        public boolean submit(ServerPlayer player) {
            if (player.experienceLevel >= this.amount) {
                player.giveExperienceLevels(-this.amount);
                return true;
            }
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(ConfigHandler.lang.get(this.getId().toString()), this.amount);
        }
    }

    public record AdvancementEntry(ResourceLocation advancement, boolean reset) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "advancement");
        public static final Codec<AdvancementEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(ResourceLocation.CODEC.fieldOf("advancement").forGetter(d -> d.advancement),
                        Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset)
                ).apply(instance, AdvancementEntry::new));

        @Override
        public boolean submit(ServerPlayer player) {
            Advancement adv = player.getServer().getAdvancements().getAdvancement(this.advancement);
            boolean ret = adv != null && (player.getAdvancements().getOrStartProgress(adv).isDone());
            if (ret && this.reset) {
                AdvancementProgress prog = player.getAdvancements().getOrStartProgress(adv);
                prog.getCompletedCriteria().forEach(s -> player.getAdvancements().revoke(adv, s));
            }
            return ret;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            Advancement advancement = player.getServer().getAdvancements().getAdvancement(this.advancement());
            Component adv;
            if (advancement == null)
                adv = new TranslatableComponent(ConfigHandler.lang.get("simplequests.missing.advancement"), this.advancement());
            else
                adv = advancement.getChatComponent();
            return new TranslatableComponent(ConfigHandler.lang.get(this.getId().toString()), adv);
        }
    }

    public record PositionEntry(BlockPos pos, int minDist, String description) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "position");
        public static final Codec<PositionEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(BlockPos.CODEC.fieldOf("pos").forGetter(d -> d.pos),
                        Codec.INT.fieldOf("minDist").forGetter(d -> d.minDist),
                        Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description))
                ).apply(instance, (pred, amount, desc) -> new PositionEntry(pred, amount, desc.orElse(""))));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(!this.description.isEmpty() ? this.description : ConfigHandler.lang.get(this.getId().toString()), this.pos.getX(), this.pos.getY(), this.pos.getZ());
        }

        @Override
        public Function<PlayerData, Boolean> tickable() {
            return d -> {
                ServerPlayer p = d.getPlayer();
                return p.tickCount % 20 == 0 && p.blockPosition().distSqr(this.pos) < this.minDist * this.minDist;
            };
        }
    }

    /**
     * Quest entry to check if a player matches a given location.
     *
     * @param location    The LocationPredicate to check
     * @param description Parsing a location predicate is way too complicated. Its easier instead to have the datapack maker provide a description instead
     */
    public record LocationEntry(LocationPredicate location, String description) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "location");

        public static final Codec<LocationEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.LOCATION_PREDICATE_CODEC.fieldOf("predicate").forGetter(d -> d.location),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description)
                ).apply(instance, LocationEntry::new));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(this.description);
        }

        @Override
        public Function<PlayerData, Boolean> tickable() {
            return d -> {
                ServerPlayer p = d.getPlayer();
                return p.tickCount % 20 == 0 && this.location.matches(p.getLevel(), p.getX(), p.getY(), p.getZ());
            };
        }
    }

    /**
     * Quest entry to check if a player interacts with an entity.
     *
     * @param description Parsing the predicates is way too complicated. Its easier instead to have the datapack maker provide a description instead
     */
    public record EntityInteractEntry(ItemPredicate heldItem, EntityPredicate entityPredicate, int amount,
                                      boolean consume,
                                      String description) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "entity_interact");
        public static final Codec<EntityInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),

                        JsonCodecs.ITEM_PREDICATE_CODEC.optionalFieldOf("item").forGetter(d -> d.heldItem == ItemPredicate.ANY ? Optional.empty() : Optional.of(d.heldItem)),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("predicate").forGetter(d -> d.entityPredicate == EntityPredicate.ANY ? Optional.empty() : Optional.of(d.entityPredicate)),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount)
                ).apply(instance, (consume, desc, item, pred, amount) -> new EntityInteractEntry(item.orElse(ItemPredicate.ANY), pred.orElse(EntityPredicate.ANY), amount, consume, desc)));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(this.description);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.interactProgress(player, id);
        }

        public boolean check(ServerPlayer player, Entity entity) {
            boolean b = this.heldItem.matches(player.getMainHandItem()) && this.entityPredicate.matches(player, entity);
            if (b && this.consume && !player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }
            return b;
        }
    }

    /**
     * Quest entry to check when a player interacts with a block
     *
     * @param use If the player should use (right click) or break the block
     */
    public record BlockInteractEntry(ItemPredicate heldItem, BlockPredicate blockPredicate, int amount, boolean use,
                                     boolean consumeItem, String description) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "block_interact");
        public static final Codec<BlockInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.BOOL.fieldOf("use").forGetter(d -> d.use),
                        Codec.BOOL.fieldOf("consumeItem").forGetter(d -> d.consumeItem),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),

                        JsonCodecs.ITEM_PREDICATE_CODEC.optionalFieldOf("item").forGetter(d -> d.heldItem == ItemPredicate.ANY ? Optional.empty() : Optional.of(d.heldItem)),
                        JsonCodecs.BLOCK_PREDICATE_CODEC.optionalFieldOf("block").forGetter(d -> d.blockPredicate == BlockPredicate.ANY ? Optional.empty() : Optional.of(d.blockPredicate)),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount)
                ).apply(instance, (use, consume, desc, item, block, amount) -> {
                    ItemPredicate itemPredicate = item.orElse(ItemPredicate.ANY);
                    BlockPredicate blockPredicate = block.orElse(BlockPredicate.ANY);
                    if (itemPredicate == ItemPredicate.ANY && blockPredicate == BlockPredicate.ANY)
                        throw new IllegalStateException("Either item or block has to be defined");
                    return new BlockInteractEntry(itemPredicate, blockPredicate, amount, use, consume, desc);
                }));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(this.description);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.blockInteractProgress(player, id);
        }

        public boolean check(ServerPlayer player, BlockPos pos, boolean use) {
            if (use != this.use)
                return false;
            boolean b = this.heldItem.matches(player.getMainHandItem()) && this.blockPredicate.matches(player.getLevel(), pos);
            if (b && this.consumeItem && !player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }
            return b;
        }
    }

    /**
     * Quest entry to check for when a player crafts something
     */
    public record CraftingEntry(ItemPredicate item, EntityPredicate playerPredicate, int amount,
                                String description) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "crafting");
        public static final Codec<CraftingEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.ITEM_PREDICATE_CODEC.fieldOf("item").forGetter(d -> d.item),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> d.playerPredicate == EntityPredicate.ANY ? Optional.empty() : Optional.of(d.playerPredicate)),
                        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description)
                ).apply(instance, (item, pred, amount, desc) -> new CraftingEntry(item, pred.orElse(EntityPredicate.ANY), amount, desc)));

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public MutableComponent translation(ServerPlayer player) {
            return new TranslatableComponent(this.description);
        }

        @Nullable
        @Override
        public MutableComponent progress(ServerPlayer player, QuestProgress progress, String id) {
            return progress.craftingProgress(player, id);
        }

        public boolean check(ServerPlayer player, ItemStack stack) {
            return this.item.matches(stack) && this.playerPredicate.matches(player, player);
        }
    }
}
