package com.kaedevicky.nicecard.logic;

import com.kaedevicky.nicecard.card.CardCategory;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.card.CardRarity;
import com.kaedevicky.nicecard.manager.CardManager;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import com.kaedevicky.nicecard.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CardGenerator {

    /**
     * 生成包含 5 张卡牌的列表
     * @param random 随机源
     * @param category 目标分类 (例如 OVERWORLD)
     * @return 包含 5 张卡牌的列表 (可能包含重复卡牌)
     */
    public static List<ItemStack> generatePackContents(RandomSource random, CardCategory category) {
        List<ItemStack> resultPack = new ArrayList<>();

        // 1. 从 Manager 获取该分类下的所有卡牌 (底池)
        List<CardDefinition> categoryPool = CardManager.INSTANCE.getCardsByCategory(category);

        // 如果底池为空 (JSON 没写)，直接返回空列表
        if (categoryPool.isEmpty()) {
            return resultPack;
        }

        // 2. 循环 5 次，生成 5 张卡
        for (int i = 0; i < 5; i++) {
            // 每次循环都独立抽取一张，放入结果列表
            ItemStack card = generateSingleCard(random, categoryPool);

            // 只要生成成功 (不是空)，就加进去
            // 这里不检查是否重复 (List 允许重复元素)
            if (!card.isEmpty()) {
                resultPack.add(card);
            }
        }
        System.out.println("Result Pack:" + resultPack);
        return resultPack;
    }

    /**
     * 内部辅助方法：从给定的池子里抽一张
     */
    private static ItemStack generateSingleCard(RandomSource random, List<CardDefinition> pool) {
        // 1. 随机决定稀有度 (权重)
        // 普通 60%, 稀有 25%, 史诗 10%, 传说 5%
        float roll = random.nextFloat();
        CardRarity targetRarity;

        if (roll < 0.60f) {
            targetRarity = CardRarity.COMMON;
        } else if (roll < 0.85f) {
            targetRarity = CardRarity.RARE;
        } else if (roll < 0.95f) {
            targetRarity = CardRarity.EPIC;
        } else {
            targetRarity = CardRarity.LEGENDARY;
        }

        // 2. 在【同分类池】里，筛选出符合【目标稀有度】的卡
        List<CardDefinition> rarityPool = new ArrayList<>();
        for (CardDefinition def : pool) {
            if (def.rarity() == targetRarity) {
                rarityPool.add(def);
            }
        }

        // 3. 严格检查：如果该稀有度没有卡，返回空 (该卡位轮空)
        // 因为你确认每个稀有度都有卡，所以这里理论上不会触发
        if (rarityPool.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 4. 从符合条件的卡里随机选一张
        // random.nextInt 保证了随机性，允许重复选中同一张
        CardDefinition selectedDef = rarityPool.get(random.nextInt(rarityPool.size()));

        // 5. 封装成物品
        // 请确保这里的物品注册名正确 (例如 ModItems.BASE_CARD.get())
        ItemStack stack = new ItemStack(ModItems.BASE_CARD.get());
        stack.set(ModDataComponents.CARD_DEF, selectedDef);

        return stack;
    }
}