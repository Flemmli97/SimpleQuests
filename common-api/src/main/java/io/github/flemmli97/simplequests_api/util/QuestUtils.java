package io.github.flemmli97.simplequests_api.util;

import com.google.gson.JsonSyntaxException;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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

    private static final Pattern DATE_PATTERN = Pattern.compile("(?:(?<weeks>[0-9]{1,2})w)?" +
            "(?:(?:^|:)(?<days>[0-9])d)?" +
            "(?:(?:^|:)(?<hours>[0-9]{1,2})h)?" +
            "(?:(?:^|:)(?<minutes>[0-9]{1,2})m)?" +
            "(?:(?:^|:)(?<seconds>[0-9]{1,2})s)?");

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

    public static Optional<ItemStack> defaultChecked(ItemStack stack, Item defaultValue) {
        if (stack.isEmpty())
            return Optional.empty();
        if (stack.getCount() == 1 && stack.getTag() == null && defaultValue != null && stack.getItem() == defaultValue)
            return Optional.empty();
        return Optional.of(stack);
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
        TextComponent comp = new TextComponent("[#" + tagKey.location() + "]");
        if (tagEntries.isEmpty()) {
            comp.setStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("simplequests_api.empty_tag"))));
        } else if (tagEntries.size() == 1) {
            comp.setStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tagEntries.get(0).withStyle(ChatFormatting.AQUA))));
        } else {
            MutableComponent items = null;
            for (MutableComponent c : tagEntries) {
                if (items == null)
                    items = new TextComponent("[").append(c);
                else
                    items.append(new TextComponent(", ")).append(c);
            }
            items.append("]");
            comp.setStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, items.withStyle(ChatFormatting.AQUA))));
        }
        return comp;
    }
}
