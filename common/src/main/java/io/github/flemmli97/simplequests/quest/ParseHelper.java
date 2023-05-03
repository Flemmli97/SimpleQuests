package io.github.flemmli97.simplequests.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.flemmli97.simplequests.SimpleQuests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseHelper {

    private static final Codec<ItemStack> STACK_CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemStack::getItem),
                            Codec.INT.optionalFieldOf("count").forGetter((itemStack) -> Optional.of(itemStack.getCount())),
                            Codec.STRING.optionalFieldOf("tag").forGetter((itemStack) -> Optional.ofNullable(itemStack.getTag()).map(CompoundTag::toString)))
                    .apply(instance, (item, count, tag) -> {
                        CompoundTag nbt = tag.map(sTag -> {
                            try {
                                return TagParser.parseTag(sTag);
                            } catch (CommandSyntaxException e) {
                                return null;
                            }
                        }).orElse(null);
                        ItemStack stack = new ItemStack(item, count.orElse(1));
                        stack.setTag(nbt);
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
                .resultOrPartial(SimpleQuests.logger::error).orElse(ItemStack.EMPTY);
        if (result.isEmpty())
            return new ItemStack(fallback);
        return result;
    }

    public static Optional<JsonElement> writeItemStackToJson(ItemStack stack, Item defaultValue) {
        if (stack.getCount() == 1 && !stack.hasTag())
            return defaultValue != null && stack.getItem() == defaultValue ? Optional.empty() : Optional.of(new JsonPrimitive(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        return STACK_CODEC.encodeStart(JsonOps.INSTANCE, stack).resultOrPartial(SimpleQuests.logger::error);
    }
}
