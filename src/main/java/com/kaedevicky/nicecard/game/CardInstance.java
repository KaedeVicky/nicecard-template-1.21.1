package com.kaedevicky.nicecard.game;

import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.manager.CardManager; // 引用你的 CardManager
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class CardInstance {
    // 核心身份
    private UUID uuid;
    private CardDefinition definition;

    // 动态属性
    private int currentCost;
    private int currentAttack;
    private int maxHealth;
    private int damageTaken; // 当前血量 = maxHealth - damageTaken

    // 状态
    private boolean isSleep;
    private boolean hasAttacked;
    // private boolean hasTaunt; // 可以在这里存，也可以直接读 definition

    // =========================================================
    // 1. 公开构造函数：用于【新创建】一张卡牌
    // =========================================================
    public CardInstance(CardDefinition def) {
        this.uuid = UUID.randomUUID(); // 生成新 ID
        this.definition = def;

        // 初始化数值 (满状态)
        this.currentCost = def.cost();
        this.currentAttack = def.damage();
        this.maxHealth = def.health();
        this.damageTaken = 0;

        // 初始化状态 (刚进场会有睡眠)
        this.isSleep = true;
        this.hasAttacked = false;
    }

    // =========================================================
    // 2. 私有构造函数：用于【读取】存档
    // =========================================================
    // 这个构造函数只做最基本的赋值，不做任何初始化逻辑
    private CardInstance(CardDefinition def, UUID uuid) {
        this.definition = def;
        this.uuid = uuid;
    }

    // =========================================================
    // 3. NBT 序列化与反序列化
    // =========================================================

    // 保存到 NBT
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("UUID", uuid);
        // 保存 ID 字符串，例如 "nicecard:creeper"
        // 这里的 ID 我们用 CardManager 里的 key 或者 textureLocation 都可以，
        // 但最稳妥的是你在 CardDefinition 里存了一个 id 字段。
        // 如果没有 id 字段，我们可以暂时用 textureLocation.toString() 代替，或者你在 CardDefinition 里加一个 id()
        // 假设这里我们存的是 textureLocation 对应的字符串 (或者你需要给 CardDefinition 加一个 getId())
        tag.putString("DefId", definition.textureLocation().toString()); // ⚠️注意：确保这个能唯一对应

        tag.putInt("Cost", currentCost);
        tag.putInt("Atk", currentAttack);
        tag.putInt("MaxHP", maxHealth);
        tag.putInt("Dmg", damageTaken);

        tag.putBoolean("Sleep", isSleep);
        tag.putBoolean("Attacked", hasAttacked);
        return tag;
    }

    // 从 NBT 读取 (静态工厂方法)
    public static CardInstance load(CompoundTag tag) {
        // 1. 获取 ID 字符串
        String defIdString = tag.getString("DefId");
        ResourceLocation id = ResourceLocation.tryParse(defIdString);

        if (id == null) {
            System.err.println("卡牌 ID 格式错误: " + defIdString);
            return null;
        }

        // 2. 从 Manager 查找定义
        // 你的 CardManager 有 getCard(ResourceLocation) 方法
        CardDefinition def = CardManager.INSTANCE.getCard(id).orElse(null);

        if (def == null) {
            // 如果原来的卡被删了，或者 mod 卸载了，这里会找不到
            System.err.println("未知卡牌，无法加载: " + id);
            return null;
        }

        // 3. 获取 UUID
        UUID savedUuid;
        if (tag.hasUUID("UUID")) {
            savedUuid = tag.getUUID("UUID");
        } else {
            savedUuid = UUID.randomUUID(); //以此防崩，如果存档坏了就生成新的
        }

        // 4. 【核心】调用私有构造函数
        // 这里我们只传 def 和 uuid，其他属性下面手动覆盖
        CardInstance card = new CardInstance(def, savedUuid);

        // 5. 恢复数值状态
        card.currentCost = tag.getInt("Cost");
        card.currentAttack = tag.getInt("Atk");
        card.maxHealth = tag.getInt("MaxHP");
        card.damageTaken = tag.getInt("Dmg");

        card.isSleep = tag.getBoolean("Sleep");
        card.hasAttacked = tag.getBoolean("Attacked");

        return card;
    }

    // Getters
    public CardDefinition getDefinition() { return definition; }
    public int getHealth() { return maxHealth - damageTaken; }
    public int getAttack() {
        return currentAttack;
    }
    // ... 其他 getter

    // 在 CardInstance 类中添加：

    // 1. 获取当前费用
    public int getCurrentCost() {
        return currentCost;
    }

    // 2. 能否攻击
    public boolean canAttack() {
        // 攻击力>0 且 没睡 且 没攻击过
        return currentAttack > 0 && !isSleep && !hasAttacked;
    }

    // 3. 设置攻击状态
    public void setHasAttacked(boolean hasAttacked) {
        this.hasAttacked = hasAttacked;
    }

    // 4. 回合开始重置
    public void onTurnStart() {
        this.isSleep = false;     // 解除召唤失调
        this.hasAttacked = false; // 重置攻击次数
    }

    // 5. 是否有嘲讽 (需要你在 CardKeyword 枚举里有 TAUNT)
    public boolean hasTaunt() {
        // 检查原始定义里的关键字
        if (definition.keywords() != null) {
            for (var k : definition.keywords()) {
                if (k.name().equalsIgnoreCase("taunt")) return true;
            }
        }
        return false;
        // 或者使用: return definition.keywords().contains(CardKeyword.TAUNT);
    }

    // 6. 受伤
    public void takeDamage(int amount) {
        this.damageTaken += amount;
    }


}