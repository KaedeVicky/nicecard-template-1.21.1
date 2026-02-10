package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.registry.ModMenuTypes;
import com.kaedevicky.nicecard.registry.ModItems;
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

public class CardBinderMenu extends AbstractContainerMenu {
    public static final int TOTAL_SLOTS = 108;
    public static final int SLOTS_PER_PAGE = 54;

    private final SimpleContainer binderInventory;
    private final ItemStack binderStack;
    private final int binderSlotIndex;

    private int currentPage = 0;

    // 客户端构造
    public CardBinderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readInt());
    }

    // 服务端构造
    public CardBinderMenu(int containerId, Inventory playerInventory, int binderSlotIndex) {
        super(ModMenuTypes.CARD_BINDER_MENU.get(), containerId);
        this.binderSlotIndex = binderSlotIndex;

        if (binderSlotIndex >= 0) {
            this.binderStack = playerInventory.getItem(binderSlotIndex);
        } else {
            this.binderStack = playerInventory.player.getMainHandItem();
        }

        this.binderInventory = new SimpleContainer(TOTAL_SLOTS);

        ItemContainerContents contents = this.binderStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(this.binderInventory.getItems());
        }

        this.binderInventory.addListener(container -> saveToItem());

        // 1. 添加所有插槽 (构造时就定好位置)
        addBinderSlots();
        addPlayerInventory(playerInventory);

        // 2. 根据当前页码开关插槽
        updateSlotActiveState();
    }

    private void addBinderSlots() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            // 魔法计算：
            // 无论 i 是 0 (第1页第一个) 还是 54 (第2页第一个)，
            // visualIndex 都是 0。这样它们就在构造时被放在了同一个坐标。
            int visualIndex = i % SLOTS_PER_PAGE;

            int col = visualIndex % 9;
            int row = visualIndex / 9;

            int x = 8 + col * 18;
            int y = 18 + row * 18;

            // 使用我们自定义的 CardBinderSlot
            this.addSlot(new CardBinderSlot(binderInventory, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() != ModItems.CARD_BINDER.get();
                }
            });
        }
    }

    /**
     * 翻页核心：不改坐标，改 active 状态
     */
    private void updateSlotActiveState() {
        int startIndex = currentPage * SLOTS_PER_PAGE;
        int endIndex = startIndex + SLOTS_PER_PAGE;

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            // 确保我们获取的对象是 CardBinderSlot
            if (this.slots.get(i) instanceof CardBinderSlot slot) {
                if (i >= startIndex && i < endIndex) {
                    slot.setActive(true);  // 在当前页范围内 -> 激活
                } else {
                    slot.setActive(false); // 不在范围内 -> 禁用 (隐藏且不可点击)
                }
            }
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            if (col == binderSlotIndex) {
                this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198) {
                    @Override
                    public boolean mayPickup(Player player) { return false; }
                    @Override
                    public boolean mayPlace(ItemStack stack) { return false; }
                });
            } else {
                this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
            }
        }
    }

    private void saveToItem() {
        if (!binderStack.isEmpty()) {
            List<ItemStack> items = new ArrayList<>();
            for(int i = 0; i < binderInventory.getContainerSize(); i++) {
                items.add(binderInventory.getItem(i));
            }
            binderStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }

    public void setPage(int page, Inventory playerInv) {
        if (page < 0 || page > 1) return;
        this.currentPage = page;
        updateSlotActiveState(); // 翻页时更新激活状态
    }

    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // ============================================================
            // 情况 1: 物品在卡册中 (索引 0 ~ 107) -> 移回玩家背包
            // ============================================================
            if (index < TOTAL_SLOTS) {
                // 移动到玩家背包区域 (包括快捷栏)
                if (!this.moveItemStackTo(itemstack1, TOTAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // ============================================================
            // 情况 2: 物品在玩家背包中 -> 移入卡册
            // ============================================================
            else {
                // 1. 计算当前页面的槽位范围
                // 第 0 页: start=0,  end=54
                // 第 1 页: start=54, end=108
                int pageStart = currentPage * SLOTS_PER_PAGE;
                int pageEnd = pageStart + SLOTS_PER_PAGE;

                // 2. 【优先】尝试放入当前页面可见的槽位
                if (!this.moveItemStackTo(itemstack1, pageStart, pageEnd, false)) {

                    // 3. 【备选】如果当前页满了，尝试放入另一页 (整本卡册的其他位置)
                    // 如果你不希望它自动跳到另一页，可以把下面这个 if 删掉
                    int otherStart = (currentPage == 0) ? SLOTS_PER_PAGE : 0;
                    int otherEnd = otherStart + SLOTS_PER_PAGE;

                    if (!this.moveItemStackTo(itemstack1, otherStart, otherEnd, false)) {
                        return ItemStack.EMPTY; // 两页都满了，操作失败
                    }
                }
            }

            // 处理物品堆叠数量变化
            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !binderStack.isEmpty();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.container == player.getInventory() && slot.getContainerSlot() == binderSlotIndex) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    // --- 静态内部类 ---
    public static class CardBinderSlot extends Slot {
        private boolean active = true;

        public CardBinderSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }
}