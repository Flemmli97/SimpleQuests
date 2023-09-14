package io.github.flemmli97.simplequests.quest;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests.JsonCodecs;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.api.QuestEntry;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Optional;

public class FuzzyQuestEntryImpls {

    public static final Gson GSON = Deserializers.createConditionSerializer().create();

    public record FuzzyItemEntry(List<ItemPredicate> predicate,
                                 NumberProvider amount,
                                 String description, boolean consumeItems,
                                 String resolvedDescription) implements QuestEntry {

        public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "fuzzy_item");
        public static final Codec<FuzzyItemEntry> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(Codec.BOOL.fieldOf("consumeItems").forGetter(d -> d.consumeItems),
                        Codec.STRING.optionalFieldOf("resolvedDescription").forGetter(d -> d.resolvedDescription.isEmpty() ? Optional.empty() : Optional.of(d.resolvedDescription)),

                        JsonCodecs.ITEM_PREDICATE_CODEC.listOf().fieldOf("predicates").forGetter(d -> d.predicate),
                        JsonCodecs.jsonCodecBuilder(GSON::toJsonTree, e -> GSON.fromJson(e, NumberProvider.class), "NumberProvider").fieldOf("amount").forGetter(d -> d.amount),
                        Codec.STRING.optionalFieldOf("description").forGetter(d -> d.description.isEmpty() ? Optional.empty() : Optional.of(d.description))
                ).apply(instance, (consume, resolvedDescription, pred, amount, desc) -> {
                    if (pred.isEmpty())
                        throw new IllegalStateException("Predicates can't be empty");
                    return new FuzzyItemEntry(pred, amount, desc.orElse(""), consume, resolvedDescription.orElse(""));
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

        @Override
        public QuestEntry resolve(ServerPlayer player) {
            LootContext ctx = EntityPredicate.createContext(player, player);
            return new QuestEntryImpls.ItemEntry(this.predicate.get(player.getRandom().nextInt(this.predicate.size())),
                    this.amount.getInt(ctx), this.resolvedDescription, this.consumeItems);
        }
    }
}
