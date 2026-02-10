package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.logic.PackRewardManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PacketClaimRewards() implements CustomPacketPayload {

    public static final Type<PacketClaimRewards> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "claim_rewards"));

    // 空包，不需要传输数据，只是一个信号
    public static final StreamCodec<ByteBuf, PacketClaimRewards> STREAM_CODEC = StreamCodec.unit(new PacketClaimRewards());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 处理逻辑：服务端收到包 -> 从缓存取出卡牌 -> 给玩家
    public static void handle(PacketClaimRewards payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 从管理器领取暂存的卡牌
                List<ItemStack> cards = PackRewardManager.claimReward(player);

                if (cards != null && !cards.isEmpty()) {
                    for (ItemStack card : cards) {
                        if (!player.getInventory().add(card)) {
                            player.drop(card, false);
                        }
                    }
                    // 可选：播放获得物品的音效
                    // player.playSound(SoundEvents.ITEM_PICKUP, 0.2F, 1.0F);
                }
            }
        });
    }
}