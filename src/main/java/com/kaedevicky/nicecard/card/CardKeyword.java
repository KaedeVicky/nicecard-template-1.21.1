package com.kaedevicky.nicecard.card;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum CardKeyword implements StringRepresentable {
    // 战吼：打出时触发
    BATTLECRY("battlecry"),

    // 亡语：死亡时触发
    DEATHRATTLE("deathrattle"),

    // 嘲讽：必须优先攻击该随从 (预留，你可以先加上，也可以之后再加)
    TAUNT("taunt"),

    // 冲锋：当回合即可攻击 (预留)
    CHARGE("charge");

    // 自动生成 Codec，用于 JSON 解析
    public static final Codec<CardKeyword> CODEC = StringRepresentable.fromEnum(CardKeyword::values);

    private final String name;

    CardKeyword(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    // 辅助方法：获取显示的本地化键名 (例如 keyword.nicecard.battlecry)
    // 方便你以后做多语言支持
    public String getTranslationKey() {
        return "keyword.nicecard." + this.name;
    }
}