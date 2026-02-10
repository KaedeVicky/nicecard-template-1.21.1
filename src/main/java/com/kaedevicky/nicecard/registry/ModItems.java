package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardCategory;
import com.kaedevicky.nicecard.item.CardBinderItem;
import com.kaedevicky.nicecard.item.CardItem;
import com.kaedevicky.nicecard.item.CardPackItem;
import com.kaedevicky.nicecard.item.GameBinderItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.kaedevicky.nicecard.registry.ModBlocks.GAME_TABLE_BLOCK;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NiceCard.MODID);

    // 基础卡牌物品模板
    // 所有的具体卡牌（爬行者、僵尸等）本质上都是这一个 Item，只是携带的 Data Component 不同
    public static final DeferredItem<CardItem> BASE_CARD = ITEMS.register("card",
            () -> new CardItem(new Item.Properties().stacksTo(64)));
    // 堆叠设为1，因为每张卡可能有不同词条

    // 卡包 (后续实现逻辑)
    public static final DeferredItem<Item> PACK_OVERWORLD = ITEMS.register("pack_overworld",
            () -> new CardPackItem(new Item.Properties(), CardCategory.OVERWORLD));
    public static final DeferredItem<Item> PACK_NETHER = ITEMS.register("pack_nether",
            () -> new CardPackItem(new Item.Properties(), CardCategory.NETHER));
    public static final DeferredItem<Item> PACK_END = ITEMS.register("pack_end",
            () -> new CardPackItem(new Item.Properties(), CardCategory.END));

    // 卡册 (后续实现逻辑)
    public static final DeferredItem<Item> CARD_BINDER = ITEMS.register("card_binder",
            () -> new CardBinderItem(new Item.Properties()));

    public static final Supplier<Item> GAME_TABLE_ITEM = ITEMS.register("game_table",
            () -> new BlockItem(GAME_TABLE_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, GameBinderItem> GAME_BINDER = ITEMS.register("game_binder",
            () -> new GameBinderItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}