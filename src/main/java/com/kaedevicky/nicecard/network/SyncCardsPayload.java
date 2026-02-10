package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.client.ClientCardManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncCardsPayload(List<CardDefinition> cards) implements CustomPacketPayload {

    public static final Type<SyncCardsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "sync_cards"));

    // 修正部分：使用 NeoForge 1.21.1 标准写法
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCardsPayload> STREAM_CODEC = StreamCodec.composite(
            // 1. 将 JSON Codec 转为网络 StreamCodec，并应用 List 转换
            ByteBufCodecs.fromCodec(CardDefinition.CODEC).apply(ByteBufCodecs.list()),
            // 2. 对应记录类字段的 Getter
            SyncCardsPayload::cards,
            // 3. 构造函数引用
            SyncCardsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleData(final SyncCardsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 【修正】直接更新通用的 CardManager，而不是 ClientCardManager
            // 这样 CardInstance.load() 里的 CardManager.INSTANCE.getCard() 就能在客户端工作了
            NiceCard.LOGGER.info("CLIENT RECEIVED CARDS: " + payload.cards().size());
            ClientCardManager.INSTANCE.updateCards(payload.cards());
        });
    }
}