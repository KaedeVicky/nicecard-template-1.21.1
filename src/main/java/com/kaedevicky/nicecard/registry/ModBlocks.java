package com.kaedevicky.nicecard.registry;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlock;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, NiceCard.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NiceCard.MODID);

    // 注册游戏桌方块
    public static final Supplier<Block> GAME_TABLE_BLOCK = BLOCKS.register("game_table",
            () -> new GameTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f)
                    .noOcclusion()
                    // 如果需要可以加 .requiresCorrectToolForDrops() 等
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
