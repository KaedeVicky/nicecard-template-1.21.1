package com.kaedevicky.nicecard.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class ClientCardTooltip implements ClientTooltipComponent {
    private final ResourceLocation bgTexture;
    private final ResourceLocation artTexture;
    private final ResourceLocation borderTexture;

    private final int cost;
    private final int attack;
    private final int health;

    // 【修改】改为 3:4 比例
    private static final int WIDTH = 90;
    private static final int HEIGHT = 120;

    public ClientCardTooltip(CardImageTooltip data) {
        this.bgTexture = data.bgTexture();
        this.artTexture = data.artTexture();
        this.borderTexture = data.borderTexture();
        this.cost = data.cost();
        this.attack = data.attack();
        this.health = data.health();
    }

    @Override
    public int getHeight() {
        return HEIGHT + 4;
    }

    @Override
    public int getWidth(Font font) {
        return WIDTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 【修改】使用 3:4 裁剪绘制
        drawTexture3x4(graphics, bgTexture, x, y, WIDTH, HEIGHT);
        drawTexture3x4(graphics, artTexture, x, y, WIDTH, HEIGHT);
        drawTexture3x4(graphics, borderTexture, x, y, WIDTH, HEIGHT);

        // --- 属性数值 ---
        float textScale = 0.8f; // 固定缩放，看起来比较精致
        float margin = 5.0f;    // 像素边距

        // A. 左上：Cost (青色)
        renderStat(graphics, font, String.valueOf(cost),
                x + margin, y + margin,
                0x00FFFF, textScale);

        // B. 左下：Attack (金色)
        renderStat(graphics, font, String.valueOf(attack),
                x + margin, y + HEIGHT - margin - 8,
                0xFFAA00, textScale);

        // C. 右下：Health (红色)
        String hpText = String.valueOf(health);
        int hpWidth = (int)(font.width(hpText) * textScale);
        renderStat(graphics, font, hpText,
                x + WIDTH - margin - hpWidth - 2, y + HEIGHT - margin - 8,
                0xFF5555, textScale);

        RenderSystem.disableBlend();
    }

    // 【新增】通用裁剪方法：从 1024x1024 正方形中截取中间的 768x1024 (3:4)
    private void drawTexture3x4(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
        graphics.blit(texture, x, y, w, h, 0, 0, 768, 1024, 768, 1024);
    }

    private void renderStat(GuiGraphics graphics, Font font, String text, float x, float y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 200.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }
}