package com.kaedevicky.nicecard.game;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class GameData {
    // 两个玩家的状态
    // 初始化为空对象，防止空指针，等玩家加入时再覆盖或填充
    public PlayerState player1 = new PlayerState(null, "");
    public PlayerState player2 = new PlayerState(null, "");

    // 全局状态
    public int turnCount = 0;       // 回合数
    public int currentPlayer = 1;   // 当前回合玩家: 1 或 2
    public boolean isGameOver = false;
    public int winner = 0;          // 0:无, 1:P1, 2:P2

    public GameData() {}

    // --- 辅助方法 ---

    // 获取当前回合的玩家对象
    public PlayerState getCurrentPlayerState() {
        return currentPlayer == 1 ? player1 : player2;
    }

    // 获取对手玩家对象
    public PlayerState getOpponentState() {
        return currentPlayer == 1 ? player2 : player1;
    }

    // 切换回合
    public void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        turnCount++;
    }

    // --- NBT 序列化 (存盘/同步用) ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Turn", turnCount);
        tag.putInt("CurrentPlayer", currentPlayer);
        tag.putBoolean("GameOver", isGameOver);
        tag.putInt("Winner", winner);

        // 递归保存两个玩家的状态
        tag.put("P1", player1.save());
        tag.put("P2", player2.save());

        return tag;
    }

    public void load(CompoundTag tag) {
        this.turnCount = tag.getInt("Turn");
        this.currentPlayer = tag.getInt("CurrentPlayer");
        this.isGameOver = tag.getBoolean("GameOver");
        this.winner = tag.getInt("Winner");

        if (tag.contains("P1")) player1.load(tag.getCompound("P1"));
        if (tag.contains("P2")) player2.load(tag.getCompound("P2"));
    }
}