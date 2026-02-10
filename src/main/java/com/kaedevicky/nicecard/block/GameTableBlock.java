package com.kaedevicky.nicecard.block;

import com.kaedevicky.nicecard.network.PacketOpenGameTable;
import com.kaedevicky.nicecard.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class GameTableBlock extends BaseEntityBlock {

    // 1. 定义 Codec (这是 1.21+ 必须的)
    public static final MapCodec<GameTableBlock> CODEC = simpleCodec(GameTableBlock::new);

    // 2. 构造函数修改：接收 Properties 参数
    // 标准写法是将属性在注册时传入，而不是硬编码在 super() 里
    public GameTableBlock(Properties properties) {
        super(properties);
    }

    // 3. 实现 codec() 方法
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 发送数据包给玩家，通知他打开界面
            PacketDistributor.sendToPlayer(serverPlayer, new PacketOpenGameTable(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GameTableBlockEntity(pos, state);
    }

    // 确保方块模型能正常渲染（否则会隐形）
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 如果需要Tick更新游戏逻辑
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide) return null; // 逻辑只在服务端运行
        return createTickerHelper(blockEntityType, ModBlocks.GAME_TABLE_BE.get(), GameTableBlockEntity::tick);
    }
}