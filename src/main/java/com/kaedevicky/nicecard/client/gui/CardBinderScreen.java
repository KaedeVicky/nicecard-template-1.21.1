package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.network.PacketBinderPage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class CardBinderScreen extends AbstractContainerScreen<CardBinderMenu> {

    // 使用原版双箱子贴图 (54格)
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private Button btnPrev;
    private Button btnNext;

    public CardBinderScreen(CardBinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222; // 双箱子高度
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 放置翻页按钮
        // 放在界面右侧外部，避免遮挡槽位
        int btnX = this.leftPos + 178;
        int btnY = this.topPos + 18;

        this.btnPrev = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(0))
                .bounds(btnX, btnY, 20, 20)
                .build());

        this.btnNext = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(btnX, btnY + 22, 20, 20)
                .build());

        updateButtons();
    }

    private void changePage(int page) {
        // 发包给服务端
        PacketDistributor.sendToServer(new PacketBinderPage(page));
        // 客户端预先更新一下，虽然会被服务端同步覆盖
        this.menu.setPage(page, this.minecraft.player.getInventory());
        updateButtons();
    }

    private void updateButtons() {
        int page = this.menu.getCurrentPage();
        this.btnPrev.active = (page == 1); // 如果在第2页，可以点上一页
        this.btnNext.active = (page == 0); // 如果在第1页，可以点下一页
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 绘制页码文字
        String pageText = (this.menu.getCurrentPage() + 1) + " / 2";
        graphics.drawString(this.font, pageText, x + 180, y + 50, 0xFFFFFF, true);
    }
}