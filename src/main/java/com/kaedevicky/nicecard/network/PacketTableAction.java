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

public record PacketTableAction(BlockPos pos, int actionType) implements CustomPacketPayload {
    // actionType: 1=Join P1, 2=Join P2, 3=Start Game

    public static final Type<PacketTableAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "table_action"));

    public static final StreamCodec<ByteBuf, PacketTableAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketTableAction::pos,
            ByteBufCodecs.INT, PacketTableAction::actionType,
            PacketTableAction::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketTableAction payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof GameTableBlockEntity table) {
                switch (payload.actionType()) {
                    case 1 -> table.tryJoin(player, 1);
                    case 2 -> table.tryJoin(player, 2);
                    case 3 -> table.tryStartGame(player);
                    case 4 -> {
                        if (table.gameData.currentPlayer == 1 && player.getUUID().equals(table.gameData.player1.uuid)) {
                            // P1 结束回合
                            new GameController(table.gameData).endTurn();
                            table.syncData();
                        } else if (table.gameData.currentPlayer == 2 && player.getUUID().equals(table.gameData.player2.uuid)) {
                            // P2 结束回合
                            new GameController(table.gameData).endTurn();
                            table.syncData();
                        }
                    }
                }
            }
        });
    }
}