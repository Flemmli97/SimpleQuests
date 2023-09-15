package io.github.flemmli97.simplequests.gui;

import com.mojang.datafixers.util.Pair;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.inv.SeparateInv;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.quest.CompositeQuest;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompositeQuestScreenHandler extends ServerOnlyScreenHandler<CompositeQuestScreenHandler.GuiData> {

    private List<ResourceLocation> quests;
    private final CompositeQuest quest;
    private final QuestCategory category;
    private final boolean canGoBack;
    private int page;

    private CompositeQuestScreenHandler(int syncId, Inventory playerInventory, CompositeQuest quest, QuestCategory category, boolean canGoBack, int page) {
        super(syncId, playerInventory, (quest.getCompositeQuests().size() / 7) + 1, new GuiData(quest, category, (quest.getCompositeQuests().size() / 7) + 1, page, canGoBack));
        this.quest = quest;
        this.category = category;
        this.canGoBack = canGoBack;
        this.page = page;
    }

    public static void openScreen(ServerPlayer player, CompositeQuest quest, QuestCategory category, boolean canGoBack, int page) {
        MenuProvider fac = new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new CompositeQuestScreenHandler(syncId, inv, quest, category, canGoBack, page);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable(ConfigHandler.lang.get("simplequests.gui.composite.quest"));
            }
        };
        player.openMenu(fac);
    }

    private ItemStack ofQuest(Quest quest, ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        ItemStack stack = quest.getIcon();
        stack.setHoverName(quest.getTask().setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GOLD)));
        ListTag lore = new ListTag();
        quest.getDescription().forEach(c -> lore.add(StringTag.valueOf(Component.Serializer.toJson(c.setStyle(c.getStyle().withItalic(false))))));
        if (data.isActive(quest)) {
            stack.enchant(Enchantments.UNBREAKING, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
        }
        for (MutableComponent comp : quest.getFormattedGuiTasks(player))
            lore.add(StringTag.valueOf(Component.Serializer.toJson(comp.setStyle(comp.getStyle().withItalic(false)))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        stack.getOrCreateTagElement("SimpleQuests").putString("Quest", quest.id.toString());
        return stack;
    }

    @Override
    protected void fillInventoryWith(Player player, SeparateInv inv, GuiData additionalData) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        Map<ResourceLocation, Quest> questMap = additionalData.quest.getCompositeQuests()
                .stream().map(r -> Pair.of(r, QuestsManager.instance().getAllQuests().get(r)))
                .filter(p -> p.getSecond() instanceof Quest).collect(Collectors.toMap(
                        Pair::getFirst,
                        e -> (Quest) e.getSecond(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        this.quests = new ArrayList<>(questMap.keySet());
        int id = 0;
        for (int i = 0; i < additionalData.rows * 9; i++) {
            int mod = i % 9;
            if ((additionalData.rows > 2 && (i < 9 || i > this.size - 1)) || mod == 0 || mod == 8)
                inv.updateStack(i, QuestGui.emptyFiller());
            else {
                if (id < this.quests.size()) {
                    ItemStack stack = this.ofQuest(questMap.get(this.quests.get(id)), serverPlayer);
                    if (!stack.isEmpty()) {
                        inv.updateStack(i, this.ofQuest(questMap.get(this.quests.get(id)), serverPlayer));
                        id++;
                    }
                }
            }
        }
    }

    @Override
    protected boolean isRightSlot(int slot) {
        int mod = slot % 9;
        return mod >= 1 && mod <= 7;
    }

    @Override
    protected boolean handleSlotClicked(ServerPlayer player, int index, Slot slot, int clickType) {
        ItemStack stack = slot.getItem();
        if (!stack.hasTag())
            return false;
        CompoundTag tag = stack.getTag().getCompound("SimpleQuests");
        if (!tag.contains("Quest"))
            return false;
        if (stack.getItem() == Items.BOOK) {
            QuestGui.playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            return false;
        }
        ResourceLocation id = new ResourceLocation(tag.getString("Quest"));
        Quest actual = QuestsManager.instance().getActualQuests(id);
        if (actual == null) {
            SimpleQuests.logger.error("No such quest " + id);
            return false;
        }
        ConfirmScreenHandler.openConfirmScreen(player, b -> {
            if (b) {
                player.closeContainer();
                if (PlayerData.get(player).acceptQuest(actual, this.quest))
                    QuestGui.playSongToPlayer(player, SoundEvents.NOTE_BLOCK_PLING, 1, 1.2f);
                else
                    QuestGui.playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            } else {
                player.closeContainer();
                player.getServer().execute(() -> QuestGui.openGui(player, this.category, this.canGoBack, this.page));
                QuestGui.playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            }
        }, "simplequests.gui.confirm");
        return true;
    }

    record GuiData(CompositeQuest quest, QuestCategory category, int rows, int page, boolean canGoBack) {
    }
}
