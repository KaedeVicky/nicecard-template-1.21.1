package com.kaedevicky.nicecard.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

// 修改：接收三个参数 (背景, 插画, 边框)
public record CardImageTooltip(
        ResourceLocation bgTexture,
        ResourceLocation artTexture,
        ResourceLocation borderTexture,
        int cost,   // <--- 新增
        int attack, // <--- 新增
        int health  // <--- 新增
) implements TooltipComponent {
}