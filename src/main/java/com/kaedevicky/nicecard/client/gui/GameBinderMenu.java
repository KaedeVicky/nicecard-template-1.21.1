package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.card.CardCategory;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import com.kaedevicky.nicecard.registry.ModItems;
import com.kaedevicky.nicecard.registry.ModMenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class GameBinderMenu extends AbstractContainerMenu {
    // 限制：30 张卡
    public static final int MAX_CARDS = 30;
    // 限制：每种卡最多 2 张
    public static final int MAX_COPIES_PER_CARD = 2;

    private final SimpleContainer deckInventory;
    private final ItemStack binderStack;
    private final int binderSlotIndex;

    // 客户端构造函数 (通过 Network 传入)
    public GameBinderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    // 服务端构造函数
    public GameBinderMenu(int containerId, Inventory playerInventory, int binderSlotIndex) {
        super(ModMenuTypes.GAME_BINDER_MENU.get(), containerId);
        this.binderSlotIndex = binderSlotIndex;

        // 获取卡册 ItemStack
        // 如果是手持打开，binderSlotIndex 通常传 -1 或特定约定值
        // 这里假设如果 >=0 是背包槽位，否则是主手
        if (binderSlotIndex >= 0) {
            this.binderStack = playerInventory.getItem(binderSlotIndex);
        } else {
            this.binderStack = playerInventory.player.getMainHandItem();
        }

        // 初始化卡册内部容器
        this.deckInventory = new SimpleContainer(MAX_CARDS);

        // 1. 读取数据 (NeoForge 1.21.1 标准写法)
        // 使用 getOrDefault 防止 NBT 为空导致崩溃
        ItemContainerContents contents = this.binderStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        // 将内容复制到 SimpleContainer 中
        contents.copyInto(this.deckInventory.getItems());

        // 2. 添加监听器：当容器内容变化时，实时写回 ItemStack
        this.deckInventory.addListener(container -> saveToItem());

        // 3. 添加卡组插槽 (5行 x 6列 = 30格)
        int startX = 34; // 居中起始 X
        int startY = 18; // 起始 Y

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 6; col++) {
                // index: 0 ~ 29
                this.addSlot(new DeckSlot(deckInventory, col + row * 6, startX + col * 18, startY + row * 18));
            }
        }

        // 4. 添加玩家背包插槽
        addPlayerInventory(playerInventory);
    }

    // --- 核心逻辑：保存数据到物品 ---
    private void saveToItem() {
        if (!binderStack.isEmpty()) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < deckInventory.getContainerSize(); i++) {
                items.add(deckInventory.getItem(i));
            }
            // 1.21.1 写法：使用 ItemContainerContents.fromItems
            binderStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }

    // --- 核心逻辑：获取当前卡组被锁定的阵营 ---
    public CardCategory getLockedCategory() {
        for (int i = 0; i < deckInventory.getContainerSize(); i++) {
            ItemStack stack = deckInventory.getItem(i);
            if (stack.isEmpty()) continue;

            CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
            if (def != null) {
                // 只要发现一张不是中立的卡，整个卡组就被锁定为该阵营
                if (def.category() != CardCategory.NEUTRAL) {
                    return def.category();
                }
            }
        }
        return null; // 全空或全中立 -> 未锁定
    }

    // --- 核心逻辑：规则验证 ---
    public boolean canAcceptCard(ItemStack stack) {
        // 1. 必须是基础卡牌物品
        if (stack.getItem() != ModItems.BASE_CARD.get()) return false;

        // 2. 必须包含卡牌定义数据
        CardDefinition incomingDef = stack.get(ModDataComponents.CARD_DEF);
        if (incomingDef == null) return false;

        // 3. 阵营检查
        // 如果放入的是中立卡，永远允许 (除非有其他限制)
        if (incomingDef.category() != CardCategory.NEUTRAL) {
            CardCategory locked = getLockedCategory();
            // 如果卡组已锁定阵营，且新卡阵营不匹配 -> 拒绝
            if (locked != null && locked != incomingDef.category()) {
                return false;
            }
            // 补充逻辑：如果卡组目前全是中立卡(locked == null)，
            // 那么放入这张卡后，卡组就会变成该阵营，这是允许的。
        }

        // 4. 同名卡检查 (最多 2 张)
        int existingCopies = 0;
        for (int i = 0; i < deckInventory.getContainerSize(); i++) {
            ItemStack s = deckInventory.getItem(i);
            if (!s.isEmpty()) {
                CardDefinition def = s.get(ModDataComponents.CARD_DEF);
                // 优先比较 ID，如果没有 ID 则比较 TextureLocation
                if (def != null && isSameCard(incomingDef, def)) {
                    existingCopies += s.getCount();
                }
            }
        }

        // 如果加上手里这张(stack.getCount 一般是 1)会超过限制，则拒绝
        // 注意：这里我们验证的是“能否放入 1 张”，所以是 < MAX
        return existingCopies < MAX_COPIES_PER_CARD;
    }

    // 辅助：判断两张卡是否相同
    private boolean isSameCard(CardDefinition a, CardDefinition b) {
        if (a.id() != null && b.id() != null) {
            return a.id().equals(b.id());
        }
        return a.textureLocation().equals(b.textureLocation());
    }

    // --- Shift + 点击 逻辑 ---
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 区域划分：
            // 0 ~ 29 : 卡组槽位 (Deck)
            // 30 ~ 56 : 玩家背包 (Inventory)
            // 57 ~ 65 : 玩家快捷栏 (Hotbar)

            int deckSize = MAX_CARDS;
            int invStart = deckSize;
            int hotbarStart = invStart + 27;
            int end = hotbarStart + 9;

            // 1. 如果点击的是卡组里的卡 -> 移回玩家背包
            if (index < deckSize) {
                if (!this.moveItemStackTo(itemstack1, invStart, end, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 2. 如果点击的是玩家背包里的卡 -> 尝试放入卡组
            else {
                // 【关键】在移动前先检查是否符合卡组规则
                if (!canAcceptCard(itemstack1)) {
                    return ItemStack.EMPTY;
                }

                // 尝试移动到卡组 (0 ~ 30)
                if (!this.moveItemStackTo(itemstack1, 0, deckSize, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        // 简单的校验：确保手里的物品还是那个卡册
        // 这种校验防止玩家在打开界面时把卡册扔了导致刷物品
        return !binderStack.isEmpty() && binderStack.getItem() == ModItems.GAME_BINDER.get();
    }

    // 防止点击正在打开的卡册 (防止死循环或丢失物品)
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == binderSlotIndex) {
                return; // 禁止操作当前打开的卡册
            }
        }
        // 处理数字键交换 (Hotbar Swapping)
        if (clickType == ClickType.SWAP && button == binderSlotIndex) {
            return; // 禁止用快捷键交换当前卡册
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void addPlayerInventory(Inventory playerInv) {
        // 假设 GUI 背景图较大，玩家背包下移以避开卡组区
        int playerInvY = 120;
        int hotbarY = playerInvY + 58;

        // 玩家背包 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // 快捷栏 (1行)
        for (int col = 0; col < 9; col++) {
            // 如果是当前打开的卡册槽位，锁定它
            if (col == binderSlotIndex) {
                this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY) {
                    @Override public boolean mayPickup(Player p) { return false; }
                    @Override public boolean mayPlace(ItemStack s) { return false; }
                });
            } else {
                this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
            }
        }
    }

    // --- 自定义 Slot 类 ---
    private class DeckSlot extends Slot {
        public DeckSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 将规则检查代理给外部方法
            return canAcceptCard(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1; // 强制单张堆叠
        }
    }
}