package io.github.flemmli97.simplequests_api.forge.data;

import com.google.gson.JsonParser;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.provider.QuestProvider;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuest;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.impls.quests.SequentialQuest;
import io.github.flemmli97.simplequests_api.impls.tasks.AdvancementTask;
import io.github.flemmli97.simplequests_api.impls.tasks.BlockInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.CraftingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.EntityInteractTask;
import io.github.flemmli97.simplequests_api.impls.tasks.FishingTask;
import io.github.flemmli97.simplequests_api.impls.tasks.ItemTask;
import io.github.flemmli97.simplequests_api.impls.tasks.KillTask;
import io.github.flemmli97.simplequests_api.impls.tasks.PredicateTask;
import io.github.flemmli97.simplequests_api.impls.tasks.XPTask;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import io.github.flemmli97.simplequests_api.quest.QuestCategory;
import io.github.flemmli97.simplequests_api.util.DescriptiveValue;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = SimpleQuestsAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExampleQuestPackGenerator extends QuestProvider {

    private static final String PACK_META = "{\"pack\": {\"pack_format\": 9,\"description\": [{\"text\":\"Example Quests\",\"color\":\"gold\"}]}}";

    public ExampleQuestPackGenerator(PackOutput output, boolean full) {
        super(createGenerator(output), full);
    }

    /**
     * Reroute to Example Questpack folder
     */
    private static PackOutput createGenerator(PackOutput old) {
        String path = System.getProperty("ExampleGenPath");
        if (path == null || path.isEmpty())
            return old;
        return new PackOutput(Path.of(path));
    }

    @SubscribeEvent
    public static void data(GatherDataEvent event) {
        DataGenerator data = event.getGenerator();
        data.addProvider(event.includeServer(), new ExampleQuestPackGenerator(data.getPackOutput(), true));
        data.addProvider(event.includeClient(), new LangAPIGen(data));
    }

    @Override
    protected void add() {
        //Advancement example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advancement_example"),
                "Example for an advancement quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.EMERALD, 5))
                .addTaskEntry("trade", new AdvancementTask(DescriptiveValue.list(new ResourceLocation("minecraft:adventure/trade")).build(), true, "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/advancement_example_multi"),
                "Example for a multi advancements quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.MAP, 5))
                .addTaskEntry("trade", new AdvancementTask(DescriptiveValue.list(new ResourceLocation("minecraft:adventure/trade"))
                        .add(new ResourceLocation("minecraft:adventure/bullseye"))
                        .add(new ResourceLocation("minecraft:adventure/ol_betsy")).build(), true,
                        "Task Description. Will select one of the following advancements: adventure/trade, adventure/bullseye, adventure/ol_betsy", null)));

        //Entity interact example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "interact_example"),
                "Example for an entity interaction quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("interact", new EntityInteractTask(DescriptiveValue.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.CHICKEN)).build()),
                        DescriptiveValue.of(ItemPredicate.Builder.item().of(Items.NAME_TAG).build()), 2, true, "Use nametag on 2 chickens", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/interact_example_multi"),
                "Example for a multi entity interaction quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("interact", new EntityInteractTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.CHICKEN)).build())
                        .add(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build()).build(),
                        DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.NAME_TAG).build()).build(),
                        UniformGenerator.between(3, 6), true, "Use a nametag on either a cow/chicken 3-6 times", null)));

        //Item example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list((ItemPredicate.Builder.item().of(Items.COD).build())).build(), ConstantValue.exactly(15), "Give 15 cods", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_tag_example"),
                "Example for an item tag quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.OAK_LOG))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list((ItemPredicate.Builder.item().of(ItemTags.LOGS).build())).build(), ConstantValue.exactly(15), "", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/item_example_multi"),
                "Example for a multi item quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.COBBLESTONE))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new ItemTask(List.of(
                        DescriptiveValue.of(ItemPredicate.Builder.item().of(Items.SALMON).build()),
                        DescriptiveValue.of(ItemPredicate.Builder.item().of(Items.COD).build(), "cod")), UniformGenerator.between(10, 15), "Give 10-15 cods or salmon", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/item_example_multi_increase"),
                "Example for an item quest that increases in difficulty the more times you complete it",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.COD))
                .addDescription("This is an example description")
                .addDescription("This is another example description")
                .addTaskEntry("fish", new ItemTask(List.of(
                        DescriptiveValue.of(ItemPredicate.Builder.item().of(Items.COD).build(), "cod")), new QuestNumberProvider.ContextMultiplierNumberProvider(UniformGenerator.between(10, 15), 1, 10), "Give 10-15 * amount of quest completed cods or salmon", true, null)));

        //Kill example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "kill_example"),
                "Example for a kill quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("3d:5h")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("cows", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build()).build(), ConstantValue.exactly(15), "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/kill_example_multi"),
                "Example for a multi kill quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("3d:5h")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("cows", new KillTask(List.of(
                        DescriptiveValue.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build()),
                        DescriptiveValue.of(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW))
                                .located(LocationPredicate.inBiome(Biomes.PLAINS)).build(), "Kill %2$s plains cow")), UniformGenerator.between(5, 8), "Task: 5-8 cows or cows in a plains biome", null)));

        //Location example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "submitting_predicate_example"),
                "Example for a predicate quest. Requires submitting",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.MAP))
                .addTaskEntry("structure", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_warm"))))
                        .build(), "Find a warm ocean ruin").build(), "", true))
                .addTaskEntry("structure2", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_cold"))))
                        .build(), "Find a cold ocean ruin").build(), "", true)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "predicate_example"),
                "Example for a predicate quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.MAP))
                .addTaskEntry("structure", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_warm"))))
                        .build(), "Find a warm ocean ruin").build(), ""))
                .addTaskEntry("structure2", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_cold"))))
                        .build(), "Find a cold ocean ruin").build(), "")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/predicate_example_multi"),
                "Example for a multi predicate quests",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.COMPASS))
                .addTaskEntry("structure", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                                .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE,
                                        new ResourceLocation("ocean_ruin_warm")))).build(), "Go to warm ocean ruin")
                        .add(EntityPredicate.Builder.entity()
                                .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE,
                                        new ResourceLocation("ocean_ruin_cold")))).build(), "Go to cold ocean ruin").build(), "Find a warm or cold ocean ruin")));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "predicate_example_sneak"),
                "Example for a predicate quest using a predicate type. Player needs to sneak additionally",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.MAP))
                .addTaskEntry("structure", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .flags(EntityFlagsPredicate.Builder.flags().setCrouching(true).build())
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_warm"))))
                        .build(), "Find a warm ocean ruin. Sneak when there").build(), ""))
                .addTaskEntry("structure2", new PredicateTask(DescriptiveValue.list(EntityPredicate.Builder.entity()
                        .flags(EntityFlagsPredicate.Builder.flags().setCrouching(true).build())
                        .located(LocationPredicate.inStructure(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation("ocean_ruin_cold"))))
                        .build(), "Find a cold ocean ruin. Sneak when there").build(), "")));

        //XP example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "xp_example"),
                "Example for an xp quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("xp", new XPTask(ConstantValue.exactly(5), "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/xp_example_multi"),
                "Example for an xp with range quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("xp", new XPTask(UniformGenerator.between(5, 10), "Submit xp to this quest", null)));

        // lock Interact example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_interact"),
                "Example for block interaction task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("task", new BlockInteractTask(DescriptiveValue.list(BlockPredicate.Builder.block().of(Blocks.CHEST).build()).build(), List.of(), ConstantValue.exactly(3), true, false, false, "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_break"),
                "Example for block breaking task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("break", new BlockInteractTask(DescriptiveValue.list(BlockPredicate.Builder.block().of(Blocks.DIAMOND_ORE).build()).build(), DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.DIAMOND_PICKAXE).build()).build(), ConstantValue.exactly(3), false, false, false, "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "block_place"),
                "Example of using block interaction as block place detection",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.EMERALD_BLOCK))
                .addTaskEntry("place", new BlockInteractTask(List.of(), DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.EMERALD_BLOCK).build()).build(), ConstantValue.exactly(3), true, false, false, "Place 3 emerald blocks", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/block_place_multi"),
                "Example of using multi block place task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.EMERALD_BLOCK))
                .addTaskEntry("place", new BlockInteractTask(List.of(), DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.EMERALD_BLOCK).build(), "Place Emerald Blocks %2$sx").build(), UniformGenerator.between(3, 5), true, false, false,
                        "Place the blocks from the quest", null)));

        //Crafting example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "crafting_task"),
                "Example of use of a crafting task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.STICK))
                .addTaskEntry("sticks", new CraftingTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.STICK).build()).build(), ConstantValue.exactly(3), "Craft 3 sticks", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/crafting_task_multi"),
                "Example of use of a multi crafting task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.CRAFTING_TABLE))
                .addTaskEntry("crafting", new CraftingTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.STICK).build(), "Craft sticks x2$sx")
                        .add(ItemPredicate.Builder.item().of(Items.FURNACE).build(), "Craft furnaces x2$sx").build(), UniformGenerator.between(3, 5), "Craft some items", null)));

        //Fishing example
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "fishing_task"),
                "Example of use of a fishing task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.FISHING_ROD))
                .addTaskEntry("cod", new FishingTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build()).build(), ConstantValue.exactly(3), "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "multi/fishing_task_multi"),
                "Example of use of a fishing task",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1d")
                .withIcon(new ItemStack(Items.FISHING_ROD))
                .addTaskEntry("fish", new FishingTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build(), "")
                        .add(ItemPredicate.Builder.item().of(Items.SALMON).build(), "").build(), UniformGenerator.between(3, 5), "Fish some fishes", null)));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "daily_quest_item"),
                "Example for an daily item quest",
                new ResourceLocation("chests/buried_treasure"))
                .withIcon(new ItemStack(Items.DIRT))
                .setDailyQuest()
                .addTaskEntry("interact", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build()).build(), ConstantValue.exactly(5), "", true, null)));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/advanced_item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("sword", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.DIAMOND_SWORD)
                        .hasEnchantment(new EnchantmentPredicate(Enchantments.SHARPNESS, MinMaxBounds.Ints.between(2, 3)))
                        .build(), "Diamond Sword (Sharpness 2-3)").build(), ConstantValue.exactly(1), "Give 1 diamond sword with sharp 2 or 3", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/overworld_hostile"),
                "Slay mobs",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("1w")
                .withIcon(new ItemStack(Items.DIAMOND_SWORD))
                .addTaskEntry("zombies", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE)).build()).build(), ConstantValue.exactly(10), "", null))
                .addTaskEntry("zombies_baby", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE))
                        .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true).build()).build(), "Baby Zombie").build(), ConstantValue.exactly(3), "", null))
                .addTaskEntry("skeleton", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SKELETON)).build()).build(), ConstantValue.exactly(10), "", null))
                .addTaskEntry("spiders", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SPIDER)).build()).build(), ConstantValue.exactly(5), "", null))
                .addTaskEntry("xp", new XPTask(ConstantValue.exactly(5), "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/locked_nether_hostile"),
                "Example of a locked quest. Needs to be unlocked (via command) to accept it.",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2w")
                .needsUnlocking()
                .withIcon(new ItemStack(Items.NETHER_BRICK))
                .addTaskEntry("piglin", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIFIED_PIGLIN)).build()).build(), ConstantValue.exactly(15), "", null))
                .addTaskEntry("blaze", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.BLAZE)).build()).build(), ConstantValue.exactly(5), "", null))
                .addTaskEntry("wither_skeleton", new KillTask(DescriptiveValue.list(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.WITHER_SKELETON)).build()).build(), ConstantValue.exactly(5), "", null))
                .addTaskEntry("fortress", new AdvancementTask(DescriptiveValue.list(new ResourceLocation("minecraft:nether/find_fortress")).build(), false, "", null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/child_fishing_quest"),
                "Catch lots of fishes! Needs parent quest completed before doing this quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2h")
                .addParent(new ResourceLocation("example", "item_example"))
                .withIcon(new ItemStack(Items.COD))
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build()).build(), ConstantValue.exactly(15), "Give 15 cods", true, null))
                .addTaskEntry("fish_2", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.SALMON).build()).build(), ConstantValue.exactly(15), "Give 15 salmon", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/advanced_block_break"),
                "Mine a east facing quartz block with an unbreaking diamond pickaxe 3 times",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.DIAMOND_PICKAXE))
                .addTaskEntry("break", new BlockInteractTask(DescriptiveValue.list(BlockPredicate.Builder.block().of(Blocks.QUARTZ_STAIRS)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HorizontalDirectionalBlock.FACING, Direction.EAST).build()).build(), "East Facing Quartz Block").build(),
                        DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.DIAMOND_PICKAXE)
                                .hasEnchantment(new EnchantmentPredicate(Enchantments.UNBREAKING, MinMaxBounds.Ints.atLeast(1))).build(), "Unbreaking 1 Diamond Pickaxe").build(), ConstantValue.exactly(3), false, false, false, "Quartz Stairs", null)));

        // Visibility tests
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "visibility/requirements"),
                "Example for an item quest needing an requirements to accept",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .setVisibility(QuestBase.Visibility.ALWAYS)
                .withUnlockCondition(EntityPredicate.Builder.entity().located(LocationPredicate.Builder.location().setY(MinMaxBounds.Doubles.atLeast(64)).build()).build())
                .addDescription("Requires player to be y > 64")
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build()).build(), ConstantValue.exactly(15), "Give 15 cods", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "visibility/daily_limit"),
                "Example for an item quest completable only once per day",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setMaxDaily(1)
                .withIcon(new ItemStack(Items.DIRT))
                .setVisibility(QuestBase.Visibility.ALWAYS)
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.COD).build()).build(), ConstantValue.exactly(15), "Give 15 cods", true, null)));

        QuestCategory category = new QuestCategory.Builder(new ResourceLocation("example", "category_1"), "Example category 1")
                .withIcon(new ItemStack(Items.DIAMOND_BLOCK)).build();
        QuestCategory category2 = new QuestCategory.Builder(new ResourceLocation("example", "category_2"), "Example category 2")
                .withIcon(new ItemStack(Items.BEACON))
                .setHidden().build();
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_1"),
                "Example for an item quest with category 1",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category)
                .withIcon(new ItemStack(Items.STONE))
                .addTaskEntry("fish", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.STONE).build()).build(), ConstantValue.exactly(15), "Give 15 stone", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_2"),
                "Example for an item quest with category 2",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withCategory(category2)
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.ANDESITE))
                .addTaskEntry("andesite", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.ANDESITE).build()).build(), ConstantValue.exactly(15), "Give 15 andesite", true, null)));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/selection_a"),
                "Selection Quest Example a",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.ANDESITE))
                .addTaskEntry("andesite", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.ANDESITE).build()).build(), ConstantValue.exactly(15), "Give 15 andesite", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/selection_b"),
                "Selection Quest Example b",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.GRANITE))
                .addTaskEntry("granite", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.GRANITE).build()).build(), ConstantValue.exactly(15), "Give 15 granite", true, null)));
        this.addQuest(new CompositeQuest.Builder(new ResourceLocation("example", "selection_quest_example"),
                "Example for a selection quest")
                .setRepeatDelay(36000)
                .withCategory(category)
                .withIcon(new ItemStack(Items.COBBLED_DEEPSLATE))
                .addQuest(new ResourceLocation("example", "hidden/selection_a"))
                .addQuest(new ResourceLocation("example", "hidden/selection_b")));

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/sequential_a"),
                "Sequential Quest Example a",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .addTaskEntry("andesite", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.ANDESITE).build()).build(), ConstantValue.exactly(15), "Give 15 andesite", true, null)));
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "hidden/sequential_b"),
                "Sequential Quest Example b",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .addTaskEntry("granite", new ItemTask(DescriptiveValue.list(ItemPredicate.Builder.item().of(Items.GRANITE).build()).build(), ConstantValue.exactly(15), "Give 15 granite", true, null)));
        this.addQuest(new SequentialQuest.Builder(new ResourceLocation("example", "sequential_quest_example"),
                "Example for a sequential quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category)
                .withIcon(new ItemStack(Items.COBBLED_DEEPSLATE))
                .addQuest(new ResourceLocation("example", "hidden/sequential_a"))
                .addQuest(new ResourceLocation("example", "hidden/sequential_b")));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(super.run(cache),
                DataProvider.saveStable(cache, JsonParser.parseString(PACK_META), this.output.getOutputFolder().resolve("pack.mcmeta")));
    }
}
