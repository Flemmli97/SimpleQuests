package io.github.flemmli97.simplequests.quest;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;

public class FuzzyQuestEntryImpls {

    public static final Gson GSON = Deserializers.createConditionSerializer().create();

    public static class FuzzyItemEntry implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "fuzzy_item");

        public final List<ItemPredicate> predicate;
        public final NumberProvider amount;
        public final String description, resolvedDescription;
        public final boolean consumeItems;

        public FuzzyItemEntry(List<ItemPredicate> predicate, NumberProvider amount, String description, boolean consumeItems, String resolvedDescription) {
            this.predicate = predicate;
            this.amount = amount;
            this.description = description;
            this.consumeItems = consumeItems;
            this.resolvedDescription = resolvedDescription;
        }

        @Override
        public boolean submit(ServerPlayer player) {
            return false;
        }

        @Override
        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            JsonArray pred = new JsonArray();
            this.predicate.forEach(p -> pred.add(p.serializeToJson()));
            obj.add("predicates", pred);
            if (!this.description.isEmpty())
                obj.addProperty("description", this.description);
            obj.addProperty("amount", GSON.toJson(this.amount));
            obj.addProperty("consumeItems", this.consumeItems);
            if (!this.resolvedDescription.isEmpty())
                obj.addProperty("resolvedDescription", this.resolvedDescription);
            return obj;
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
        public QuestEntry resolve(ServerPlayer player) {
            LootContext ctx = EntityPredicate.createContext(player, player);
            return new QuestEntryImpls.ItemEntry(this.predicate.get(player.getRandom().nextInt(this.predicate.size())),
                    this.amount.getInt(ctx), this.resolvedDescription, this.consumeItems);
        }

        public static FuzzyQuestEntryImpls.FuzzyItemEntry fromJson(JsonObject obj) {
            ImmutableList.Builder<ItemPredicate> builder = ImmutableList.builder();
            JsonArray arr = obj.getAsJsonArray("predicates");
            if (arr.isEmpty())
                throw new IllegalStateException("Predicates can't be empty");
            arr.forEach(e -> builder.add(ItemPredicate.fromJson(e)));
            return new FuzzyQuestEntryImpls.FuzzyItemEntry(builder.build(), GSON.fromJson(obj.get("amount"), NumberProvider.class),
                    GsonHelper.getAsString(obj, "description", ""), GsonHelper.getAsBoolean(obj, "consumeItems", true),
                    GsonHelper.getAsString(obj, "resolvedDescription", ""));
        }
    }
}
