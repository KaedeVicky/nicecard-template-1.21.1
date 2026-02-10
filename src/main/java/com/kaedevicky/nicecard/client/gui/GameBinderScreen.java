package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.card.CardCategory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GameBinderScreen extends AbstractContainerScreen<GameBinderMenu> {

    // 复用原版双箱子贴图
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public GameBinderScreen(GameBinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;

        // 调整文字标签的位置
        this.inventoryLabelY = this.imageHeight - 94; // 玩家背包文字
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // ⚠️ 重要：千万不要调用 super.renderLabels()
        // 因为 super 会画那个讨厌的默认标题，导致重叠。
        // 我们在这里完全接管标题的绘制。

        // 1. 绘制下方的 "Inventory" (玩家背包) 文字
        // this.inventoryLabelX/Y 是原版计算好的坐标，直接用
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 2. 计算卡牌数量
        int count = 0;
        for (int i = 0; i < GameBinderMenu.MAX_CARDS; i++) {
            if (this.menu.getSlot(i).hasItem()) count++;
        }

        // 3. 绘制左上角的动态标题
        // 格式: "Deck (5/30)"
        String titleText = "Deck (" + count + "/30)";
        // titleLabelX 默认是 8, titleLabelY 默认是 6
        graphics.drawString(this.font, titleText, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // 4. 绘制右上角的阵营提示
        CardCategory locked = this.menu.getLockedCategory();
        String categoryText = (locked == null) ? "Neutral / Any" : locked.name();
        int color = (locked == null) ? 0x666666 : 0xD4AF37; // 灰色 vs 金色

        // 计算文字宽度，以便右对齐
        int textWidth = this.font.width(categoryText);
        // imageWidth 通常是 176
        // x = 176 - 8 (右边距) - 文字宽度
        int categoryX = this.imageWidth - 8 - textWidth;

        graphics.drawString(this.font, categoryText, categoryX, this.titleLabelY, color, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}