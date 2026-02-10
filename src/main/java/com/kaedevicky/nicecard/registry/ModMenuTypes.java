package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.client.gui.CardBinderMenu;
import com.kaedevicky.nicecard.client.gui.GameBinderMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NiceCard.MODID);

    // 注册卡册菜单
    public static final DeferredHolder<MenuType<?>, MenuType<CardBinderMenu>> CARD_BINDER_MENU =
            MENUS.register("card_binder", () -> IMenuTypeExtension.create(CardBinderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GameBinderMenu>> GAME_BINDER_MENU =
            MENUS.register("game_binder", () -> IMenuTypeExtension.create(GameBinderMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}