package com.kaedevicky.nicecard.card;

import net.minecraft.ChatFormatting;

public enum CardRarity {
    COMMON(ChatFormatting.WHITE, 1),
    RARE(ChatFormatting.AQUA, 1),
    EPIC(ChatFormatting.LIGHT_PURPLE, 2),
    LEGENDARY(ChatFormatting.GOLD, 3);

    private final ChatFormatting color;
    private final int affixSlots;

    CardRarity(ChatFormatting color, int affixSlots) {
        this.color = color;
        this.affixSlots = affixSlots;
    }

    public ChatFormatting getColor() { return color; }
    public int getAffixSlots() { return affixSlots; }
}