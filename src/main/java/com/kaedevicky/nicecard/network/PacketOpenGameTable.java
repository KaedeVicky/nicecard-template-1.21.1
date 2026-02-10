package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.client.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketOpenGameTable(BlockPos pos) implements CustomPacketPayload {

    public static final Type<PacketOpenGameTable> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "open_game_table"));

    public static final StreamCodec<ByteBuf, PacketOpenGameTable> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PacketOpenGameTable::pos,
            PacketOpenGameTable::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketOpenGameTable payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            // 调用客户端钩子打开屏幕
            // 我们不能直接在这里写 new GameTableScreen，因为服务端代码不能引用客户端类
            ClientHooks.openGameTableScreen(payload.pos());
        });
    }
}