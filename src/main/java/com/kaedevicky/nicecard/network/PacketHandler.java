package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1"); // 版本号 "1"

        registrar.playToClient(
                SyncCardsPayload.TYPE,
                SyncCardsPayload.STREAM_CODEC,
                SyncCardsPayload::handleData
        );

        registrar.playToClient(
                OpenPackPayload.TYPE,
                OpenPackPayload.STREAM_CODEC,
                (payload, context) -> {
                    // 在主线程处理 GUI 打开逻辑
                    context.enqueueWork(() -> {
                        // 调用客户端方法打开屏幕 (需要物理隔离，下面会写)
                        System.out.println("Client received OpenPackPayload! Card count: " + payload.cards().size());
                        com.kaedevicky.nicecard.client.ClientHooks.openCardPackScreen(payload.cards());
                    });
                }
        );

        registrar.playToServer(
                PacketClaimRewards.TYPE,         // 包类型 ID
                PacketClaimRewards.STREAM_CODEC, // 编解码器
                PacketClaimRewards::handle       // 处理方法 (我们在 PacketClaimRewards 类里写好的静态方法)
        );

        registrar.playToServer(
                PacketBinderPage.TYPE,         // 包类型
                PacketBinderPage.STREAM_CODEC, // 编解码器
                PacketBinderPage::handle       // 处理逻辑 (引用 PacketBinderPage 类里的静态 handle 方法)
        );

        registrar.playToServer(
                PacketTableAction.TYPE,         // 包类型
                PacketTableAction.STREAM_CODEC, // 编解码器
                PacketTableAction::handle       // 处理逻辑 (引用 PacketTableAction 类里的静态 handle 方法)
        );

        registrar.playToClient(
                PacketOpenGameTable.TYPE,
                PacketOpenGameTable.STREAM_CODEC,
                PacketOpenGameTable::handle
        );

        registrar.playToServer(
                PacketGameMove.TYPE,
                PacketGameMove.STREAM_CODEC,
                PacketGameMove::handle
        );

    }
}