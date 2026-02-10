package com.kaedevicky.nicecard.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

public class CardModelGeometry implements IUnbakedGeometry<CardModelGeometry> {

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        // 我们不需要在这里烘焙，因为具体的贴图要在运行时(Overrides里)决定
        // 所以这里返回一个空的包装器即可，关键逻辑全在 CardItemOverrides 里
        return new CardBakedModel(spriteGetter, baker, modelState);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
    }

    public static class Loader implements IGeometryLoader<CardModelGeometry> {
        public static final Loader INSTANCE = new Loader();
        @Override
        public CardModelGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
            return new CardModelGeometry();
        }
    }
}