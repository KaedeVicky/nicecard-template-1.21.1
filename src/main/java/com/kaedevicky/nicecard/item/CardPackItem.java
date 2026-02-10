package com.kaedevicky.nicecard.item;

import com.kaedevicky.nicecard.card.CardCategory;
import com.kaedevicky.nicecard.logic.CardGenerator;
import com.kaedevicky.nicecard.logic.PackRewardManager;
import com.kaedevicky.nicecard.network.OpenPackPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class CardPackItem extends Item {
    private final CardCategory packCategory;

    public CardPackItem(Properties properties, CardCategory category) {
        super(properties);
        this.packCategory = category;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 只有服务端才需要startUsingItem来同步状态，客户端主要是为了动画连贯
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        // 1. 消耗卡包
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // 2. 生成卡牌
        List<ItemStack> rawCards = CardGenerator.generatePackContents(level.random, this.packCategory);

        // 3. 【修复崩溃的核心步骤】
        // NeoForge 的 Codec 严禁列表里出现 ItemStack.EMPTY。
        // 我们必须进行流式过滤，彻底剔除空物品。
        List<ItemStack> validCards = rawCards.stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // 如果没有有效卡牌，提示错误并结束，不要发包
        if (validCards.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Error: Pack opened but no valid cards generated."));
            return stack;
        }

        // 4. 【修改逻辑】
        // 不再直接 add 到 inventory。
        // 而是存入服务端的临时缓存中。
        PackRewardManager.addReward(player, validCards);

        // 5. 发送网络包
        // 此时 validCards 里绝对没有空物品，不会再崩溃了
        PacketDistributor.sendToPlayer(player, new OpenPackPayload(validCards));

        player.awardStat(Stats.ITEM_USED.get(this));
        return stack;
    }
}