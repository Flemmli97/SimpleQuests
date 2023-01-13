package io.github.flemmli97.simplequests.gui;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.inv.SeparateInv;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestGui extends ServerOnlyScreenHandler<QuestCategory> {

    public static int QUEST_PER_PAGE = 12;

    private int page, maxPages;
    private List<ResourceLocation> quests;
    private final ServerPlayer player;

    private final QuestCategory category;

    private Map<Integer, Quest> updateList;
    private final List<Integer> toremove = new ArrayList<>();

    protected QuestGui(int syncId, Inventory playerInventory, QuestCategory category) {
        super(syncId, playerInventory, 6, category);
        this.category = category;
        if (playerInventory.player instanceof ServerPlayer)
            this.player = (ServerPlayer) playerInventory.player;
        else
            throw new IllegalStateException("This is a server side container");
    }

    public static void openGui(Player player, QuestCategory category) {
        MenuProvider fac = new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new QuestGui(syncId, inv, category);
            }

            @Override
            public Component getDisplayName() {
                return Component.literal(ConfigHandler.lang.get("simplequests.gui.main"));
            }
        };
        player.openMenu(fac);
    }

    private ItemStack ofQuest(int i, Quest quest, ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        PlayerData.AcceptType type = data.canAcceptQuest(quest);
        ItemStack stack = type == PlayerData.AcceptType.ACCEPT ? quest.getIcon() : new ItemStack(Items.BOOK);
        stack.setHoverName(Component.literal(quest.questTaskString).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GOLD)));
        ListTag lore = new ListTag();
        if (data.isActive(quest)) {
            stack.enchant(Enchantments.UNBREAKING, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
        }
        if (type == PlayerData.AcceptType.DELAY) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(String.format(ConfigHandler.lang.get(type.langKey()), data.formattedCooldown(quest))).withStyle(ChatFormatting.DARK_RED))));
            this.updateList.put(i, quest);
        }
        for (MutableComponent comp : quest.getFormattedGuiTasks(player))
            lore.add(StringTag.valueOf(Component.Serializer.toJson(comp.setStyle(comp.getStyle().withItalic(false)))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        stack.getOrCreateTagElement("SimpleQuests").putString("Quest", quest.id.toString());
        return stack;
    }

    public static ItemStack emptyFiller() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(Component.literal(""));
        return stack;
    }

    private static void playSongToPlayer(ServerPlayer player, SoundEvent event, float vol, float pitch) {
        player.connection.send(
                new ClientboundSoundPacket(event, SoundSource.PLAYERS, player.position().x, player.position().y, player.position().z, vol, pitch, player.getRandom().nextLong()));
    }

    @Override
    protected void fillInventoryWith(Player player, SeparateInv inv, QuestCategory category) {
        this.updateList = new HashMap<>();
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        Map<ResourceLocation, Quest> questMap = QuestsManager.instance().getQuestsForCategory(category);
        this.quests = new ArrayList<>(questMap.keySet());
        this.quests.removeIf(res -> {
            PlayerData.AcceptType type = PlayerData.get(serverPlayer).canAcceptQuest(questMap.get(res));
            return type == PlayerData.AcceptType.REQUIREMENTS || type == PlayerData.AcceptType.ONETIME || type == PlayerData.AcceptType.DAILYFULL || type == PlayerData.AcceptType.LOCKED;
        });
        this.maxPages = (this.quests.size() - 1) / QUEST_PER_PAGE;
        int id = 0;
        for (int i = 0; i < 54; i++) {
            if (i == 8 && this.quests.size() > QUEST_PER_PAGE) {
                ItemStack close = new ItemStack(Items.ARROW);
                close.setHoverName(Component.literal(ConfigHandler.lang.get("simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                inv.updateStack(i, close);
            } else if (i == 45) {
                ItemStack stack = new ItemStack(Items.TNT);
                stack.setHoverName(Component.literal(ConfigHandler.lang.get("simplequests.button.main")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                inv.updateStack(i, stack);
            } else if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8)
                inv.updateStack(i, emptyFiller());
            else if (i % 9 == 1 || i % 9 == 4 || i % 9 == 7) {
                if (id < this.quests.size()) {
                    ItemStack stack = this.ofQuest(i, questMap.get(this.quests.get(id)), serverPlayer);
                    if (!stack.isEmpty()) {
                        inv.updateStack(i, this.ofQuest(i, questMap.get(this.quests.get(id)), serverPlayer));
                        id++;
                    }
                }
            }
        }
    }

    private void flipPage() {
        this.updateList.clear();
        Map<ResourceLocation, Quest> questMap = QuestsManager.instance().getQuestsForCategory(this.category);
        int id = this.page * QUEST_PER_PAGE;
        for (int i = 0; i < 54; i++) {
            if (i == 0) {
                ItemStack stack = emptyFiller();
                if (this.page > 0) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(Component.literal(ConfigHandler.lang.get("simplequests.gui.prev")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (i == 8) {
                ItemStack stack = emptyFiller();
                if (this.page < this.maxPages) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(Component.literal(ConfigHandler.lang.get("simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (i == 45) {
                ItemStack stack = new ItemStack(Items.TNT);
                stack.setHoverName(Component.literal(ConfigHandler.lang.get("simplequests.button.main")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                this.slots.get(i).set(stack);
            } else if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8)
                this.slots.get(i).set(emptyFiller());
            else if (i % 9 == 1 || i % 9 == 4 || i % 9 == 7) {
                if (id < this.quests.size()) {
                    this.slots.get(i).set(this.ofQuest(i, questMap.get(this.quests.get(id)), this.player));
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
            QuestGui.playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        if (index == 8) {
            this.page++;
            this.flipPage();
            QuestGui.playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        if (index == 45) {
            player.closeContainer();
            player.getServer().execute(() -> QuestCategoryGui.openGui(player));
            QuestGui.playSongToPlayer(player, SoundEvents.UI_BUTTON_CLICK, 1, 1f);
            return true;
        }
        ItemStack stack = slot.getItem();
        if (!stack.hasTag())
            return false;
        CompoundTag tag = stack.getTag().getCompound("SimpleQuests");
        if (!tag.contains("Quest"))
            return false;
        if (stack.getItem() == Items.BOOK) {
            playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            return false;
        }
        ResourceLocation id = new ResourceLocation(tag.getString("Quest"));
        Quest quest = QuestsManager.instance().getQuestsForCategory(this.category).get(id);
        if (quest == null) {
            SimpleQuests.logger.error("No such quest " + id);
            return false;
        }
        boolean remove = stack.isEnchanted();
        ConfirmScreenHandler.openConfirmScreen(player, b -> {
            if (b) {
                player.closeContainer();
                if (remove) {
                    PlayerData.get(player).reset(quest.id, true);
                    playSongToPlayer(player, SoundEvents.ANVIL_FALL, 1, 1.2f);
                } else {
                    if (PlayerData.get(player).acceptQuest(quest))
                        playSongToPlayer(player, SoundEvents.NOTE_BLOCK_PLING, 1, 1.2f);
                    else
                        playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
                }
            } else {
                player.closeContainer();
                player.getServer().execute(() -> QuestGui.openGui(player, this.category));
                playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
            }
        }, remove ? "simplequests.gui.reset" : "simplequests.gui.confirm");
        return true;
    }

    @Override
    protected boolean isRightSlot(int slot) {
        return (this.page > 0 && slot == 0) || (this.page < this.maxPages && slot == 8) || slot == 45 || (slot < 45 && slot > 8 && (slot % 9 == 1 || slot % 9 == 4 || slot % 9 == 7));
    }

    public void update() {
        PlayerData data = PlayerData.get(this.player);
        this.toremove.removeIf(i -> {
            this.slots.get(i).set(this.ofQuest(i, this.updateList.get(i), this.player));
            this.updateList.remove(i);
            return true;
        });
        this.updateList.forEach((i, q) -> {
            ItemStack stack = this.slots.get(i).getItem();
            ListTag tag = stack.getOrCreateTagElement("display").getList("Lore", Tag.TAG_STRING);
            String delay = data.formattedCooldown(q);
            tag.set(0, StringTag.valueOf(Component.Serializer.toJson(Component.literal(String.format(ConfigHandler.lang.get(PlayerData.AcceptType.DELAY.langKey()), delay)).withStyle(ChatFormatting.DARK_RED))));
            if (delay.equals("0s"))
                this.toremove.add(i);
        });
    }
}
