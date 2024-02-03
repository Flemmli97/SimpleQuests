package io.github.flemmli97.simplequests.gui;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.config.ConfigHandler;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.gui.inv.SeparateInv;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.quest.QuestCategory;
import io.github.flemmli97.simplequests.quest.types.CompositeQuest;
import io.github.flemmli97.simplequests.quest.types.QuestBase;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

public class QuestGui extends ServerOnlyScreenHandler<QuestGui.QuestGuiData> {

    public static int QUEST_PER_PAGE = 12;

    private int page, maxPages;
    private List<ResourceLocation> quests;
    private final ServerPlayer player;

    private final QuestCategory category;

    private final boolean canGoBack;

    private Map<Integer, QuestBase> updateList;
    private final List<Integer> toremove = new ArrayList<>();

    protected QuestGui(int syncId, Inventory playerInventory, QuestGui.QuestGuiData data) {
        super(syncId, playerInventory, 6, data);
        this.category = data.category;
        this.page = data.page;
        this.canGoBack = data.canGoBack;
        if (playerInventory.player instanceof ServerPlayer)
            this.player = (ServerPlayer) playerInventory.player;
        else
            throw new IllegalStateException("This is a server side container");
    }

    public static void openGui(ServerPlayer player, QuestCategory category) {
        openGui(player, category, true, 0);
    }

    public static void openGui(ServerPlayer player, QuestCategory category, boolean canGoBack, int page) {
        MenuProvider fac = new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                return new QuestGui(syncId, inv, new QuestGuiData(category, page, canGoBack));
            }

            @Override
            public Component getDisplayName() {
                return category == null || category == QuestCategory.DEFAULT_CATEGORY ?
                        new TranslatableComponent(ConfigHandler.LANG.get(player, "simplequests.gui.main")) : category.getName();
            }
        };
        player.openMenu(fac);
    }

    private ItemStack ofQuest(int i, QuestBase quest, ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        PlayerData.AcceptType type = data.canAcceptQuest(quest);
        ItemStack stack = type == PlayerData.AcceptType.ACCEPT ? quest.getIcon() : new ItemStack(Items.BOOK);
        stack.setHoverName(quest.getTask(player).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GOLD)));
        ListTag lore = new ListTag();
        quest.getDescription(player).forEach(c -> lore.add(StringTag.valueOf(Component.Serializer.toJson(c.setStyle(c.getStyle().withItalic(false))))));
        if (data.isActive(quest)) {
            stack.enchant(Enchantments.UNBREAKING, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
        }
        if (type == PlayerData.AcceptType.DELAY) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(new TranslatableComponent(ConfigHandler.LANG.get(player, type.langKey()), data.formattedCooldown(quest)).withStyle(ChatFormatting.DARK_RED))));
            this.updateList.put(i, quest);
        }
        for (MutableComponent comp : quest.getFormattedGuiTasks(player))
            lore.add(StringTag.valueOf(Component.Serializer.toJson(comp.setStyle(comp.getStyle().withItalic(false)))));
        MutableComponent requirement = switch (type) {
            case REQUIREMENTS, ONETIME, DAILYFULL, LOCKED ->
                    new TranslatableComponent(ConfigHandler.LANG.get(player, type.langKey())).withStyle(ChatFormatting.DARK_RED);
            default -> null;
        };
        if (requirement != null)
            lore.add(StringTag.valueOf(Component.Serializer.toJson(requirement.setStyle(requirement.getStyle().withItalic(false)))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        stack.getOrCreateTagElement("SimpleQuests").putString("Quest", quest.id.toString());
        return stack;
    }

    public static ItemStack emptyFiller() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(new TextComponent(""));
        return stack;
    }

    public static void playSongToPlayer(ServerPlayer player, SoundEvent event, float vol, float pitch) {
        player.connection.send(
                new ClientboundSoundPacket(event, SoundSource.PLAYERS, player.position().x, player.position().y, player.position().z, vol, pitch));
    }

    @Override
    protected void fillInventoryWith(Player player, SeparateInv inv, QuestGui.QuestGuiData data) {
        this.updateList = new HashMap<>();
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        Map<ResourceLocation, QuestBase> questMap = QuestsManager.instance().getQuestsForCategory(data.category());
        this.quests = new ArrayList<>(questMap.keySet());
        this.quests.removeIf(res -> {
            QuestBase quest = questMap.get(res);
            if (quest.visibility != QuestBase.Visibility.DEFAULT)
                return quest.visibility == QuestBase.Visibility.NEVER;
            PlayerData.AcceptType type = PlayerData.get(serverPlayer).canAcceptQuest(quest);
            return type == PlayerData.AcceptType.REQUIREMENTS || type == PlayerData.AcceptType.ONETIME
                    || type == PlayerData.AcceptType.DAILYFULL || type == PlayerData.AcceptType.LOCKED;
        });
        this.maxPages = (this.quests.size() - 1) / QUEST_PER_PAGE;
        int page = Mth.clamp(data.page, 0, this.maxPages);
        int id = page * QUEST_PER_PAGE;
        for (int i = 0; i < 54; i++) {
            if (i == 0) {
                ItemStack stack = emptyFiller();
                if (page > 0) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(serverPlayer, "simplequests.gui.previous")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                inv.updateStack(i, stack);
            } else if (i == 8 && page < this.maxPages) {
                ItemStack close = new ItemStack(Items.ARROW);
                close.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(serverPlayer, "simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                inv.updateStack(i, close);
            } else if (data.canGoBack() && i == 45) {
                ItemStack stack = new ItemStack(Items.TNT);
                stack.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(serverPlayer, "simplequests.gui.button.main")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
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
        Map<ResourceLocation, QuestBase> questMap = QuestsManager.instance().getQuestsForCategory(this.category);
        int id = this.page * QUEST_PER_PAGE;
        for (int i = 0; i < 54; i++) {
            if (i == 0) {
                ItemStack stack = emptyFiller();
                if (this.page > 0) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(this.player, "simplequests.gui.previous")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (i == 8) {
                ItemStack stack = emptyFiller();
                if (this.page < this.maxPages) {
                    stack = new ItemStack(Items.ARROW);
                    stack.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(this.player, "simplequests.gui.next")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
                }
                this.slots.get(i).set(stack);
            } else if (this.canGoBack && i == 45) {
                ItemStack stack = new ItemStack(Items.TNT);
                stack.setHoverName(new TranslatableComponent(ConfigHandler.LANG.get(this.player, "simplequests.gui.button.main")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.WHITE)));
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
        if (this.canGoBack && index == 45) {
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
        QuestBase quest = QuestsManager.instance().getQuestsForCategory(this.category).get(id);
        if (quest == null) {
            SimpleQuests.LOGGER.error("No such quest " + id);
            return false;
        }
        boolean remove = stack.isEnchanted();
        if (quest instanceof CompositeQuest composite) {
            if (remove) {
                ConfirmScreenHandler.openConfirmScreen(player, b -> {
                    if (b) {
                        player.closeContainer();
                        PlayerData data = PlayerData.get(player);
                        composite.getCompositeQuests().forEach(r -> {
                            if (data.isActive(r))
                                data.reset(r, true);
                        });
                        playSongToPlayer(player, SoundEvents.ANVIL_FALL, 1, 1.2f);
                    } else {
                        player.closeContainer();
                        player.getServer().execute(() -> QuestGui.openGui(player, this.category, this.canGoBack, this.page));
                        playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
                    }
                }, "simplequests.gui.reset");
            } else
                CompositeQuestScreenHandler.openScreen(player, composite, this.category, this.canGoBack, this.page);
        } else {
            ConfirmScreenHandler.openConfirmScreen(player, b -> {
                if (b) {
                    player.closeContainer();
                    if (remove) {
                        PlayerData.get(player).reset(quest.id, true);
                        playSongToPlayer(player, SoundEvents.ANVIL_FALL, 1, 1.2f);
                    } else {
                        if (PlayerData.get(player).acceptQuest(quest, 0))
                            playSongToPlayer(player, SoundEvents.NOTE_BLOCK_PLING, 1, 1.2f);
                        else
                            playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
                    }
                } else {
                    player.closeContainer();
                    player.getServer().execute(() -> QuestGui.openGui(player, this.category, this.canGoBack, this.page));
                    playSongToPlayer(player, SoundEvents.VILLAGER_NO, 1, 1f);
                }
            }, remove ? "simplequests.gui.reset" : "simplequests.gui.confirm");
        }
        return true;
    }

    @Override
    protected boolean isRightSlot(int slot) {
        return (this.page > 0 && slot == 0) || (this.page < this.maxPages && slot == 8) || (this.canGoBack && slot == 45) || (slot < 45 && slot > 8 && (slot % 9 == 1 || slot % 9 == 4 || slot % 9 == 7));
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
            tag.set(0, StringTag.valueOf(Component.Serializer.toJson(new TranslatableComponent(ConfigHandler.LANG.get(this.player, PlayerData.AcceptType.DELAY.langKey()), delay).withStyle(ChatFormatting.DARK_RED))));
            if (delay.equals("0s"))
                this.toremove.add(i);
        });
    }

    record QuestGuiData(QuestCategory category, int page, boolean canGoBack) {
    }
}
