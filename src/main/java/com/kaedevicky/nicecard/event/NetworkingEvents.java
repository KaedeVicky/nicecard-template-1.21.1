package com.kaedevicky.nicecard.event;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.manager.CardManager;
import com.kaedevicky.nicecard.network.SyncCardsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = NiceCard.MODID)
public class NetworkingEvents {

    // 当玩家登录时发送
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncCardsToPlayer(serverPlayer);
        }
    }

    // 当数据包重载时发送（例如执行 /reload）
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            syncCardsToPlayer(event.getPlayer());
        } else {
            // 如果 player 为 null，说明是全服广播（例如 dedicated server 启动完毕或全体 reload）
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                syncCardsToPlayer(player);
            }
        }
    }

    private static void syncCardsToPlayer(ServerPlayer player) {
        List<CardDefinition> cards = new ArrayList<>(CardManager.INSTANCE.getAllCards());
        // 发送包
        PacketDistributor.sendToPlayer(player, new SyncCardsPayload(cards));
    }
}