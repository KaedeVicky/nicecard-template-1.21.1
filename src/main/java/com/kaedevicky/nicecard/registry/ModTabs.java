package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.client.ClientCardManager;
import com.kaedevicky.nicecard.item.CardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
                        // 确保 GAME_TABLE_ITEM 不为空再添加
                        if (ModItems.GAME_TABLE_ITEM != null && ModItems.GAME_TABLE_ITEM.get() != null) {
                            output.accept(ModItems.GAME_TABLE_ITEM.get());
                        }
                        output.accept(ModItems.GAME_BINDER.get());

                        // 2. 动态添加卡牌
                        Collection<CardDefinition> cards = ClientCardManager.INSTANCE.getCards();

                        // 【调试日志】
                        NiceCard.LOGGER.info("ModTabs: Loading Creative Tab items. Card count: " + cards.size());

                        if (!cards.isEmpty()) {
                            List<CardDefinition> sortedCards = new ArrayList<>(cards);
                            // 按 ID 排序，方便查找
                            sortedCards.sort(Comparator.comparing(CardDefinition::id));

                            for (CardDefinition def : sortedCards) {
                                ItemStack stack = CardItem.createCard(def);
                                output.accept(stack);
                            }
                        } else {
                            // 如果是空的，我们可以加一个占位符，提示玩家数据未加载
                            // 或者不做任何事
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}