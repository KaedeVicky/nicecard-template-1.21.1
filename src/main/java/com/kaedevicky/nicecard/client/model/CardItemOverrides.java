package com.kaedevicky.nicecard.client.model;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CardItemOverrides extends ItemOverrides {

    private final Map<String, BakedModel> cache = new ConcurrentHashMap<>();
    private final Function<Material, TextureAtlasSprite> spriteGetter;
    private final ModelBaker baker;
    private final ModelState modelState;

    private final ItemModelGenerator itemModelGenerator = new ItemModelGenerator();
    private final FaceBakery faceBakery = new FaceBakery();

    private static final float THICKNESS_MULTIPLIER = 1f;

    public CardItemOverrides(Function<Material, TextureAtlasSprite> spriteGetter, ModelBaker baker, ModelState modelState) {
        this.spriteGetter = spriteGetter;
        this.baker = baker;
        this.modelState = modelState;
    }

    @Nullable
    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
        if (def == null) return model;

        String cacheKey = def.id();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // --- 1. 准备 Sprite ---
        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "item/card/bg/" + def.category().name().toLowerCase());

        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath;
        if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        if (fileName.endsWith(".png")) fileName = fileName.substring(0, fileName.length() - 4);
        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "item/card/art/" + fileName);

        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "item/card/border/" + def.rarity().name().toLowerCase());

        TextureAtlasSprite bgSprite = getSprite(bgLoc);
        TextureAtlasSprite artSprite = getSprite(artLoc);
        TextureAtlasSprite borderSprite = getSprite(borderLoc);

        // --- 2. 获取原版物品的显示变换 (关键！解决掉落物位置问题) ---
        // 我们借用 "item/generated" 的显示参数，它定义了物品在手持、掉落、展示框中的标准位置
        BakedModel generatedModel = baker.bake(ResourceLocation.withDefaultNamespace("item/generated"), modelState);
        ItemTransforms transforms = generatedModel != null ? generatedModel.getTransforms() : ItemTransforms.NO_TRANSFORMS;

        // --- 3. 生成几何体 ---
        // 分别生成三层
        List<BlockElement> bgElements = itemModelGenerator.processFrames(0, "layer0", bgSprite.contents());
        List<BlockElement> artElements = itemModelGenerator.processFrames(1, "layer1", artSprite.contents());
        List<BlockElement> borderElements = itemModelGenerator.processFrames(2, "layer2", borderSprite.contents());

        // --- 4. 物理加厚 (核心修改) ---
        // 不再使用矩阵变换，而是直接修改方块的坐标数据
        // 这确保了模型是物理居中的，不会绕着奇怪的点转
        resizeElements(bgElements);
        resizeElements(artElements);
        resizeElements(borderElements);

        // --- 5. 构建模型 ---
        SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(
                true, true, false,
                transforms, // 使用正确的显示参数
                ItemOverrides.EMPTY
        );

        // 烘焙 (使用原始的 modelState，因为坐标已经手动改好了)
        bakeElements(bgElements, bgSprite, modelState, builder);
        bakeElements(artElements, artSprite, modelState, builder);
        bakeElements(borderElements, borderSprite, modelState, builder);

        builder.particle(bgSprite);

        BakedModel finalModel = builder.build();
        cache.put(cacheKey, finalModel);
        return finalModel;
    }

    /**
     * 核心方法：直接修改元素的 Z 轴坐标来实现加厚
     * 强制以 Z=8.0 (像素中心) 为基准进行缩放
     */
    private void resizeElements(List<BlockElement> elements) {
        for (BlockElement element : elements) {
            // 原理：新坐标 = 中心点 + (旧坐标 - 中心点) * 倍数
            // Minecraft 像素坐标中心是 8.0f

            element.from.z = 8.0f + (element.from.z - 8.0f) * THICKNESS_MULTIPLIER;
            element.to.z = 8.0f + (element.to.z - 8.0f) * THICKNESS_MULTIPLIER;
        }
    }

    private void bakeElements(List<BlockElement> elements, TextureAtlasSprite sprite, ModelState state, SimpleBakedModel.Builder builder) {
        for (BlockElement element : elements) {
            for (Direction direction : element.faces.keySet()) {
                BlockElementFace face = element.faces.get(direction);
                BakedQuad quad = faceBakery.bakeQuad(
                        element.from, element.to,
                        face,
                        sprite,
                        direction,
                        state,
                        element.rotation,
                        true
                );
                builder.addUnculledFace(quad);
            }
        }
    }

    private TextureAtlasSprite getSprite(ResourceLocation loc) {
        return spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, loc));
    }
}