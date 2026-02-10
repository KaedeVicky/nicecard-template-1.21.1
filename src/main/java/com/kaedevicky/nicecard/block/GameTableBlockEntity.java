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

// 1. 不再实现 MenuProvider
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

    // --- 游戏逻辑 (保持不变) ---
    public void tryJoin(ServerPlayer player, int slot) {
        if (isGameStarted) return;
        if (!hasBinder(player)) {
            player.sendSystemMessage(Component.literal("§c你需要携带一个 [卡组盒] 才能入座！"));
            return;
        }
        if (player.getUUID().equals(player1) || player.getUUID().equals(player2)) {
            player.sendSystemMessage(Component.literal("§e你已经入座了。"));
            return;
        }
        if (slot == 1 && player1 == null) {
            player1 = player.getUUID();
            player1Name = player.getGameProfile().getName();
            syncData();
        } else if (slot == 2 && player2 == null) {
            player2 = player.getUUID();
            player2Name = player.getGameProfile().getName();
            syncData();
        }
    }

    public void tryStartGame(ServerPlayer requestPlayer) { // 参数其实没用到，或者是用来校验权限的
        // 必须双方都已入座
        if (player1 == null || player2 == null) return;
        if (isGameStarted) return;

        if (level instanceof ServerLevel serverLevel) {
            // 1. 获取玩家实体
            ServerPlayer p1Entity = (ServerPlayer) serverLevel.getPlayerByUUID(player1);
            ServerPlayer p2Entity = (ServerPlayer) serverLevel.getPlayerByUUID(player2);

            // 如果有人离线了，就不能开始
            if (p1Entity == null || p2Entity == null) {
                // 可以发个消息提示 "对方已离线"
                return;
            }

            isGameStarted = true;

            // 2. 初始化 GameData 里的玩家信息 (UUID, 名字)
            gameData.player1 = new PlayerState(player1, player1Name);
            gameData.player2 = new PlayerState(player2, player2Name);

            // 3. 【关键】调用 Controller 进行发牌
            GameController controller = new GameController(gameData);
            // 这里会触发 buildDeck -> createTestDeck -> drawCard
            controller.initGame(p1Entity, p2Entity);

            // 4. 【非常关键】同步数据到客户端！
            // 如果不调用这个，客户端虽然游戏开始了，但不知道自己手牌里有东西
            syncData();
        }
    }

    private boolean hasBinder(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == ModItems.GAME_BINDER.get()) return true;
        }
        return false;
    }

    // --- 数据同步核心 (Server -> Client) ---

    public void syncData() {
        setChanged(); // 标记脏数据，确保存储
        if (level instanceof ServerLevel serverLevel) {
            // 发送数据块更新包，这会触发 getUpdatePacket -> onDataPacket
            serverLevel.getChunkSource().blockChanged(worldPosition);
        }
    }

    // 1. 服务端：决定发给客户端什么数据
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 2. 服务端：把数据写入 NBT (用于 getUpdatePacket)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // 3. 客户端：收到包后怎么处理
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        // 调用 loadAdditional 读取 NBT
        // 注意：在 1.21 中有时需要显式调用 handleUpdateTag，但 onDataPacket 是更底层的入口
        super.onDataPacket(net, pkt, lookupProvider);
        // 这一步非常关键：不用等到下次区块加载，立即读取包里的 tag 更新字段
        // 如果没有这行，客户端可能只有在重进游戏时才会更新
        loadAdditional(pkt.getTag(), lookupProvider);
    }

    // 4. 客户端：处理更新 Tag (备用，某些情况下会走这里)
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        loadAdditional(tag, lookupProvider);
    }

    // --- NBT 保存与读取 (保持不变) ---
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

    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }

    public static void tick(Level level, BlockPos pos, BlockState state, GameTableBlockEntity be) {
        // 游戏循环逻辑...
    }
}