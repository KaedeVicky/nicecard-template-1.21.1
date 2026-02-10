package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.client.gui.CardBinderMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PacketBinderPage(int page) implements CustomPacketPayload {
    public static final Type<PacketBinderPage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "binder_page"));

    public static final StreamCodec<ByteBuf, PacketBinderPage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PacketBinderPage::page,
            PacketBinderPage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketBinderPage payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof CardBinderMenu binderMenu) {
                binderMenu.setPage(payload.page(), context.player().getInventory());
                // 强制同步容器状态给客户端，防止显示不同步
                binderMenu.broadcastChanges();
            }
        });
    }
}