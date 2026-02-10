package com.kaedevicky.nicecard.block;

import com.kaedevicky.nicecard.game.GameController;
import com.kaedevicky.nicecard.game.GameData;
import com.kaedevicky.nicecard.game.PlayerState;
import com.kaedevicky.nicecard.registry.ModBlocks;
import com.kaedevicky.nicecard.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GameTableBlockEntity extends BlockEntity {

    private UUID player1;
    private String player1Name = "";
    private UUID player2;
    private String player2Name = "";
    public boolean isGameStarted = false;

    public final GameData gameData = new GameData();

    public GameTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.GAME_TABLE_BE.get(), pos, blockState);
    }

    public void tryJoin(ServerPlayer player, int slot) {
        if (isGameStarted) return;
        if (!hasBinder(player)) {
            player.sendSystemMessage(Component.literal("§cYou need a [Game Binder] to join!"));
            return;
        }
        if (player.getUUID().equals(player1) || player.getUUID().equals(player2)) {
            player.sendSystemMessage(Component.literal("§eYou are already seated."));
            return;
        }

        boolean updated = false;
        if (slot == 1 && player1 == null) {
            player1 = player.getUUID();
            player1Name = player.getGameProfile().getName();
            updated = true;
        } else if (slot == 2 && player2 == null) {
            player2 = player.getUUID();
            player2Name = player.getGameProfile().getName();
            updated = true;
        }

        // 关键：只有数据真正改变了才同步，避免无效包
        if (updated) {
            syncData();
        }
    }

    public void tryStartGame(ServerPlayer requestPlayer) {
        if (player1 == null || player2 == null) return;
        if (isGameStarted) return;

        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer p1Entity = (ServerPlayer) serverLevel.getPlayerByUUID(player1);
            ServerPlayer p2Entity = (ServerPlayer) serverLevel.getPlayerByUUID(player2);

            if (p1Entity == null || p2Entity == null) return;

            isGameStarted = true;

            // 初始化游戏数据
            gameData.player1 = new PlayerState(player1, player1Name);
            gameData.player2 = new PlayerState(player2, player2Name);

            GameController controller = new GameController(gameData);
            controller.initGame(p1Entity, p2Entity);

            syncData();
        }
    }

    private boolean hasBinder(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == ModItems.GAME_BINDER.get()) return true;
        }
        return player.getOffhandItem().getItem() == ModItems.GAME_BINDER.get();
    }

    // --- 数据同步核心 ---

    public void syncData() {
        setChanged(); // 标记需要保存到硬盘
        if (level != null) {
            // 发送更新包给所有在此区块加载范围内的玩家
            // 3 = Block.UPDATE_ALL (更新客户端 + 通知邻居)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // 当区块加载或 sendBlockUpdated 时调用
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        // 客户端收到包后，立即读取
        super.onDataPacket(net, pkt, lookupProvider);
        loadAdditional(pkt.getTag(), lookupProvider);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (player1 != null) {
            tag.putUUID("P1", player1);
            tag.putString("P1Name", player1Name);
        }
        if (player2 != null) {
            tag.putUUID("P2", player2);
            tag.putString("P2Name", player2Name);
        }
        tag.putBoolean("Started", isGameStarted);
        tag.put("GameData", gameData.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("P1")) {
            player1 = tag.getUUID("P1");
            player1Name = tag.getString("P1Name");
        } else {
            player1 = null; player1Name = "";
        }

        if (tag.hasUUID("P2")) {
            player2 = tag.getUUID("P2");
            player2Name = tag.getString("P2Name");
        } else {
            player2 = null; player2Name = "";
        }

        isGameStarted = tag.getBoolean("Started");
        if (tag.contains("GameData")) {
            gameData.load(tag.getCompound("GameData"));
        }
    }

    // 【关键】Getter 方法 (供 Screen 实时读取状态)
    public UUID getPlayer1() { return player1; }
    public String getPlayer1Name() { return player1Name; }
    public UUID getPlayer2() { return player2; }
    public String getPlayer2Name() { return player2Name; }

    public static void tick(Level level, BlockPos pos, BlockState state, GameTableBlockEntity be) {
        // 游戏循环逻辑...
    }
}