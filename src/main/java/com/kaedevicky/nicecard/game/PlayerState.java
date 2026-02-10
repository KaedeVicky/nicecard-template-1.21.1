package com.kaedevicky.nicecard.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerState {
    public UUID uuid;
    public String name;

    public int health = 30;
    public int maxHealth = 30;
    public int maxMana = 0;
    public int currentMana = 0;

    @Nullable
    public WeaponInstance weapon; // 如果为 null，表示没有武器
    public boolean isHeroPowerUsed = false; // 本回合是否使用了技能

    // 核心区域
    public List<CardInstance> deck = new ArrayList<>();
    public List<CardInstance> hand = new ArrayList<>();
    public List<CardInstance> board = new ArrayList<>();
    public List<CardInstance> graveyard = new ArrayList<>();

    public PlayerState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    // --- NBT 序列化 ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (uuid != null) tag.putUUID("UUID", uuid);
        if (name != null) tag.putString("Name", name);

        tag.putInt("HP", health);
        tag.putInt("MaxHP", maxHealth);
        tag.putInt("Mana", currentMana);
        tag.putInt("MaxMana", maxMana);

        // 【新增 3】 保存武器和技能状态
        if (this.weapon != null) {
            tag.put("Weapon", this.weapon.save());
        }
        tag.putBoolean("PowerUsed", this.isHeroPowerUsed);

        // 保存列表 (Deck, Hand, Board)
        tag.put("Deck", saveCardList(deck));
        tag.put("Hand", saveCardList(hand));
        tag.put("Board", saveCardList(board));
        tag.put("Grave", saveCardList(graveyard));

        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID("UUID")) this.uuid = tag.getUUID("UUID");
        if (tag.contains("Name")) this.name = tag.getString("Name");

        this.health = tag.getInt("HP");
        this.maxHealth = tag.getInt("MaxHP");
        this.currentMana = tag.getInt("Mana");
        this.maxMana = tag.getInt("MaxMana");

        // 【新增 4】 读取武器和技能状态
        if (tag.contains("Weapon")) {
            this.weapon = WeaponInstance.load(tag.getCompound("Weapon"));
        } else {
            this.weapon = null; // 确保清空
        }
        this.isHeroPowerUsed = tag.getBoolean("PowerUsed");

        // 读取列表
        this.deck = loadCardList(tag.getList("Deck", Tag.TAG_COMPOUND));
        this.hand = loadCardList(tag.getList("Hand", Tag.TAG_COMPOUND));
        this.board = loadCardList(tag.getList("Board", Tag.TAG_COMPOUND));
        this.graveyard = loadCardList(tag.getList("Grave", Tag.TAG_COMPOUND));
    }

    // 辅助：保存卡牌列表
    private ListTag saveCardList(List<CardInstance> list) {
        ListTag listTag = new ListTag();
        for (CardInstance card : list) {
            listTag.add(card.save());
        }
        return listTag;
    }

    // 辅助：读取卡牌列表
    private List<CardInstance> loadCardList(ListTag listTag) {
        List<CardInstance> list = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = listTag.getCompound(i);
            CardInstance card = CardInstance.load(tag);
            if (card != null) list.add(card);
        }
        return list;
    }

    // 在 PlayerState 类中添加：

    // 1. 抽牌
    public void drawCard() {
        if (deck.isEmpty()) {
            // 疲劳扣血逻辑 (暂时略)
            return;
        }
        CardInstance card = deck.remove(0);
        if (hand.size() < 10) {
            hand.add(card);
        } else {
            // 爆牌
            graveyard.add(card);
        }
    }

    // 2. 消耗法力
    public boolean spendMana(int amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            return true;
        }
        return false;
    }

    // 3. 回合开始
    public void startTurn() {
        // 法力上限+1 (最高10)
        if (maxMana < 10) maxMana++;
        // 补满法力
        currentMana = maxMana;

        // 【新增 2】 重置技能状态
        this.isHeroPowerUsed = false;

        // 抽一张牌
        drawCard();

        // 唤醒所有随从 (解除召唤失调)
        for (CardInstance minion : board) {
            minion.onTurnStart();
        }
    }

    // 4. 受伤
    public void takeDamage(int amount) {
        this.health -= amount;
        // if (this.health <= 0) gameOver();
    }
}