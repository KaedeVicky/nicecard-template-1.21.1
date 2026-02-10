package com.kaedevicky.nicecard.item;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.client.tooltip.CardImageTooltip;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

public class CardItem extends Item {

    public CardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 1. 获取卡牌定义
        CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
        if (def == null) {
            tooltipComponents.add(Component.translatable("tooltip.nicecard.unknown_card").withStyle(ChatFormatting.RED));
            return;
        }

        // 2. 显示基本信息
        // 名称与稀有度颜色
        tooltipComponents.add(Component.literal(def.name()).withStyle(def.rarity().getColor(), ChatFormatting.BOLD));

        // 类别
        tooltipComponents.add(Component.literal("Category: " + def.category().name()).withStyle(ChatFormatting.GRAY));

        // 类型 (Minion/Spell)
        tooltipComponents.add(Component.literal("Type: " + def.type().name()).withStyle(ChatFormatting.BLUE));

        // 费用与属性
        tooltipComponents.add(Component.literal("Cost: " + def.cost() + " | DMG: " + def.damage() + " | Health: " + def.health()).withStyle(ChatFormatting.YELLOW));

        // 描述
        tooltipComponents.add(Component.literal(def.description()).withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE));

        // 3. 显示词条 (Affixes)
        List<String> affixes = stack.get(ModDataComponents.AFFIXES);
        if (affixes != null && !affixes.isEmpty()) {
            tooltipComponents.add(Component.literal("Affixes:").withStyle(ChatFormatting.GOLD));
            for (String affix : affixes) {
                tooltipComponents.add(Component.literal(" - " + affix).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        // 1. 获取定义 (如果为空直接返回)
        CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
        if (def == null) return Optional.empty();

        // 2. 计算背景路径 (GUI 高清版)
        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/bg/" + def.category().name().toLowerCase() + ".png");

        // 3. 计算插画路径 (GUI 高清版)
        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath;
        if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        if (fileName.endsWith(".png")) fileName = fileName.substring(0, fileName.length() - 4);

        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/art/" + fileName + ".png");

        // 4. 计算边框路径 (GUI 高清版)
        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/border/" + def.rarity().name().toLowerCase() + ".png");

        // ------------------------------------------------------------
        // 【核心修改】这里需要传入 6 个参数了
        // ------------------------------------------------------------
        return Optional.of(new CardImageTooltip(
                bgLoc,
                artLoc,
                borderLoc,
                def.cost(),     // 传入花费
                def.damage(),   // 传入攻击力 (注意：如果你的 CardDefinition 里叫 damage()，这里就写 def.damage())
                def.health()    // 传入血量
        ));
    }



    // 辅助方法：创建一个具体的卡牌 ItemStack
    public static ItemStack createCard (CardDefinition def){
        ItemStack stack = new ItemStack(com.kaedevicky.nicecard.registry.ModItems.BASE_CARD.get());
        stack.set(ModDataComponents.CARD_DEF, def);
        return stack;
    }
}