package com.kaedevicky.nicecard.card;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

// 这个类对应你的 JSON 结构
public record CardDefinition(
        String id,
        String name,
        CardRarity rarity,
        CardCategory category,
        CardType type,
        ResourceLocation textureLocation, // 卡牌内容大图路径
        int cost,
        int damage,
        int health,
        String description, // 效果描述
        List<CardKeyword> keywords
) {
    // 序列化编解码器，用于读取 JSON
    public static final Codec<CardDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CardDefinition::id),
            Codec.STRING.fieldOf("name").forGetter(CardDefinition::name),
            Codec.STRING.xmap(CardRarity::valueOf, CardRarity::name).fieldOf("rarity").forGetter(CardDefinition::rarity),
            Codec.STRING.xmap(CardCategory::valueOf, CardCategory::name).fieldOf("category").forGetter(CardDefinition::category),
            Codec.STRING.xmap(CardType::valueOf, CardType::name).fieldOf("type").forGetter(CardDefinition::type),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(CardDefinition::textureLocation),
            Codec.INT.fieldOf("cost").forGetter(CardDefinition::cost),
            Codec.INT.fieldOf("damage").forGetter(CardDefinition::damage),
            Codec.INT.fieldOf("health").forGetter(CardDefinition::health),
            Codec.STRING.fieldOf("description").forGetter(CardDefinition::description),
            CardKeyword.CODEC.listOf().optionalFieldOf("keywords", List.of()).forGetter(CardDefinition::keywords)
    ).apply(instance, CardDefinition::new));
}