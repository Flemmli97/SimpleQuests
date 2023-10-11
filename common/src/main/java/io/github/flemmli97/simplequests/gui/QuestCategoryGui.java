package io.github.flemmli97.simplequests.gui;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.inv.SeparateInv;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestCategoryGui extends ServerOnlyScreenHandler<Object> {

    public static int ENTRY_PER_PAGE = 12;

    private int page, maxPages;
    private List<ResourceLocation> categories;
    private final ServerPlayer player;

    protected QuestCategoryGui(int syncId, Inventory playerInventory) {
        super(syncId, playerInventory, 6, null);
        if (playerInventory.player instanceof ServerPlayer)
            this.player = (ServerPlayer) playerInventory.player;
        else
            throw new IllegalStateException("This is a server side container");
    }

    public static void openGui(Player player) {
        MenuProvider fac = new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new QuestCategoryGui(syncId, inv);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable(ConfigHandler.LANG.get("simplequests.gui.main"));
            }
        };
        player.openMenu(fac);
    }

    private ItemStack ofCategory(int i, QuestCategory category, ServerPlayer player) {
        ItemStack stack = category.getIcon();
        stack.setHoverName(category.getName().setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GOLD)));
        ListTag lore = new ListTag();
        for (String comp : category.description)
            lore.add(StringTag.valueOf(comp));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        stack.getOrCreateTagElement("SimpleQuests").putString("QuestCategory", category.id.toString());
        return stack;
    }

    public static ItemStack emptyFiller() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(Component.literal(""));
        return stack;
    }

    private static void playSongToPlayer(ServerPlayer player, Holder<SoundEvent> event, float vol, float pitch) {
        player.connection.send(
                new ClientboundSoundPacket(event, SoundSource.PLAYERS, player.position().x, player.position().y, player.position().z, vol, pitch, player.getRandom().nextLong()));
    }

    @Override
    protected void fillInventoryWith(Player player, SeparateInv inv, Object additionalData) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        Map<ResourceLocation, QuestCategory> categoryMap = QuestsManager.instance().getSelectableCategories();
        this.categories = new ArrayList<>(categoryMap.keySet());
        this.categories.removeIf(res -> QuestsManager.instance().getQuestsForCategory(categoryMap.get(res)).isEmpty());
        this.maxPages = (this.categories.size() - 1) / ENTRY_PER_PAGE;
        int id = 0;
        for (int i = 0; i < 54; i++) {
            if (i == 8 && this.categories.size() > ENTRY_PER_PAGE) {
                ItemStack close = new ItemStack(Items.ARROW);
                close.setHoverName(Component.translatable(ConfigHandler.LANG.get("simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                inv.updateStack(i, close);
            } else if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8)
                inv.updateStack(i, emptyFiller());
            else if (i % 9 == 1 || i % 9 == 4 || i % 9 == 7) {
                if (id < this.categories.size()) {
                    ItemStack stack = this.ofCategory(i, categoryMap.get(this.categories.get(id)), serverPlayer);
                    if (!stack.isEmpty()) {
                        inv.updateStack(i, this.ofCategory(i, categoryMap.get(this.categories.get(id)), serverPlayer));
                        id++;
                    }
                }
            }
        }
    }

    private void flipPage() {
        Map<ResourceLocation, QuestCategory> categoryMap = QuestsManager.instance().getSelectableCategories();
        int id = this.page * ENTRY_PER_PAGE;
        for (int i = 0; i < 54; i++) {
            if (i == 0) {
                ItemStack stack = emptyFiller();
                if (this.page > 0) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(Component.translatable(ConfigHandler.LANG.get("simplequests.gui.previous")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (i == 8) {
                ItemStack stack = emptyFiller();
                if (this.page < this.maxPages) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(Component.translatable(ConfigHandler.LANG.get("simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8)
                this.slots.get(i).set(emptyFiller());
            else if (i % 9 == 1 || i % 9 == 4 || i % 9 == 7) {
                if (id < this.categories.size()) {
                    this.slots.get(i).set(this.ofCategory(i, categoryMap.get(this.categories.get(id)), this.player));
                    id++;
                } else
                    this.slots.get(i).set(ItemStack.EMPTY);
            }
        }
        this.broadcastChanges();
    }

    @Override
    protected boolean handleSlotClicked(ServerPlayer player, int index, Slot slot, int clickType) {
        if (index == 0) {
            this.page--;
            this.flipPage();
            playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        if (index == 8) {
            this.page++;
            this.flipPage();
            playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        ItemStack stack = slot.getItem();
        if (!stack.hasTag())
            return false;
        CompoundTag tag = stack.getTag().getCompound("SimpleQuests");
        if (!tag.contains("QuestCategory"))
            return false;
        ResourceLocation id = new ResourceLocation(tag.getString("QuestCategory"));
        QuestCategory category = QuestsManager.instance().getQuestCategory(id);
        if (category == null) {
            SimpleQuests.LOGGER.error("No such category " + id);
            return false;
        }
        player.closeContainer();
        player.getServer().execute(() -> QuestGui.openGui(player, category));
        return true;
    }

    @Override
    protected boolean isRightSlot(int slot) {
        return (this.page > 0 && slot == 0) || (this.page < this.maxPages && slot == 8) || (slot < 45 && slot > 8 && (slot % 9 == 1 || slot % 9 == 4 || slot % 9 == 7));
    }
}
