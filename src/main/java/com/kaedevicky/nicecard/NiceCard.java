package com.kaedevicky.nicecard;

import com.kaedevicky.nicecard.event.ClientModEvents;
import com.kaedevicky.nicecard.network.PacketHandler;
import com.kaedevicky.nicecard.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(NiceCard.MODID)
public class NiceCard {
    public static final String MODID = "nicecard";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NiceCard(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // 1. 注册核心组件 (顺序很重要：Data -> Items/Blocks -> Tabs -> Menus)
        ModDataComponents.register(modEventBus); // 数据组件最先
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModTabs.register(modEventBus);           // 创造栏依赖 Items
        ModMenuTypes.register(modEventBus);

        // 2. 注册网络包
        modEventBus.addListener(PacketHandler::register);

        // 3. 注册客户端事件 (仅在客户端运行)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModEvents.register(modEventBus);
        }

        // 4. 注册通用事件总线
        NeoForge.EVENT_BUS.register(this);

        // 5. 注册配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("NiceCard Common Setup Complete.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NiceCard Server Starting...");
    }
}