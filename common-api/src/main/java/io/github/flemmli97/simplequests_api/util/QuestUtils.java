package io.github.flemmli97.simplequests_api.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestUtils {

    private static final Codec<ItemStack> STACK_CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemStack::getItem),
                            Codec.INT.optionalFieldOf("count").forGetter((itemStack) -> Optional.of(itemStack.getCount())),
                            CompoundTag.CODEC.optionalFieldOf("tag").forGetter((itemStack) -> Optional.ofNullable(itemStack.getTag())))
                    .apply(instance, (item, count, tag) -> {
                        ItemStack stack = new ItemStack(item, count.orElse(1));
                        stack.setTag(tag.orElse(null));
                        return stack;
                    }));

    private static final Pattern DATE_PATTERN = Pattern.compile("(?:(?<weeks>[0-9]{1,2})w)?" +
            "(?:(?:^|:)(?<days>[0-9])d)?" +
            "(?:(?:^|:)(?<hours>[0-9]{1,2})h)?" +
            "(?:(?:^|:)(?<minutes>[0-9]{1,2})m)?" +
            "(?:(?:^|:)(?<seconds>[0-9]{1,2})s)?");

    public static int tryParseTime(JsonObject obj, String name, int fallback) {
        JsonElement e = obj.get(name);
        if (e == null || !e.isJsonPrimitive())
            return fallback;
        if (e.getAsJsonPrimitive().isNumber())
            return e.getAsInt();
        return tryParseTime(e.getAsString(), name);
    }

    public static int tryParseTime(String time, String id) {
        Matcher matcher = DATE_PATTERN.matcher(time);
        if (!matcher.matches()) {
            throw new JsonSyntaxException("Malformed date time for " + id + ".");
        }
        int ticks = 0;
        ticks += asTicks(matcher, "weeks", 12096000);
        ticks += asTicks(matcher, "days", 1728000);
        ticks += asTicks(matcher, "hours", 72000);
        ticks += asTicks(matcher, "minutes", 1200);
        ticks += asTicks(matcher, "seconds", 20);
        return ticks;
    }

    private static int asTicks(Matcher matcher, String group, int multiplier) {
        String val = matcher.group(group);
        if (val != null) {
            try {
                return Integer.parseInt(val) * multiplier;
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public static ItemStack icon(JsonObject obj, String name, Item fallback) {
        JsonElement element = obj.get(name);
        if (element == null)
            return new ItemStack(fallback);
        if (element.isJsonPrimitive()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(element.getAsString())));
            if (stack.isEmpty())
                return new ItemStack(fallback);
            return stack;
        }
        ItemStack result = STACK_CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(SimpleQuestsAPI.LOGGER::error).orElse(ItemStack.EMPTY);
        if (result.isEmpty())
            return new ItemStack(fallback);
        return result;
    }

    public static Optional<JsonElement> writeItemStackToJson(ItemStack stack, Item defaultValue) {
        if (stack.getCount() == 1 && !stack.hasTag())
            return defaultValue != null && stack.getItem() == defaultValue ? Optional.empty() : Optional.of(new JsonPrimitive(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        return STACK_CODEC.encodeStart(JsonOps.INSTANCE, stack).resultOrPartial(SimpleQuestsAPI.LOGGER::error);
    }

    public static int getAmount(NumberProvider provider, LootContext ctx, PlayerQuestData data, ResourceLocation quest) {
        if (!(provider instanceof QuestNumberProvider.ContextMultiplierNumberProvider mult))
            return provider.getInt(ctx);
        return Math.round(mult.getFloatWith(ctx, () -> (float) data.getTimesCompleted(quest)));
    }

    public static Optional<String> optStr(String s) {
        return s == null || s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    public static <T> MutableComponent tagsComponent(TagKey<T> tagKey, Registry<T> registry, Function<T, MutableComponent> translation) {
        List<MutableComponent> tagEntries = new ArrayList<>();
        registry.getTag(tagKey).ifPresent(n -> n.forEach(h -> tagEntries.add(translation.apply(h.value()))));
        if (tagEntries.isEmpty()) {
            return Component.translatable("simplequest_api.empty_tag");
        }
        MutableComponent comp =  Component.literal("[#" + tagKey.location() + "]");
        if (tagEntries.size() == 1) {
            comp.setStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tagEntries.get(0).withStyle(ChatFormatting.AQUA))));
        } else {
            MutableComponent items = null;
            for (MutableComponent c : tagEntries) {
                if (items == null)
                    items =  Component.literal("[").append(c);
                else
                    items.append( ", ").append(c);
            }
            items.append("]");
            comp.setStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, items.withStyle(ChatFormatting.AQUA))));
        }
        return comp;
    }
}
