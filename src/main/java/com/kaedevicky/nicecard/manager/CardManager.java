package com.kaedevicky.nicecard.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kaedevicky.nicecard.card.CardCategory;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.*;

public class CardManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    // 单例模式，方便全局访问（注意：在服务端和客户端分别会有不同的数据状态，后续需要网络同步）
    public static final CardManager INSTANCE = new CardManager();

    // 缓存加载后的卡牌
    private final Map<ResourceLocation, CardDefinition> cards = new HashMap<>();

    public CardManager() {
        // 指定读取 data/modid/cards 目录
        super(GSON, "cards");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.cards.clear();
        LOGGER.info("Starting to load NiceCards...");

        object.forEach((location, json) -> {
            try {
                // 使用我们在 CardDefinition 中定义的 CODEC 将 JSON 解析为 Java 对象
                CardDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> LOGGER.error("Failed to parse card JSON: {} - {}", location, error))
                        .ifPresent(cardDefinition -> {
                            // 这里的 location 是文件名，比如 nicecard:creeper
                            // 我们可以把 ID 覆盖为文件名，确保一致性
                            CardDefinition finalCard = new CardDefinition(
                                    location.toString(), // 使用文件路径作为 ID
                                    cardDefinition.name(),
                                    cardDefinition.rarity(),
                                    cardDefinition.category(),
                                    cardDefinition.type(),
                                    cardDefinition.textureLocation(),
                                    cardDefinition.cost(),
                                    cardDefinition.damage(),
                                    cardDefinition.health(),
                                    cardDefinition.description(),
                                    cardDefinition.keywords()
                            );

                            cards.put(location, finalCard);
                        });
            } catch (Exception e) {
                LOGGER.error("Exception loading card: {}", location, e);
            }
        });

        LOGGER.info("Loaded {} NiceCards.", cards.size());
    }

    /**
     * 获取所有加载的卡牌
     */
    public Collection<CardDefinition> getAllCards() {
        return cards.values();
    }

    /**
     * 根据 ID 获取特定卡牌
     */
    public Optional<CardDefinition> getCard(ResourceLocation id) {
        return Optional.ofNullable(cards.get(id));
    }

    public List<CardDefinition> getCardsByCategory(CardCategory category) {
        if (cards == null || cards.isEmpty()) return List.of();

        // 使用 Stream 筛选出符合 Category 的卡牌
        // 建议：如果你非常在意性能，可以在 apply 加载时就预先分类存好 Map<Category, List<CardDefinition>>
        // 但对于几百张卡牌的规模，直接 Stream 过滤也是毫秒级的，完全够用。
        return cards.values().stream()
                .filter(def -> def.category() == category)
                .toList();
    }

    public CardDefinition getCardById(String id) {
        if (id == null || id.isEmpty()) return null;

        // 把 String (例如 "nicecard:creeper") 转回 ResourceLocation
        ResourceLocation loc = ResourceLocation.tryParse(id);

        // 如果转换失败（比如格式不对），或者找不到卡牌，直接返回 null
        if (loc == null) return null;

        return cards.get(loc);
    }
}