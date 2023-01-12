package io.github.flemmli97.simplequests.forge.test;

import com.google.gson.JsonParser;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.provider.QuestProvider;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import java.io.IOException;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = SimpleQuests.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExampleQuestPackGenerator extends QuestProvider {

    private static final String PACK_META = "{\"pack\": {\"pack_format\": 9,\"description\": [{\"text\":\"Example Quests\",\"color\":\"gold\"}]}}";

    public ExampleQuestPackGenerator(DataGenerator gen, boolean full) {
        super(gen, full);
    }

    @SubscribeEvent
    public static void data(GatherDataEvent event) {
        DataGenerator data = event.getGenerator();
        if (event.includeServer()) {
            data.addProvider(new ExampleQuestPackGenerator(data, true));
        }
    }

    @Override
    protected void add() {
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advancement_example"),
                "Example for an advancement quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.EMERALD, 5))
                .addTaskEntry("trade", new QuestEntryImpls.AdvancementEntry(new ResourceLocation("minecraft:adventure/trade"), true))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "interact_example"),
                "Example for an interaction quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("interact", new QuestEntryImpls.EntityInteractEntry(ItemPredicate.Builder.item().of(Items.NAME_TAG).build(), EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.CHICKEN)).build(), 2, true, "Use nametag on 2 chickens"))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "kill_example"),
                "Example for a kill quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("3d:5h")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("cows", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.COW)).build(), 15))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "location_example"),
                "Example for a location quest using a location predicate",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withSortingNum(1)
                .withIcon(new ItemStack(Items.MAP))
                .addTaskEntry("structure", new QuestEntryImpls.LocationEntry(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_warm"))), "Find a warm ocean ruin"))
                .addTaskEntry("structure2", new QuestEntryImpls.LocationEntry(LocationPredicate.inFeature(ResourceKey.create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, new ResourceLocation("ocean_ruin_cold"))), "Find a cold ocean ruin"))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "position_example"),
                "Example for a simple position quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("place", new QuestEntryImpls.PositionEntry(new BlockPos(0, 50, 0), 15))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "xp_example"),
                "Example for an xp quest",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay("1w:5d:3h:2m")
                .withIcon(new ItemStack(Items.PURPUR_BLOCK))
                .addTaskEntry("xp", new QuestEntryImpls.XPEntry(5))
                .build());

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "daily_quest_item"),
                "Example for an daily item quest",
                new ResourceLocation("chests/buried_treasure"))
                .withIcon(new ItemStack(Items.DIRT))
                .setDailyQuest()
                .addTaskEntry("interact", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 5, "Give 5 cods", true))
                .build());

        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/advanced_item_example"),
                "Example for an item quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay(36000)
                .withIcon(new ItemStack(Items.DIRT))
                .addTaskEntry("sword", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.DIAMOND_SWORD)
                        .hasEnchantment(new EnchantmentPredicate(Enchantments.SHARPNESS, MinMaxBounds.Ints.between(2, 3)))
                        .build(), 1, "Give 1 diamond sword with sharp 2 or 3", true))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/overworld_hostile"),
                "Slay mobs",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("1w")
                .withIcon(new ItemStack(Items.DIAMOND_SWORD))
                .addTaskEntry("zombies", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE)).build(), 10))
                .addTaskEntry("zombies_baby", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIE))
                        .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true).build()).build(), 3))
                .addTaskEntry("skeleton", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SKELETON)).build(), 10))
                .addTaskEntry("spiders", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.SPIDER)).build(), 5))
                .addTaskEntry("xp", new QuestEntryImpls.XPEntry(5))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/locked_nether_hostile"),
                "Example of a locked quest. Needs to be unlocked (via command) to accept it.",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2w")
                .needsUnlocking()
                .withIcon(new ItemStack(Items.NETHER_BRICK))
                .addTaskEntry("piglin", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.ZOMBIFIED_PIGLIN)).build(), 15))
                .addTaskEntry("blaze", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.BLAZE)).build(), 5))
                .addTaskEntry("wither_skeleton", new QuestEntryImpls.KillEntry(EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(EntityType.WITHER_SKELETON)).build(), 5))
                .addTaskEntry("fortress", new QuestEntryImpls.AdvancementEntry(new ResourceLocation("minecraft:nether/find_fortress"), false))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "advanced/child_fishing_quest"),
                "Catch lots of fishes! Needs parent quest completed before doing this quest",
                new ResourceLocation("chests/end_city_treasure"))
                .setRepeatDelay("2h")
                .addParent(new ResourceLocation("example", "item_example"))
                .withIcon(new ItemStack(Items.COD))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.COD).build(), 15, "Give 15 cods", true))
                .addTaskEntry("fish_2", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.SALMON).build(), 15, "Give 15 salmon", true))
                .build());

        QuestCategory category = new QuestCategory.Builder(new ResourceLocation("example", "category_1"), "Example category 1")
                .withIcon(new ItemStack(Items.DIAMOND_BLOCK)).build();
        QuestCategory category2 = new QuestCategory.Builder(new ResourceLocation("example", "category_2"), "Example category 2")
                .withIcon(new ItemStack(Items.BEACON))
                .unselectable().build();
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_1"),
                "Example for an item quest with category 1",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category)
                .withIcon(new ItemStack(Items.STONE))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.STONE).build(), 15, "Give 15 stone", true))
                .build());
        this.addQuest(new Quest.Builder(new ResourceLocation("example", "item_example_category_2"),
                "Example for an item quest with category 2",
                new ResourceLocation("chests/abandoned_mineshaft"))
                .setRepeatDelay(36000)
                .withCategory(category2)
                .withIcon(new ItemStack(Items.ANDESITE))
                .addTaskEntry("fish", new QuestEntryImpls.ItemEntry(ItemPredicate.Builder.item().of(Items.ANDESITE).build(), 15, "Give 15 andesite", true))
                .build());
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
