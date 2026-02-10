package com.kaedevicky.nicecard.client;

import com.kaedevicky.nicecard.card.CardDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端专用的卡牌管理器
 * 它的数据来源不是 JSON 文件，而是服务端发的网络包
 */
public class ClientCardManager {
    public static final ClientCardManager INSTANCE = new ClientCardManager();

    private final Map<String, CardDefinition> syncedCards = new HashMap<>();

    // 接收网络包时调用此方法更新数据
    public void updateCards(Collection<CardDefinition> cards) {
        this.syncedCards.clear();
        for (CardDefinition card : cards) {
            this.syncedCards.put(card.id(), card);
        }
    }

    public Collection<CardDefinition> getCards() {
        return Collections.unmodifiableCollection(syncedCards.values());
    }
}