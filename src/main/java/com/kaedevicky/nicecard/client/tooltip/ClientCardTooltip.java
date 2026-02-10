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

    // 新增：属性字段
    private final int cost;
    private final int attack;
    private final int health;

    // 显示尺寸
    private static final int DISPLAY_SIZE = 128;

    public ClientCardTooltip(CardImageTooltip data) {
        this.bgTexture = data.bgTexture();
        this.artTexture = data.artTexture();
        this.borderTexture = data.borderTexture();

        // 从 data 中读取数值
        this.cost = data.cost();
        this.attack = data.attack();
        this.health = data.health();
    }

    @Override
    public int getHeight() {
        return DISPLAY_SIZE + 4;
    }

    @Override
    public int getWidth(Font font) {
        return DISPLAY_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ---------------------------------------------------------
        // 1. 图层绘制 (调整了顺序：背景 -> 插画 -> 边框)
        // ---------------------------------------------------------

        // (1) 背景 (最底层)
        drawLayer(graphics, bgTexture, x, y);

        // (2) 插画 (中间层) - 应该被边框盖住边缘，所以先画
        drawLayer(graphics, artTexture, x, y);

        // (3) 边框 (最上层图片)
        drawLayer(graphics, borderTexture, x, y);

        // ---------------------------------------------------------
        // 2. 属性数值绘制 (文字层)
        // ---------------------------------------------------------

        // 计算字体缩放比例 (基于 DISPLAY_SIZE 自动调整)
        // 128px 大小时，缩放约 1.0~1.1 倍比较合适
        float textScale = (DISPLAY_SIZE / 128.0f) * 1.1f;
        if (textScale < 0.5f) textScale = 0.5f;

        // 边距比例 (控制数字离边缘多远)
        float margin = 0.12f;

        // A. 左上角：花费 (Cost) - 青色
        renderStat(graphics, font, String.valueOf(cost),
                x + DISPLAY_SIZE * margin,
                y + DISPLAY_SIZE * margin,
                0x00FFFF, textScale);

        // B. 左下角：攻击力 (Attack) - 金色
        renderStat(graphics, font, String.valueOf(attack),
                x + DISPLAY_SIZE * margin,
                y + DISPLAY_SIZE * (1.0f - margin),
                0xFFAA00, textScale);

        // C. 右下角：血量 (Health) - 红色
        renderStat(graphics, font, String.valueOf(health),
                x + DISPLAY_SIZE * (1.0f - margin),
                y + DISPLAY_SIZE * (1.0f - margin),
                0xFF5555, textScale);

        RenderSystem.disableBlend();
    }

    // 辅助方法：绘制图层
    private void drawLayer(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, DISPLAY_SIZE, DISPLAY_SIZE, 0, 0, 1024, 1024, 1024, 1024);
    }

    // 辅助方法：绘制属性文字
    private void renderStat(GuiGraphics graphics, Font font, String text, float cx, float cy, int color, float scale) {
        graphics.pose().pushPose();

        // 1. 移动到目标位置 (Z轴 +200 确保文字浮在所有图片最上面)
        graphics.pose().translate(cx, cy, 200.0f);

        // 2. 缩放
        graphics.pose().scale(scale, scale, 1.0f);

        // 3. 绘制文字 (居中对齐)
        // 因为 translate 到了中心点，所以 x = -width/2, y = -height/2
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        // 绘制带阴影的文字
        graphics.drawString(font, text, -textWidth / 2, -textHeight / 2 + 1, color, true);

        graphics.pose().popPose();
    }
}