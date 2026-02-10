package com.kaedevicky.nicecard.event;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.manager.CardManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = NiceCard.MODID) // 注意这里是默认的总线(Forge Bus)，不是 MOD 总线
public class CommonModEvents {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        // 将我们的 CardManager 添加到服务端的资源重载监听器列表中
        event.addListener(CardManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(AddReloadListenerEvent event) {
        // 必须注册 INSTANCE，不要 new CardManager()，否则 INSTANCE 里的 map 永远是空的
        event.addListener(CardManager.INSTANCE);
    }
}