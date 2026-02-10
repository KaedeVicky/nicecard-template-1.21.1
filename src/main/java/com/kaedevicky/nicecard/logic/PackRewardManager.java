package com.kaedevicky.nicecard.logic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PackRewardManager {
    // 简单的内存缓存：玩家UUID -> 待领取的卡牌列表
    private static final Map<UUID, List<ItemStack>> PENDING_REWARDS = new HashMap<>();

    public static void addReward(Player player, List<ItemStack> cards) {
        PENDING_REWARDS.put(player.getUUID(), cards);
    }

    public static List<ItemStack> claimReward(Player player) {
        return PENDING_REWARDS.remove(player.getUUID());
    }
}