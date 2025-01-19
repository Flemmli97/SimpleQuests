package io.github.flemmli97.simplequests.gui;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.inv.SeparateInv;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import io.github.flemmli97.simplequests.quest.types.CompositeQuest;
import io.github.flemmli97.simplequests.quest.types.Quest;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                return Component.translatable("simplequests.gui.composite.quest");
            }
        };
        player.openMenu(fac);
    }

    private ItemStack ofQuest(Quest quest, int idx, ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        ItemStack stack = quest.getIcon();
        stack.set(DataComponents.CUSTOM_NAME, quest.getTask(player).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GOLD)));
        List<Component> lore = new ArrayList<>();
        quest.getDescription(player).forEach(c -> lore.add(c.setStyle(c.getStyle().withItalic(false))));
        if (data.isActive(quest)) {
            stack.enchant(player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING), 1);
            if (stack.has(DataComponents.STORED_ENCHANTMENTS))
                stack.set(DataComponents.STORED_ENCHANTMENTS, stack.get(DataComponents.STORED_ENCHANTMENTS).withTooltip(false));
            else if (stack.has(DataComponents.ENCHANTMENTS))
                stack.set(DataComponents.ENCHANTMENTS, stack.get(DataComponents.ENCHANTMENTS).withTooltip(false));
        }
        for (MutableComponent comp : quest.getFormattedGuiTasks(player))
            lore.add(comp.setStyle(comp.getStyle().withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(QuestGui.STACK_NBT_ID, idx));
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
            if (i == 0) {
                ItemStack stack = new ItemStack(Items.ARROW);
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("simplequests.gui.button.main").setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                inv.updateStack(i, stack);
            } else if ((additionalData.rows > 2 && (i < 9 || i > this.size - 1)) || mod == 0 || mod == 8)
                inv.updateStack(i, QuestGui.emptyFiller());
            else {
                if (id < this.quests.size()) {
                    ItemStack stack = this.ofQuest(questMap.get(this.quests.get(id)), id, serverPlayer);
                    if (!stack.isEmpty()) {
                        inv.updateStack(i, stack);
                        id++;
                    }
                }
            }
        }
    }

    @Override
    protected boolean isRightSlot(int slot) {
        int mod = slot % 9;
        return slot == 0 || (mod >= 1 && mod <= 7);
    }

    @Override
    protected boolean handleSlotClicked(ServerPlayer player, int index, Slot slot, int clickType) {
        if (index == 0) {
            player.closeContainer();
            player.getServer().execute(() -> QuestGui.openGui(player, this.category, this.canGoBack, this.page));
            QuestGui.playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        ItemStack stack = slot.getItem();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null)
            return false;
        if (stack.getItem() == Items.BOOK) {
            QuestGui.playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            return false;
        }
        Optional<Integer> opt = customData.read(Codec.INT.fieldOf(QuestGui.STACK_NBT_ID)).result();
        if (opt.isEmpty())
            return false;
        int idx = opt.get();
        QuestBase actual = this.quest.resolveToQuest(player, idx);
        if (actual == null) {
            SimpleQuests.LOGGER.error("No such quest for composite " + this.quest.id);
            return false;
        }
        ConfirmScreenHandler.openConfirmScreen(player, b -> {
            if (b) {
                player.closeContainer();
                if (PlayerData.get(player).acceptQuest(this.quest, idx))
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
