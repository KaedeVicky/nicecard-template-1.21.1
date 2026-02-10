package com.kaedevicky.nicecard.event;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.client.model.CardModelGeometry;
import com.kaedevicky.nicecard.client.tooltip.CardImageTooltip;
import com.kaedevicky.nicecard.client.tooltip.ClientCardTooltip;
import com.kaedevicky.nicecard.registry.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus; // 引入 IEventBus
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.kaedevicky.nicecard.client.gui.*;

import java.util.Map;

// 移除 @EventBusSubscriber 注解
public class ClientModEvents {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::registerGeometryLoaders);
        modEventBus.addListener(ClientModEvents::registerTooltipFactories);
        modEventBus.addListener(ClientModEvents::registerScreens);
    }

    // 保持原来的静态方法逻辑不变，只是去掉了 @SubscribeEvent (对于手动注册，@SubscribeEvent 其实是可选的，但加上也没事，为了整洁通常可以保留或去掉，addListener 会自动识别)
    // 这里的参数不需要变
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "card_loader"), CardModelGeometry.Loader.INSTANCE);
    }

    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        // 告诉游戏：如果遇到 CardImageTooltip 数据，请用 ClientCardTooltip 来渲染
        event.register(CardImageTooltip.class, ClientCardTooltip::new);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.CARD_BINDER_MENU.get(), CardBinderScreen::new);
        event.register(ModMenuTypes.GAME_BINDER_MENU.get(), GameBinderScreen::new);
    }

}