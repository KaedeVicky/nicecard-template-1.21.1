package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.game.GameController;
import com.kaedevicky.nicecard.registry.ModItems; // 引入 ModItems
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PacketTableAction(BlockPos pos, int actionType) implements CustomPacketPayload {

    public static final Type<PacketTableAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "table_action"));

    public static final StreamCodec<ByteBuf, PacketTableAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketTableAction::pos,
            ByteBufCodecs.INT, PacketTableAction::actionType,
            PacketTableAction::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketTableAction payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (player.level().getBlockEntity(payload.pos()) instanceof GameTableBlockEntity table) {

                // 1. 加入 P1
                if (payload.actionType() == 1) {
                    if (hasGameBinder(player)) {
                        // 检查 P1 是否已被占用
                        if (table.gameData.player1.uuid == null) {
                            table.gameData.player1.uuid = player.getUUID();
                            table.gameData.player1.name = player.getGameProfile().getName();
                            table.syncData();
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("Server: You need a Game Binder to join!"));
                    }
                }

                // 2. 加入 P2
                else if (payload.actionType() == 2) {
                    if (hasGameBinder(player)) {
                        // 检查 P2 是否已被占用
                        if (table.gameData.player2.uuid == null) {
                            table.gameData.player2.uuid = player.getUUID();
                            table.gameData.player2.name = player.getGameProfile().getName();
                            table.syncData();
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("Server: You need a Game Binder to join!"));
                    }
                }

                // 3. 开始游戏
                else if (payload.actionType() == 3) {
                    // 只有当两人都准备好时才能开始
                    if (table.gameData.player1.uuid != null && table.gameData.player2.uuid != null) {
                        GameController controller = new GameController(table.gameData);
                        controller.initGame(player, (ServerPlayer) player.level().getPlayerByUUID(table.gameData.player2.uuid)); // 初始化牌库、抽牌等
                        table.isGameStarted = true;
                        table.syncData();
                    }
                }

                // 4. 结束回合
                else if (payload.actionType() == 4) {
                    GameController controller = new GameController(table.gameData);
                    controller.endTurn();
                    table.syncData();
                }
            }
        });
    }

    // 服务端检查：遍历背包寻找 GameBinder
    private static boolean hasGameBinder(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.GAME_BINDER.get()) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() == ModItems.GAME_BINDER.get();
    }
}