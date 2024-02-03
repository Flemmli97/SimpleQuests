package io.github.flemmli97.simplequests.quest.entry;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests.JsonCodecs;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class QuestEntryMultiImpl {

    public static class MultiItemEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_item");
        public static final Codec<MultiItemEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "predicates cant' be empty").fieldOf("predicates").forGetter(d -> d.predicate),
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.BOOL.fieldOf("consumeItems").forGetter(d -> d.consumeItems),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (pred, amount, consume, desc, player) -> new MultiItemEntry(pred, amount, desc, consume, player.orElse(null))));

        private final List<Either<ItemPredicate, Pair<ItemPredicate, String>>> predicate;
        private final NumberProvider amount;
        private final boolean consumeItems;
        private final EntityPredicate playerPredicate;

        public MultiItemEntry(List<Either<ItemPredicate, Pair<ItemPredicate, String>>> predicate, NumberProvider amount, String description, boolean consumeItems, EntityPredicate playerPredicate) {
            super(description);
            this.predicate = predicate;
            this.amount = amount;
            this.consumeItems = consumeItems;
            this.playerPredicate = playerPredicate;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Either<ItemPredicate, Pair<ItemPredicate, String>> val = this.predicate.get(ctx.getRandom().nextInt(this.predicate.size()));
            return new QuestEntryImpls.ItemEntry(val.map(e -> e, Pair::getFirst),
                    this.amount.getInt(ctx), val.map(e -> "", Pair::getSecond), this.consumeItems, this.playerPredicate);
        }
    }

    public static class MultiKillEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_kill");
        public static final Codec<MultiKillEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.ENTITY_PREDICATE_CODEC, "predicates can't be empty").fieldOf("predicates").forGetter(d -> d.predicate),
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, MultiKillEntry::new));

        private final List<Either<EntityPredicate, Pair<EntityPredicate, String>>> predicate;
        private final NumberProvider amount;
        private final EntityPredicate playerPredicate;

        public MultiKillEntry(List<Either<EntityPredicate, Pair<EntityPredicate, String>>> predicate, NumberProvider amount, String description, Optional<EntityPredicate> player) {
            super(description);
            this.predicate = predicate;
            this.amount = amount;
            this.playerPredicate = player.orElse(null);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Either<EntityPredicate, Pair<EntityPredicate, String>> val = this.predicate.get(ctx.getRandom().nextInt(this.predicate.size()));
            return new QuestEntryImpls.KillEntry(val.map(e -> e, Pair::getFirst),
                    this.amount.getInt(ctx), val.map(e -> "", Pair::getSecond), this.playerPredicate);
        }
    }

    public static class XPRangeEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_xp");
        public static final Codec<XPRangeEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, XPRangeEntry::new));

        private final NumberProvider amount;
        private final EntityPredicate playerPredicate;

        public XPRangeEntry(NumberProvider amount, String description, Optional<EntityPredicate> player) {
            super(description);
            this.amount = amount;
            this.playerPredicate = player.orElse(null);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            return new QuestEntryImpls.XPEntry(this.amount.getInt(ctx), this.playerPredicate);
        }
    }

    public static class MultiAdvancementEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_advancements");
        public static final Codec<MultiAdvancementEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.nonEmptyList(ResourceLocation.CODEC, "advancements list can't be empty").fieldOf("advancements").forGetter(d -> d.advancements),
                        Codec.BOOL.fieldOf("reset").forGetter(d -> d.reset),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, MultiAdvancementEntry::new));

        private final List<ResourceLocation> advancements;
        private final boolean reset;
        private final EntityPredicate playerPredicate;

        public MultiAdvancementEntry(List<ResourceLocation> advancements, boolean reset, String description, Optional<EntityPredicate> player) {
            super(description);
            this.advancements = advancements;
            this.reset = reset;
            this.playerPredicate = player.orElse(null);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            return new QuestEntryImpls.AdvancementEntry(this.advancements.get(ctx.getRandom().nextInt(this.advancements.size())), this.reset, this.playerPredicate);
        }
    }

    public static class MultiPositionEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_position");
        public static final Codec<MultiPositionEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.optionalDescriptiveList(JsonCodecs.BLOCK_POS_CODEC, "positions can't be empty").fieldOf("positions").forGetter(d -> d.positions),
                        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("minDist").forGetter(d -> d.minDist),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, MultiPositionEntry::new));

        private final List<Either<BlockPos, Pair<BlockPos, String>>> positions;
        private final int minDist;
        private final EntityPredicate playerPredicate;

        public MultiPositionEntry(List<Either<BlockPos, Pair<BlockPos, String>>> positions, int minDist, String description, Optional<EntityPredicate> player) {
            super(description);
            this.positions = positions;
            this.minDist = minDist;
            this.playerPredicate = player.orElse(null);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Either<BlockPos, Pair<BlockPos, String>> val = this.positions.get(ctx.getRandom().nextInt(this.positions.size()));
            return new QuestEntryImpls.PositionEntry(val.map(e -> e, Pair::getFirst), this.minDist, val.map(e -> "", Pair::getSecond), this.playerPredicate);
        }
    }

    public static class MultiLocationEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_location");
        public static final Codec<MultiLocationEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(JsonCodecs.descriptiveList(JsonCodecs.LOCATION_PREDICATE_CODEC, "location predicates can't be empty").fieldOf("locations").forGetter(d -> d.locations),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, MultiLocationEntry::new));

        private final List<Pair<LocationPredicate, String>> locations;
        private final EntityPredicate playerPredicate;

        public MultiLocationEntry(List<Pair<LocationPredicate, String>> locations, String description, Optional<EntityPredicate> player) {
            super(description);
            this.locations = locations;
            this.playerPredicate = player.orElse(null);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Pair<LocationPredicate, String> val = this.locations.get(ctx.getRandom().nextInt(this.locations.size()));
            return new QuestEntryImpls.LocationEntry(val.getFirst(), val.getSecond(), this.playerPredicate);
        }
    }

    public static class MultiEntityInteractEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_entity_interaction");
        public static final Codec<MultiEntityInteractEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.BOOL.fieldOf("consume").forGetter(d -> d.consume),
                        Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                        JsonCodecs.descriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "empty item predicates")
                                .optionalFieldOf("itemPredicates").forGetter(d -> d.heldItems.isEmpty() ? Optional.empty() : Optional.of(d.heldItems)),
                        JsonCodecs.descriptiveList(JsonCodecs.ENTITY_PREDICATE_CODEC, "empty entity predicates")
                                .optionalFieldOf("entityPredicates").forGetter(d -> d.entityPredicates.isEmpty() ? Optional.empty() : Optional.of(d.entityPredicates)),
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount),
                        JsonCodecs.ENTITY_PREDICATE_CODEC.optionalFieldOf("playerPredicate").forGetter(d -> Optional.ofNullable(d.playerPredicate))
                ).apply(instance, (consume, desc, taskDescription, item, pred, amount, player) -> new MultiEntityInteractEntry(item.orElse(List.of()), pred.orElse(List.of()), amount, consume, desc, taskDescription, player.orElse(null))));

        private final List<Pair<ItemPredicate, String>> heldItems;
        private final List<Pair<EntityPredicate, String>> entityPredicates;
        private final NumberProvider amount;
        private final boolean consume;
        private final String taskDescription;
        private final EntityPredicate playerPredicate;

        public MultiEntityInteractEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<EntityPredicate, String>> entityPredicates, NumberProvider amount, boolean consume, String description, String taskDescription, EntityPredicate playerPredicate) {
            super(description);
            this.heldItems = heldItems;
            this.entityPredicates = entityPredicates;
            this.amount = amount;
            this.consume = consume;
            this.taskDescription = taskDescription;
            this.playerPredicate = playerPredicate;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
            Pair<EntityPredicate, String> entity = this.entityPredicates.isEmpty() ? Pair.of(EntityPredicate.ANY, "") : this.entityPredicates.get(ctx.getRandom().nextInt(this.entityPredicates.size()));
            return new QuestEntryImpls.EntityInteractEntry(val.getFirst(), entity.getFirst(), this.amount.getInt(ctx), this.consume, this.taskDescription, val.getSecond(), entity.getSecond(), this.playerPredicate);
        }
    }

    public static class MultiBlockInteractEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_block_interaction");
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
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
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
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
            Pair<BlockPredicate, String> entity = this.blockPredicates.isEmpty() ? Pair.of(BlockPredicate.ANY, "") : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
            while (val.getFirst() == ItemPredicate.ANY && entity.getFirst() == BlockPredicate.ANY) {
                val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
                entity = this.blockPredicates.isEmpty() ? Pair.of(BlockPredicate.ANY, "") : this.blockPredicates.get(ctx.getRandom().nextInt(this.blockPredicates.size()));
            }
            return new QuestEntryImpls.BlockInteractEntry(val.getFirst(), entity.getFirst(), this.amount.getInt(ctx), this.use, this.consume, this.allowDupes, this.taskDescription, val.getSecond(), entity.getSecond(), this.playerPredicate);
        }
    }

    public static class MultiCraftingEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_crafting");
        public static final Codec<MultiCraftingEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                        JsonCodecs.descriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "item predicates can't be empty").fieldOf("itemPredicates").forGetter(d -> d.heldItems),
                        Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(JsonCodecs.ENTITY_PREDICATE_CODEC, Codec.STRING)).listOf()
                                .optionalFieldOf("entityPredicates").forGetter(d -> d.entityPredicates.isEmpty() ? Optional.empty() : Optional.of(d.entityPredicates)),
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
                ).apply(instance, (desc, taskDescription, item, pred, amount) -> new MultiCraftingEntry(item, pred.orElse(List.of()), amount, desc, taskDescription)));

        private final List<Pair<ItemPredicate, String>> heldItems;
        private final List<Pair<EntityPredicate, String>> entityPredicates;
        private final NumberProvider amount;
        private final String taskDescription;

        public MultiCraftingEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<EntityPredicate, String>> entityPredicates, NumberProvider amount, String description, String taskDescription) {
            super(description);
            this.heldItems = heldItems;
            this.entityPredicates = entityPredicates;
            this.amount = amount;
            this.taskDescription = taskDescription;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
            Pair<EntityPredicate, String> entity = this.entityPredicates.isEmpty() ? Pair.of(EntityPredicate.ANY, "") : this.entityPredicates.get(ctx.getRandom().nextInt(this.entityPredicates.size()));
            return new QuestEntryImpls.CraftingEntry(val.getFirst(), entity.getFirst(), this.amount.getInt(ctx), this.taskDescription, val.getSecond(), entity.getSecond());
        }
    }

    public static class MultiFishingEntry extends MultiQuestEntryBase {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "multi_fishing");
        public static final Codec<MultiFishingEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.STRING.fieldOf("description").forGetter(d -> d.description),
                        Codec.STRING.fieldOf("taskDescription").forGetter(d -> d.taskDescription),

                        JsonCodecs.descriptiveList(JsonCodecs.ITEM_PREDICATE_CODEC, "item predicates can't be empty").fieldOf("itemPredicates").forGetter(d -> d.heldItems),
                        Codec.STRING.dispatch("description", Pair::getSecond, e -> Codec.pair(JsonCodecs.ENTITY_PREDICATE_CODEC, Codec.STRING)).listOf()
                                .optionalFieldOf("entityPredicates").forGetter(d -> d.entityPredicates.isEmpty() ? Optional.empty() : Optional.of(d.entityPredicates)),
                        JsonCodecs.NUMER_PROVIDER_CODEC.fieldOf("amount").forGetter(d -> d.amount)
                ).apply(instance, (desc, taskDescription, item, pred, amount) -> new MultiFishingEntry(item, pred.orElse(List.of()), amount, desc, taskDescription)));

        private final List<Pair<ItemPredicate, String>> heldItems;
        private final List<Pair<EntityPredicate, String>> entityPredicates;
        private final NumberProvider amount;
        private final String taskDescription;

        public MultiFishingEntry(List<Pair<ItemPredicate, String>> heldItems, List<Pair<EntityPredicate, String>> entityPredicates, NumberProvider amount, String description, String taskDescription) {
            super(description);
            this.heldItems = heldItems;
            this.entityPredicates = entityPredicates;
            this.amount = amount;
            this.taskDescription = taskDescription;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public QuestEntry resolve(ServerPlayer player, QuestBase base) {
            LootContext ctx = SimpleQuests.createContext(player, player, base.id);
            Pair<ItemPredicate, String> val = this.heldItems.isEmpty() ? Pair.of(ItemPredicate.ANY, "") : this.heldItems.get(ctx.getRandom().nextInt(this.heldItems.size()));
            Pair<EntityPredicate, String> entity = this.entityPredicates.isEmpty() ? Pair.of(EntityPredicate.ANY, "") : this.entityPredicates.get(ctx.getRandom().nextInt(this.entityPredicates.size()));
            return new QuestEntryImpls.FishingEntry(val.getFirst(), entity.getFirst(), this.amount.getInt(ctx), this.taskDescription, val.getSecond(), entity.getSecond());
        }
    }
}
