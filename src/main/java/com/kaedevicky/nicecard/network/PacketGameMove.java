package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.game.GameController;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// 记录一次操作：出牌(PLAY_CARD) 或 攻击(ATTACK)
public record PacketGameMove(
        BlockPos pos,
        int moveType,      // 1=出牌, 2=攻击, 3=英雄技能, 4=结束回合
        int sourceIndex,   // 手牌索引 或 攻击者索引
        int targetIndex,   // 目标索引 (-1代表打脸/无目标)
        boolean targetIsFace // 目标是否是英雄
) implements CustomPacketPayload {

    public static final Type<PacketGameMove> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "game_move"));

    public static final StreamCodec<ByteBuf, PacketGameMove> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketGameMove::pos,
            ByteBufCodecs.INT, PacketGameMove::moveType,
            ByteBufCodecs.INT, PacketGameMove::sourceIndex,
            ByteBufCodecs.INT, PacketGameMove::targetIndex,
            ByteBufCodecs.BOOL, PacketGameMove::targetIsFace,
            PacketGameMove::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketGameMove payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof GameTableBlockEntity table) {
                // 1. 鉴权
                int playerIndex = 0;
                if (player.getUUID().equals(table.gameData.player1.uuid)) playerIndex = 1;
                else if (player.getUUID().equals(table.gameData.player2.uuid)) playerIndex = 2;

                if (playerIndex == 0) return; // 旁观者勿扰

                GameController controller = new GameController(table.gameData);

                // 2. 执行逻辑
                if (payload.moveType() == 1) {
                    // 出牌：source=手牌槽位
                    controller.playCard(playerIndex, payload.sourceIndex(), payload.targetIndex());
                } else if (payload.moveType() == 2) {
                    // 攻击：source=己方随从槽位, target=目标随从槽位
                    controller.minionAttack(playerIndex, payload.sourceIndex(), payload.targetIndex(), payload.targetIsFace());
                } else if (payload.moveType() == 4) {
                    // 结束回合
                    controller.endTurn();
                }

                // 3. 同步状态
                table.syncData();
            }
        });
    }
}