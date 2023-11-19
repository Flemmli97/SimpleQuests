package io.github.flemmli97.simplequests.forge.data;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.provider.QuestProvider;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryImpls;
import io.github.flemmli97.simplequests.quest.entry.QuestEntryMultiImpl;
import io.github.flemmli97.simplequests.quest.types.CompositeQuest;
import io.github.flemmli97.simplequests.quest.types.Quest;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import io.github.flemmli97.simplequests.quest.types.SequentialQuest;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Mod.EventBusSubscriber(modid = SimpleQuests.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExampleQuestPackGenerator extends QuestProvider {

    private static final String PACK_META = "{\"pack\": {\"pack_format\": 9,\"description\": [{\"text\":\"Example Quests\",\"color\":\"gold\"}]}}";

    public ExampleQuestPackGenerator(DataGenerator gen, boolean full) {
        super(createGenerator(gen), full);
    }

    /**
     * Reroute to Example Questpack folder
     */
    private static DataGenerator createGenerator(DataGenerator old) {
        String path = System.getProperty("ExampleGenPath");
        if (path == null || path.isEmpty())
            return old;
        DataGenerator newGen = new DataGenerator(Path.of(path), old.getInputFolders());
        old.getProviders().forEach(newGen::addProvider);
        return newGen;
    }

    @SubscribeEvent
    public static void data(GatherDataEvent event) {
        DataGenerator data = event.getGenerator();
        if (event.includeServer()) {
            data.addProvider(new ExampleQuestPackGenerator(data, true));
        }
        if (event.includeClient()) {
            data.addProvider(new LangGen(data));
        }
    }

    @Override
    protected void add() {
        //Advancement example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advancement_example"),
                "Example for an advancement quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.EMERALD, 5))
                .addTaskEntry("trade", new QuestEntryImpls.AdvancementEntry(new ResourceLocation("minecraft:adventure/trade"), true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/advancement_example_multi"),
                "Example for a multi advancements quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.MAP, 5))
                .addTaskEntry("trade", new QuestEntryMultiImpl.MultiAdvancementEntry(List.of(new ResourceLocation("minecraft:adventure/trade"),
                        new ResourceLocation("minecraft:adventure/bullseye"),
                        new ResourceLocation("minecraft:adventure/ol_betsy")), true,
                        "Task Description. Will select one of the following advancements: adventure/trade, adventure/bullseye, adventure/ol_betsy")));

        //Entity interact example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "interact_example"),
                "Example for an entity interaction quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("interact", new QuestEntryImpls.EntityInteractEntry(ItemPredicate.Builder.item().of(Items.NAME_TAG).build(), EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.CHICKEN)).build(), 2, true, "Use nametag on 2 chickens")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/interact_example_multi"),
                "Example for a multi entity interaction quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("interact", new QuestEntryMultiImpl.MultiEntityInteractEntry(List.of(Pair.of(ItemPredicate.Builder.item().of(Items.NAME_TAG).build(), "nametag")),
                        List.of(Pair.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.CHICKEN)).build(), "chicken"),
                                Pair.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build(), "cow")),
                        UniformGenerator.between(3, 6), true, "Use a nametag on either a cow/chicken 3-6 times", "Use %1$s on %2$s %3$sx")));

        //Item example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/item_example_multi"),
                "Example for a multi item quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.COBBLESTONE))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new QuestEntryMultiImpl.MultiItemEntry(List.of(
                        Either.left(ItemPredicate.Builder.item().of(Items.SALMON).build()),
                        Either.right(Pair.of(ItemPredicate.Builder.item().of(Items.COD).build(), "cod"))), UniformGenerator.between(10, 15), "Give 10-15 cods or salmon", true)));

        //Kill example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "kill_example"),
                "Example for a kill quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("3d:5h")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("cows", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build(), 15, "")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/kill_example_multi"),
                "Example for a multi kill quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("3d:5h")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("cows", new QuestEntryMultiImpl.MultiKillEntry(List.of(
                        Either.left(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build()),
                        Either.right(Pair.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW))
                                .located(LocationPredicate.inBiome(Biomes.PLAINS)).build(), "Kill %2$s plains cow"))), UniformGenerator.between(5, 8), "Task: 5-8 cows or cows in a plains biome")));

        //Location example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "location_example"),
                "Example for a location quest using a location predicate",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.MAP))
                .addTaskEntry("structure", new QuestEntryImpls.LocationEntry(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_warm"))), "Find a warm ocean ruin"))
                .addTaskEntry("structure2", new QuestEntryImpls.LocationEntry(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_cold"))), "Find a cold ocean ruin")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/location_example_multi"),
                "Example for a multi location quest using a location predicate",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.COMPASS))
                .addTaskEntry("structure", new QuestEntryMultiImpl.MultiLocationEntry(List.of(
                        Pair.of(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_warm"))), "Go to warm ocean ruin"),
                        Pair.of(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_cold"))), "Go to cold ocean ruin")), "Find a warm or cold ocean ruin")));

        //Position example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "position_example"),
                "Example for a simple position quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("place", new QuestEntryImpls.PositionEntry(new BlockPos(0, 50, 0), 15, "")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/position_example_multi"),
                "Example for a multi position quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("place", new QuestEntryMultiImpl.MultiPositionEntry(List.of(
                        Either.left(new BlockPos(0, 50, 0)),
                        Either.right(Pair.of(new BlockPos(100, 50, 100), "Position description. Go to 100, 50, 100"))), 15, "Go to the position from this quest")));

        //XP example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "xp_example"),
                "Example for an xp quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("xp", new QuestEntryImpls.XPEntry(5)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/xp_example_multi"),
                "Example for an xp with range quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("xp", new QuestEntryMultiImpl.XPRangeEntry(UniformGenerator.between(5, 10), "Submit xp to this quest")));

        //Block Interact example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_interact"),
                "Example for block interaction task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("task", new QuestEntryImpls.BlockInteractEntry(ItemPredicate.ANY, BlockPredicate.Builder.block().of(Blocks.CHEST).build(), 3, true, false, "Interact with chest x3")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_break"),
                "Example for block breaking task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("break", new QuestEntryImpls.BlockInteractEntry(ItemPredicate.Builder.item().of(Items.DIAMOND_PICKAXE).build(), BlockPredicate.Builder.block().of(Blocks.DIAMOND_ORE).build(), 3, false, false, "Mine diamond ore x3")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_place"),
                "Example of using block interaction as block place detection",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.EMERALD_BLOCK))
                .addTaskEntry("place", new QuestEntryImpls.BlockInteractEntry(ItemPredicate.Builder.item().of(Items.EMERALD_BLOCK).build(), BlockPredicate.ANY, 3, true, false, "Place 3 emerald blocks")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/block_place_multi"),
                "Example of using multi block place task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.EMERALD_BLOCK))
                .addTaskEntry("place", new QuestEntryMultiImpl.MultiBlockInteractEntry(List.of(
                        Pair.of(ItemPredicate.Builder.item().of(Items.EMERALD_BLOCK).build(), "emerald")), List.of(), UniformGenerator.between(3, 5), true, false,
                        "Place the blocks from the quest", "Place %1$s %3$sx")));

        //Crafting example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "crafting_task"),
                "Example of use of a crafting task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.STICK))
                .addTaskEntry("sticks", new QuestEntryImpls.CraftingEntry(ItemPredicate.Builder.item().of(Items.STICK).build(), EntityPredicate.ANY, 3, "Craft at 8 sticks")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/crafting_task_multi"),
                "Example of use of a multi crafting task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.CRAFTING_TABLE))
                .addTaskEntry("crafting", new QuestEntryMultiImpl.MultiFishingEntry(List.of(
                        Pair.of(ItemPredicate.Builder.item().of(Items.STICK).build(), "sticks"),
                        Pair.of(ItemPredicate.Builder.item().of(Items.FURNACE).build(), "furnace")), List.of(), UniformGenerator.between(3, 5), "Craft some items", "Craft %1$s %3$sx")));

        //Fishing example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "fishing_task"),
                "Example of use of a fishing task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.FISHING_ROD))
                .addTaskEntry("cod", new QuestEntryImpls.FishingEntry(ItemPredicate.Builder.item().of(Items.COD).build(), EntityPredicate.ANY, 5, "Fish 5 cods")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/fishing_task_multi"),
                "Example of use of a fishing task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.FISHING_ROD))
                .addTaskEntry("fish", new QuestEntryMultiImpl.MultiFishingEntry(List.of(
                        Pair.of(ItemPredicate.Builder.item().of(Items.COD).build(), "cod"),
                        Pair.of(ItemPredicate.Builder.item().of(Items.SALMON).build(), "salmon")), List.of(), UniformGenerator.between(3, 5), "Fish some fishes", "Fish %1$s %3$sx")));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "daily_quest_item"),
                "Example for an daily item quest",
                new ResourceLocation("chests/buried_treasure"))
                .withIcon(new ItemStack(Items.DIRT))
                .setDailyQuest()
                .addTaskEntry("interact", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 5, "Give 5 cods", true)));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/advanced_item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("sword", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.DIAMOND_SWORD)
                        .hasEnchantment(new EnchantmentPredicate(Enchantments.SHARPNESS, MinMaxBounds.Ints.between(2, 3)))
                        .build(), 1, "Give 1 diamond sword with sharp 2 or 3", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/overworld_hostile"),
                "Slay mobs",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("1w")
                .withIcon(new ItemStack(Items.DIAMOND_SWORD))
                .addTaskEntry("zombies", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE)).build(), 10, ""))
                .addTaskEntry("zombies_baby", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE))
                        .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true).build()).build(), 3, ""))
                .addTaskEntry("skeleton", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SKELETON)).build(), 10, ""))
                .addTaskEntry("spiders", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SPIDER)).build(), 5, ""))
                .addTaskEntry("xp", new QuestEntryImpls.XPEntry(5)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/locked_nether_hostile"),
                "Example of a locked quest. Needs to be unlocked (via command) to accept it.",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2w")
                .needsUnlocking()
                .withIcon(new ItemStack(Items.NETHER_BRICK))
                .addTaskEntry("piglin", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIFIED_PIGLIN)).build(), 15, ""))
                .addTaskEntry("blaze", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.BLAZE)).build(), 5, ""))
                .addTaskEntry("wither_skeleton", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.WITHER_SKELETON)).build(), 5, ""))
                .addTaskEntry("fortress", new QuestEntryImpls.AdvancementEntry(new ResourceLocation("minecraft:nether/find_fortress"), false)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/child_fishing_quest"),
                "Catch lots of fishes! Needs parent quest completed before doing this quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2h")
                .addParent(new ResourceLocation("example", "item_example"))
                .withIcon(new ItemStack(Items.COD))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true))
                .addTaskEntry("fish_2", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.SALMON).build(), 15, "Give 15 salmon", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/advanced_block_break"),
                "Mine a east facing quartz block with an unbreaking diamond pickaxe 3 times",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.DIAMOND_PICKAXE))
                .addTaskEntry("break", new QuestEntryImpls.BlockInteractEntry(ItemPredicate.Builder.item().of(Items.DIAMOND_PICKAXE)
                        .hasEnchantment(new EnchantmentPredicate(Enchantments.UNBREAKING, MinMaxBounds.Ints.atLeast(1))).build(),
                        BlockPredicate.Builder.block().of(Blocks.QUARTZ_STAIRS)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HorizontalDirectionalBlock.FACING, Direction.EAST).build()).build(), 3, false, false, "Quartz Stairs")));

        // Visibility tests
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "visibility/requirements"),
                "Example for an item quest needing an requirements to accept",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .setVisibility(QuestBase.Visibility.ALWAYS)
                .withUnlockCondition(EntityPredicate.Builder.entity().located(LocationPredicate.atYLocation(MinMaxBounds.Doubles.atLeast(64))).build())
                .addDescription("Requires player to be y > 64")
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "visibility/daily_limit"),
                "Example for an item quest completable only once per day",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setMaxDaily(1)
                .withIcon(new ItemStack(Items.DIRT))
                .setVisibility(QuestBase.Visibility.ALWAYS)
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true)));

        QuestCategory category = new QuestCategory.Builder(new ResourceLocation("example", "category_1"), "Example category 1")
                .withIcon(new ItemStack(Items.DIAMOND_BLOCK)).build();
        QuestCategory category2 = new QuestCategory.Builder(new ResourceLocation("example", "category_2"), "Example category 2")
                .withIcon(new ItemStack(Items.BEACON))
                .unselectable().build();
        QuestCategory hidden = new QuestCategory.Builder(new ResourceLocation("example", "hidden"), "For selection quests")
                .withIcon(new ItemStack(Items.BEACON))
                .unselectable().build();
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_1"),
                "Example for an item quest with category 1",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category)
                .withIcon(new ItemStack(Items.STONE))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.STONE).build(), 15, "Give 15 stone", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_2"),
                "Example for an item quest with category 2",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category2)
                .withIcon(new ItemStack(Items.ANDESITE))
                .addTaskEntry("andesite", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.ANDESITE).build(), 15, "Give 15 andesite", true)));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/selection_a"),
                "Selection Quest Example a",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(hidden)
                .withIcon(new ItemStack(Items.ANDESITE))
                .addTaskEntry("andesite", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.ANDESITE).build(), 15, "Give 15 andesite", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/selection_b"),
                "Selection Quest Example b",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withCategory(hidden)
                .withIcon(new ItemStack(Items.GRANITE))
                .addTaskEntry("granite", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.GRANITE).build(), 15, "Give 15 granite", true)));
        this.addQuest(new CompositeQuest.Builder(new ResourceLocation("example", "selection_quest_example"),
                "Example for a selection quest")
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.COBBLED_DEEPSLATE))
                .addQuest(new ResourceLocation("example", "hidden/selection_a"))
                .addQuest(new ResourceLocation("example", "hidden/selection_b")));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/sequential_a"),
                "Sequential Quest Example a",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(hidden)
                .addTaskEntry("andesite", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.ANDESITE).build(), 15, "Give 15 andesite", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/sequential_b"),
                "Sequential Quest Example b",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withCategory(hidden)
                .addTaskEntry("granite", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.GRANITE).build(), 15, "Give 15 granite", true)));
        this.addQuest(new SequentialQuest.Builder(new ResourceLocation("example", "sequential_quest_example"),
                "Example for a sequential quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.COBBLED_DEEPSLATE))
                .addQuest(new ResourceLocation("example", "hidden/sequential_a"))
                .addQuest(new ResourceLocation("example", "hidden/sequential_b")));
    }

    @Override
    public void run(HashCache cache) {
        super.run(cache);
        Path path = this.gen.getOutputFolder().resolve("pack.mcmeta");
        try {
            DataProvider.save(GSON, cache, JsonParser.parseString(PACK_META), path);
        } catch (IOException ignored) {
        }
    }
}
