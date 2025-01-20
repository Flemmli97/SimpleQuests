package io.github.flemmli97.simplequests_api;

import io.github.flemmli97.simplequests_api.impls.entries.single.ItemEntry;
import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleQuestsAPI {

    public static final String MODID = "simplequests_api";

    public static final Logger LOGGER = LogManager.getLogger("simplequests_api");

    public static GuiWrapper GUI_WRAPPER;

    public static LootContext createContext(PlayerQuestData data, @Nullable ResourceLocation quest) {
        ServerPlayer player = data.getPlayer();
        return new LootContext.Builder(player.getLevel()).withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position()).withRandom(data.getRandom(quest))
                .create(LootContextParamSets.ADVANCEMENT_ENTITY);
    }

    private static final int WRAP_AMOUNT = 4;

    public static List<MutableComponent> wrapForGui(ServerPlayer player, ItemEntry entry) {
        if (!entry.description().isEmpty())
            return List.of(new TranslatableComponent(entry.description()));
        List<MutableComponent> all = ItemEntry.itemComponents(entry.predicate());
        if (all.size() < WRAP_AMOUNT || GUI_WRAPPER == null || !GUI_WRAPPER.shouldWrap(player, entry))
            return List.of(entry.translation(player));
        List<MutableComponent> list = new ArrayList<>();
        MutableComponent items = null;
        int i = 0;
        for (MutableComponent comp : all) {
            if (items == null) {
                if (list.isEmpty())
                    items = new TextComponent("[").append(comp);
                else
                    items = comp;
            } else
                items.append(new TextComponent(", ")).append(comp);
            i++;
            if ((list.isEmpty() && i >= WRAP_AMOUNT - 1) || i >= WRAP_AMOUNT) {
                if (list.isEmpty()) {
                    list.add(new TranslatableComponent(entry.getId().toString() + ".multi", items.withStyle(ChatFormatting.AQUA), entry.amount()));
                } else
                    list.add(items.withStyle(ChatFormatting.AQUA));
                i = 0;
                items = null;
            }
        }
        list.get(list.size() - 1).append(new TextComponent("]"));
        return list;
    }

    public interface GuiWrapper {
        boolean shouldWrap(ServerPlayer player, ItemEntry entry);
    }
}
