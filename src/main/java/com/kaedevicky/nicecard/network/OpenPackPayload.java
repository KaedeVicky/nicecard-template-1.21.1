package com.kaedevicky.nicecard.network;

import com.kaedevicky.nicecard.NiceCard;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record OpenPackPayload(List<ItemStack> cards) implements CustomPacketPayload {

    public static final Type<OpenPackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "open_pack"));

    // 使用 NeoForge 的 Codec 自动处理 List<ItemStack> 的序列化
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPackPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.LIST_STREAM_CODEC,
            OpenPackPayload::cards,
            OpenPackPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}