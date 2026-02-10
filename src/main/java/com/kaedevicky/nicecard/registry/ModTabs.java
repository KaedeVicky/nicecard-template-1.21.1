package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.client.ClientCardManager;
import com.kaedevicky.nicecard.item.CardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.List;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NiceCard.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NICECARD_TAB = CREATIVE_MODE_TABS.register("nicecard_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nicecard"))
                    .icon(() -> ModItems.PACK_OVERWORLD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // 1. 添加固定物品
                        output.accept(ModItems.PACK_OVERWORLD.get());
                        output.accept(ModItems.PACK_NETHER.get());
                        output.accept(ModItems.PACK_END.get());
                        output.accept(ModItems.CARD_BINDER.get());
                        output.accept(ModItems.GAME_TABLE_ITEM.get());
                        output.accept(ModItems.GAME_BINDER.get());

                        // 2. 动态添加卡牌
                        // 创造模式栏是在客户端构建的，所以我们从 ClientCardManager 获取数据
                        Collection<CardDefinition> cards = ClientCardManager.INSTANCE.getCards();

                        for (CardDefinition def : cards) {
                            // 使用我们在 CardItem 里写的辅助方法创建 ItemStack
                            output.accept(CardItem.createCard(def));
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}