package com.kaedevicky.nicecard.game;

import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.card.CardType;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import com.kaedevicky.nicecard.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameController {

    private final GameData gameData;
    private final Random random = new Random();

    public GameController(GameData data) {
        this.gameData = data;
    }

    // --- 1. 游戏初始化 ---
    public void initGame(ServerPlayer p1, ServerPlayer p2) {
        // 1. 从玩家背包读取卡册，构建牌库
        gameData.player1.deck = buildDeckFromBinder(p1);
        gameData.player2.deck = buildDeckFromBinder(p2);

        if (gameData.player1.deck.isEmpty()) {
            System.out.println("P1 牌库为空，发放测试套牌");
            gameData.player1.deck = createTestDeck();
        }
        if (gameData.player2.deck.isEmpty()) {
            System.out.println("P2 牌库为空，发放测试套牌");
            gameData.player2.deck = createTestDeck();
        }

        // 2. 洗牌
        Collections.shuffle(gameData.player1.deck, random);
        Collections.shuffle(gameData.player2.deck, random);

        // 3. 发初始手牌 (双方3张)
        for (int i = 0; i < 3; i++) {
            gameData.player1.drawCard();
            gameData.player2.drawCard();
        }

        // 4. 设置先手
        gameData.currentPlayer = 1;
        gameData.player1.startTurn();
    }



    private List<CardInstance> buildDeckFromBinder(ServerPlayer player) {
        List<CardInstance> newDeck = new ArrayList<>();

        // A. 在玩家背包里找卡册
        ItemStack binderStack = findBinder(player);

        if (binderStack.isEmpty()) {
            // 如果没带卡册，给一套“测试套牌” (防止游戏崩溃)
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你没带卡册！使用默认测试套牌。"));
            return createTestDeck();
        }

        // B. 读取卡册里的卡牌
        // 尝试使用 NeoForge 的 Capability 系统读取库存 (推荐)
        Level level = player.level();
        IItemHandler handler = binderStack.getCapability(Capabilities.ItemHandler.ITEM, null);

        if (handler != null) {
            // 方式 1: 通过 Capability 读取 (首选)
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                addCardFromStack(newDeck, stack);
            }
        } else {
            // 方式 2: 如果没有 Capability，尝试读取 CustomData (旧 NBT 回退方案)
            // 在 1.21 中，原来的 NBT 标签现在存储在 DataComponents.CUSTOM_DATA 里

            // 1. 获取自定义数据组件
            CustomData customData = binderStack.get(DataComponents.CUSTOM_DATA);

            // 2. 检查是否存在且包含 "Items" 键
            if (customData != null && customData.contains("Items")) {
                // 3. 将 CustomData 复制为 CompoundTag 以便读取
                CompoundTag tag = customData.copyTag();

                // 4. 使用 ContainerHelper 读取 (需要预设足够大的列表，假设卡册最大27格)
                NonNullList<ItemStack> items = NonNullList.withSize(30, ItemStack.EMPTY);

                // 传入 registryAccess 是 1.21 的新要求
                ContainerHelper.loadAllItems(tag, items, player.registryAccess());

                // 5. 将读出的物品加入牌库
                for (ItemStack stack : items) {
                    addCardFromStack(newDeck, stack);
                }
            }
        }

        // 如果牌库是空的 (比如卡册是空的)，也给个测试套牌
        if (newDeck.isEmpty()) {
            return createTestDeck();
        }

        return newDeck;
    }

    private void addCardFromStack(List<CardInstance> deck, ItemStack stack) {
        if (stack.isEmpty()) return;

        // 检查这个物品是不是卡牌
        // 假设卡牌物品里存了 CardDefinition 数据组件
        // 在 1.21 里，我们用 DataComponents
        if (stack.has(ModDataComponents.CARD_DEF)) {
            CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
            if (def != null) {
                // 有几张就加几张 (堆叠数量)
                for (int k = 0; k < stack.getCount(); k++) {
                    deck.add(new CardInstance(def));
                }
            }
        }
    }

    private ItemStack findBinder(ServerPlayer player) {
        // 遍历主背包
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack s = player.getInventory().items.get(i);
            if (s.is(ModItems.GAME_BINDER.get())) {
                return s;
            }
        }
        // 遍历副手
        if (player.getOffhandItem().is(ModItems.GAME_BINDER.get())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    // 辅助：创建测试套牌 (全由 Creeper 组成)
    private List<CardInstance> createTestDeck() {
        List<CardInstance> deck = new ArrayList<>();

        // 从 CardManager 获取一张已存在的卡牌定义
        // 请确保 "nicecard:creeper" 这个ID是你 data/nicecard/cards/creeper.json 里定义的 ID
        // 如果你不确定 ID 是什么，去检查一下你的 json 文件名或者 CardManager 加载时的日志
        CardDefinition testCard = com.kaedevicky.nicecard.manager.CardManager.INSTANCE.getCardById("nicecard:creeper");

        if (testCard == null) {
            // 如果连 creeper 都没加载成功，那就没办法了，打印个错误
            System.err.println("【严重错误】无法找到测试卡牌 'nicecard:creeper'！请检查 JSON 是否加载成功。");
            // 为了防止崩服，还是返回空列表
            return deck;
        }

        // 塞入 10 张同样的卡
        for (int i = 0; i < 10; i++) {
            deck.add(new CardInstance(testCard));
        }

        return deck;
    }

    // --- 2. 出牌逻辑 ---
    public boolean playCard(int playerIndex, int handSlotIndex, int targetIndex) {
        // 获取当前行动的玩家对象
        PlayerState player = (playerIndex == 1) ? gameData.player1 : gameData.player2;

        // 检查：是否是该玩家的回合
        if (playerIndex != gameData.currentPlayer) return false;

        // 检查：手牌索引是否合法
        if (handSlotIndex < 0 || handSlotIndex >= player.hand.size()) return false;

        CardInstance card = player.hand.get(handSlotIndex);

        // 检查并消耗法力值 (spendMana方法需要在 PlayerState 里实现)
        if (!player.spendMana(card.getCurrentCost())) {
            return false;
        }

        // 从手牌移除
        player.hand.remove(handSlotIndex);

        // A. 随从逻辑
        if (card.getDefinition().type() == CardType.MINION) {
            if (player.board.size() >= 7) return false; // 满场不能下

            player.board.add(card);

            // TODO: 这里触发战吼 AbilityRegistry.execute(...)
        }
        // B. 法术逻辑
        else {
            // TODO: 这里执行法术效果
            player.graveyard.add(card);
        }

        return true;
    }

    // --- 3. 随从攻击逻辑 ---
    public void minionAttack(int playerIndex, int attackerIndex, int targetIndex, boolean targetIsFace) {
        PlayerState attackerPlayer = (playerIndex == 1) ? gameData.player1 : gameData.player2;
        PlayerState defenderPlayer = (playerIndex == 1) ? gameData.player2 : gameData.player1;

        if (attackerIndex < 0 || attackerIndex >= attackerPlayer.board.size()) return;
        CardInstance attacker = attackerPlayer.board.get(attackerIndex);

        // 检查攻击权
        if (!attacker.canAttack()) return;

        // 检查嘲讽 (嘲讽逻辑)
        if (!targetIsFace && hasTauntMinion(defenderPlayer)) {
            // 这里简化了：只要对面有嘲讽，且你攻击的不是脸，暂时放行
            // 严谨逻辑应该是：如果你攻击的目标没有嘲讽，但场上有其他嘲讽怪，则拦截
            CardInstance target = defenderPlayer.board.get(targetIndex);
            if (!target.hasTaunt()) return; // 必须攻击嘲讽怪
        } else if (targetIsFace && hasTauntMinion(defenderPlayer)) {
            return; // 有嘲讽不能打脸
        }

        // 标记已攻击
        attacker.setHasAttacked(true);

        if (targetIsFace) {
            // 打脸
            defenderPlayer.takeDamage(attacker.getAttack());
        } else {
            // 随从交换
            if (targetIndex < 0 || targetIndex >= defenderPlayer.board.size()) return;
            CardInstance defender = defenderPlayer.board.get(targetIndex);

            defender.takeDamage(attacker.getAttack());
            attacker.takeDamage(defender.getAttack());

            // 结算死亡
            resolveDeaths(attackerPlayer);
            resolveDeaths(defenderPlayer);
        }
    }

    // --- 4. 结束回合 ---
    public void endTurn() {
        // 切换 ID
        gameData.currentPlayer = (gameData.currentPlayer == 1) ? 2 : 1;
        gameData.turnCount++;

        // 下一位玩家回合开始
        PlayerState nextPlayer = (gameData.currentPlayer == 1) ? gameData.player1 : gameData.player2;
        nextPlayer.startTurn();
    }

    // 辅助：是否有嘲讽
    private boolean hasTauntMinion(PlayerState player) {
        for (CardInstance m : player.board) {
            if (m.hasTaunt()) return true;
        }
        return false;
    }

    // 辅助：结算死亡
    private void resolveDeaths(PlayerState player) {
        player.board.removeIf(minion -> {
            if (minion.getHealth() <= 0) {
                player.graveyard.add(minion);
                // TODO: 触发亡语
                return true;
            }
            return false;
        });
    }
}