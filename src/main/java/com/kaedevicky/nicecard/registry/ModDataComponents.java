package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries; // 必须导入这个
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDataComponents {
    // 修改点 1: 使用标准的 create 方法，传入 Registries.DATA_COMPONENT_TYPE
    public static final DeferredRegister<DataComponentType<?>> REGISTRAR =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NiceCard.MODID);

    // 存储卡牌的静态定义
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CardDefinition>> CARD_DEF = REGISTRAR.register("card_definition",
            () -> DataComponentType.<CardDefinition>builder()
                    .persistent(CardDefinition.CODEC) // 负责保存到 NBT (磁盘)
                    // 修改点 2: 使用 ByteBufCodecs.fromCodec 将 Codec 转换为 StreamCodec 用于网络同步
                    .networkSynchronized(ByteBufCodecs.fromCodec(CardDefinition.CODEC))
                    .build());

    // 存储词条列表 (List<String>)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> AFFIXES = REGISTRAR.register("affixes",
            () -> DataComponentType.<List<String>>builder()
                    .persistent(com.mojang.serialization.Codec.STRING.listOf())
                    // 修改点 3: 集合类型的网络同步标准写法
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRAR.register(eventBus);
    }
}