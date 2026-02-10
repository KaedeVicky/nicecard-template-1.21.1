package com.kaedevicky.nicecard.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CardBakedModel implements BakedModel {
    private final CardItemOverrides overrides;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;

    public CardBakedModel(Function<Material, TextureAtlasSprite> spriteGetter, ModelBaker baker, ModelState modelState) {
        // 将 baker 和 state 传递给 Overrides
        this.overrides = new CardItemOverrides(spriteGetter, baker, modelState);
        this.particle = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.withDefaultNamespace("missingno")));

        // 获取原版生成的物品变换（手持、掉落效果）
        // 这样拿在手里就像拿普通物品一样自然
        BakedModel generated = baker.bake(ResourceLocation.withDefaultNamespace("item/generated"), modelState);
        this.transforms = generated != null ? generated.getTransforms() : ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return Collections.emptyList();
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return false; } // false = 像原版物品一样在GUI是平的，在手里有厚度
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public ItemTransforms getTransforms() { return transforms; }
}