package io.github.flemmli97.simplequests.quest;

import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.mixin.EntityPredicateAccessor;
import io.github.flemmli97.simplequests.mixin.ItemPredicateAccessor;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class QuestEntryImpls {

    public static class ItemEntry implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "item");

        public final ItemPredicate predicate;
        public final int amount;
        public final MutableComponent description;
        public final boolean consumeItems;

        public ItemEntry(ItemPredicate predicate, int amount, String description, boolean consumeItems) {
            this.predicate = predicate;
            this.amount = amount;
            this.description = description.isEmpty() ? null : Component.literal(description);
            this.consumeItems = consumeItems;
        }

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
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("predicate", this.predicate.serializeToJson());
            obj.addProperty("description", this.description.getString());
            obj.addProperty("amount", this.amount);
            obj.addProperty("consumeItems", this.consumeItems);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            if (this.description != null)
                return this.description;
            List<MutableComponent> formattedItems = itemComponents(this.predicate);
            if (formattedItems.isEmpty())
                return Component.literal(ConfigHandler.lang.get(this.getId().toString() + ".empty"));
            if (formattedItems.size() == 1) {
                return Component.translatable(ConfigHandler.lang.get(this.getId().toString() + ".single" + (this.consumeItems ? "" : ".keep")), formattedItems.get(0).withStyle(ChatFormatting.AQUA), this.amount);
            }
            MutableComponent items = null;
            for (MutableComponent c : formattedItems) {
                if (items == null)
                    items = Component.literal("[").append(c);
                else
                    items.append(Component.literal(", ")).append(c);
            }
            items.append("]");
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString() + ".multi" + (this.consumeItems ? "" : ".keep")), items.withStyle(ChatFormatting.AQUA), this.amount);
        }

        public static ItemEntry fromJson(JsonObject obj) {
            return new ItemEntry(ItemPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "predicate")), obj.get("amount").getAsInt(),
                    GsonHelper.getAsString(obj, "description", ""), GsonHelper.getAsBoolean(obj, "consumeItems", true));
        }

        public static List<MutableComponent> itemComponents(ItemPredicate predicate) {
            ItemPredicateAccessor acc = (ItemPredicateAccessor) predicate;
            List<MutableComponent> formattedItems = new ArrayList<>();
            if (acc.getItems() != null)
                acc.getItems().forEach(i -> formattedItems.add(Component.translatable(i.getDescriptionId())));
            if (acc.getTag() != null)
                Registry.ITEM.getTag(acc.getTag()).ifPresent(n -> n.forEach(h -> formattedItems.add(Component.translatable(h.value().getDescriptionId()))));
            return formattedItems;
        }
    }

    public record KillEntry(EntityPredicate predicate, int amount) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "entity");

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("predicate", this.predicate.serializeToJson());
            obj.addProperty("amount", this.amount);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            EntityPredicateAccessor acc = (EntityPredicateAccessor) this.predicate;
            String s = acc.getEntityType().serializeToJson().getAsString();
            if (s.startsWith("#")) {
                return Component.translatable(ConfigHandler.lang.get(this.getId().toString() + ".tag"), Component.literal(s).withStyle(ChatFormatting.AQUA), this.amount);
            }
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString()), Component.translatable(Util.makeDescriptionId("entity", new ResourceLocation(s))).withStyle(ChatFormatting.AQUA), this.amount);
        }

        public static KillEntry fromJson(JsonObject obj) {
            return new KillEntry(EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "predicate")), GsonHelper.getAsInt(obj, "amount", 1));
        }
    }

    public record XPEntry(int amount) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "xp");

        @Override
        public boolean submit(ServerPlayer player) {
            if (player.experienceLevel >= this.amount) {
                player.giveExperienceLevels(-this.amount);
                return true;
            }
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("amount", this.amount);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            return Component.literal(String.format(ConfigHandler.lang.get(this.getId().toString()), this.amount));
        }

        public static XPEntry fromJson(JsonObject obj) {
            return new XPEntry(GsonHelper.getAsInt(obj, "amount", 1));
        }
    }

    public record AdvancementEntry(ResourceLocation advancement, boolean reset) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "advancement");

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
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("advancement", this.advancement.toString());
            obj.addProperty("reset", this.reset);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            Advancement advancement = server.getAdvancements().getAdvancement(this.advancement());
            Component adv;
            if (advancement == null)
                adv = Component.literal(String.format(ConfigHandler.lang.get("simplequests.missing.advancement"), this.advancement()));
            else
                adv = advancement.getChatComponent();
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString()), adv);
        }

        public static AdvancementEntry fromJson(JsonObject obj) {
            return new AdvancementEntry(new ResourceLocation(GsonHelper.getAsString(obj, "advancement")), GsonHelper.getAsBoolean(obj, "reset", false));
        }
    }

    public record PositionEntry(BlockPos pos, int minDist) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "position");

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", this.pos.getX());
            obj.addProperty("y", this.pos.getY());
            obj.addProperty("z", this.pos.getZ());
            obj.addProperty("minDist", this.minDist);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString()), this.pos.getX(), this.pos.getY(), this.pos.getZ());
        }

        @Override
        public Function<PlayerData, Boolean> tickable() {
            return d -> {
                ServerPlayer p = d.getPlayer();
                return p.tickCount % 20 == 0 && p.blockPosition().distSqr(this.pos) < this.minDist * this.minDist;
            };
        }

        public static PositionEntry fromJson(JsonObject obj) {
            return new PositionEntry(new BlockPos(GsonHelper.getAsInt(obj, "x"), GsonHelper.getAsInt(obj, "y"), GsonHelper.getAsInt(obj, "z")),
                    GsonHelper.getAsInt(obj, "minDist"));
        }
    }

    /**
     * Quest entry to check if a player matches a given location.
     *
     * @param location    The LocationPredicate to check
     * @param description Parsing a location predicate is way too complicated. Its easier instead to have the datapack maker provide a description instead
     */
    public record LocationEntry(LocationPredicate location, String description) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "location");

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("predicate", this.location.serializeToJson());
            obj.addProperty("description", this.description);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString()), this.description);
        }

        @Override
        public Function<PlayerData, Boolean> tickable() {
            return d -> {
                ServerPlayer p = d.getPlayer();
                return p.tickCount % 20 == 0 && this.location.matches(p.getLevel(), p.getX(), p.getY(), p.getZ());
            };
        }

        public static LocationEntry fromJson(JsonObject obj) {
            return new LocationEntry(LocationPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "predicate")), GsonHelper.getAsString(obj, "description"));
        }
    }

    /**
     * Quest entry to check if a player matches a given location.
     *
     * @param description Parsing a the predicates is way too complicated. Its easier instead to have the datapack maker provide a description instead
     */
    public record EntityInteractEntry(ItemPredicate heldItem, EntityPredicate entityPredicate, int amount,
                                      boolean consume,
                                      String description) implements QuestEntry {

        public static final ResourceLocation id = new ResourceLocation(SimpleQuests.MODID, "entity_interact");

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.add("item", this.heldItem.serializeToJson());
            obj.add("predicate", this.entityPredicate.serializeToJson());
            obj.addProperty("amount", this.amount);
            obj.addProperty("consume", this.consume);
            obj.addProperty("description", this.description);
            return obj;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public MutableComponent translation(MinecraftServer server) {
            return Component.translatable(ConfigHandler.lang.get(this.getId().toString()), this.description);
        }

        public boolean check(ServerPlayer player, Entity entity) {
            boolean b = this.heldItem.matches(player.getMainHandItem()) && this.entityPredicate.matches(player, entity);
            if (b && this.consume && !player.isCreative()) {
                player.getMainHandItem().shrink(1);
            }
            return b;
        }

        public static EntityInteractEntry fromJson(JsonObject obj) {
            return new EntityInteractEntry(ItemPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "item", null)),
                    EntityPredicate.fromJson(GsonHelper.getAsJsonObject(obj, "predicate")),
                    GsonHelper.getAsInt(obj, "amount", 1),
                    GsonHelper.getAsBoolean(obj, "consume", false),
                    GsonHelper.getAsString(obj, "description"));
        }
    }

}
