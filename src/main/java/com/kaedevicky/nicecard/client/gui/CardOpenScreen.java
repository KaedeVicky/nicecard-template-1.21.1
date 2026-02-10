package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.card.CardRarity;
import com.kaedevicky.nicecard.network.PacketClaimRewards;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CardOpenScreen extends Screen {

    private final List<ItemStack> cards;
    private final boolean[] isFlipped;
    private final float[] flipProgress;

    private boolean packOpened = false;
    private float openAnimationProgress = 0.0f;
    private int hoveredIndex = -1;

    private final int maxRarityColor;

    // 资源路径
    private static final ResourceLocation PACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_pack.png");
    private static final ResourceLocation CARD_BACK = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_back_high_res.png");
    private static final ResourceLocation PACK_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/pack_glow.png");

    private static final float[][] PENTAGON_VERTICES = {
            {0.0f, -1.0f},
            {0.951f, -0.309f},
            {0.588f, 0.809f},
            {-0.588f, 0.809f},
            {-0.951f, -0.309f}
    };

    private boolean hasMovedMouse = false;
    private final long creationTime;

    public CardOpenScreen(List<ItemStack> cards) {
        super(Component.literal("Card Opening"));
        this.cards = cards;
        this.isFlipped = new boolean[cards.size()];
        this.flipProgress = new float[cards.size()];
        this.maxRarityColor = calculateMaxRarityColor(cards);
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        if (!hasMovedMouse) {
            hasMovedMouse = true;
            long window = this.minecraft.getWindow().getWindow();
            double scale = this.minecraft.getWindow().getGuiScale();
            double targetX = (this.width / 2.0) * scale;
            double targetY = (this.height / 2.0 + 120.0) * scale;
            GLFW.glfwSetCursorPos(window, targetX, targetY);
        }
    }

    private int calculateMaxRarityColor(List<ItemStack> cards) {
        CardRarity maxRarity = CardRarity.COMMON;
        for (ItemStack stack : cards) {
            CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
            if (def != null && def.rarity().ordinal() > maxRarity.ordinal()) {
                maxRarity = def.rarity();
            }
        }
        return switch (maxRarity) {
            case COMMON -> 0xFFFFFF;
            case RARE -> 0x00AAFF;
            case EPIC -> 0xAA00FF;
            case LEGENDARY -> 0xFFAA00;
            default -> 0xFFFFFF;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        float screenCenterX = this.width / 2.0f;
        float screenCenterY = this.height / 2.0f;

        float minScreenDimension = Math.min(this.width, this.height);

        // 【修改 1】大幅增加布局半径，解决间距短的问题
        // 0.35f -> 0.42f (接近屏幕边缘)
        float layoutRadius = minScreenDimension * 0.33f;

        // 稍微调小卡牌尺寸，避免重叠
        float cardWidth = layoutRadius * 0.55f;
        float cardHeight = cardWidth * (4.0f / 3.0f);

        if (packOpened && openAnimationProgress < 1.0f) {
            openAnimationProgress += 0.04f * partialTick;
            if (openAnimationProgress > 1.0f) openAnimationProgress = 1.0f;
        }

        if (!packOpened) {
            renderPack(graphics, (int)screenCenterX, (int)screenCenterY, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, "Click to Open!", (int)screenCenterX, (int)screenCenterY + 100, 0xFFFFFF);
        } else {
            float animT = backOut(openAnimationProgress);
            hoveredIndex = -1;

            float[][] currentPos = new float[5][2];

            for (int i = 0; i < 5; i++) {
                float targetCenterX = screenCenterX + PENTAGON_VERTICES[i][0] * layoutRadius;
                float targetCenterY = screenCenterY + PENTAGON_VERTICES[i][1] * layoutRadius;

                float currentCenterX = Mth.lerp(animT, screenCenterX, targetCenterX);
                float currentCenterY = Mth.lerp(animT, screenCenterY, targetCenterY);

                currentPos[i][0] = currentCenterX - cardWidth / 2.0f;
                currentPos[i][1] = currentCenterY - cardHeight / 2.0f;

                if (isFlipped[i] && flipProgress[i] < 1.0f) {
                    flipProgress[i] += 0.04f;
                }

                if (openAnimationProgress > 0.8f) {
                    if (Math.abs(mouseX - currentCenterX) < cardWidth/2 &&
                            Math.abs(mouseY - currentCenterY) < cardHeight/2) {
                        hoveredIndex = i;
                    }
                }
            }

            for (int i = 0; i < 5; i++) {
                if (i != hoveredIndex) {
                    renderAnimatedCard(graphics, i, currentPos[i][0], currentPos[i][1], cardWidth, cardHeight, animT, false);
                }
            }

            if (hoveredIndex != -1) {
                renderAnimatedCard(graphics, hoveredIndex, currentPos[hoveredIndex][0], currentPos[hoveredIndex][1], cardWidth, cardHeight, animT, true);
            }

            if (openAnimationProgress > 0.9f) {
                graphics.drawCenteredString(font, "Click to Flip", (int)screenCenterX, (int)screenCenterY + 130, 0xFFD700);
            }
        }
    }

    private void renderAnimatedCard(GuiGraphics graphics, int index, float currentX, float currentY,
                                    float w, float h, float animT, boolean isHovered) {
        float flyRotation = (1.0f - animT) * (index - 2) * 20.0f;
        float flyScale = Mth.lerp(animT, 0.2f, 1.0f);
        renderSingleCard(graphics, index, currentX, currentY, w, h, isHovered, flyRotation, flyScale);
    }

    private void renderSingleCard(GuiGraphics graphics, int index, float x, float y, float w, float h,
                                  boolean isHovered, float rotationZ, float globalScale) {
        graphics.pose().pushPose();
        float hoverScale = isHovered ? 1.2f : 1.0f;
        float flip = flipProgress[index];
        boolean showingBack = flip < 0.5f;
        float flipScaleX = Math.abs(Mth.cos(flip * (float)Math.PI));

        graphics.pose().translate(x + w/2, y + h/2, 0);
        graphics.pose().scale(globalScale, globalScale, 1.0f);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationZ));
        graphics.pose().scale(hoverScale, hoverScale, 1.0f);
        graphics.pose().scale(flipScaleX, 1.0f, 1.0f);

        if (isHovered) graphics.pose().translate(0, 0, 200);

        graphics.pose().translate(-w/2, -h/2, 0);

        if (showingBack) {
            applyLinearFiltering(CARD_BACK);
            // 既然卡背现在也是 768x1024，使用 11 参数的 blit 进行完整缩放绘制
            graphics.blit(CARD_BACK, 0, 0, (int)w, (int)h, 0, 0, 768, 1024, 768, 1024);
        } else {
            renderHighResCardFace(graphics, cards.get(index), (int)w, (int)h);
        }
        graphics.pose().popPose();
    }

    private void renderHighResCardFace(GuiGraphics graphics, ItemStack stack, int w, int h) {
        CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
        if (def == null) return;

        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/bg/" + def.category().name().toLowerCase() + ".png");
        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1).replace(".png", "");
        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/art/" + fileName + ".png");
        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/border/" + def.rarity().name().toLowerCase() + ".png");

        RenderSystem.enableBlend();
        applyLinearFiltering(bgLoc);
        applyLinearFiltering(artLoc);
        applyLinearFiltering(borderLoc);

        // 使用 11 参数 blit 确保缩放正确
        graphics.blit(bgLoc, 0, 0, w, h, 0, 0, 768, 1024, 768, 1024);
        graphics.blit(artLoc, 0, 0, w, h, 0, 0, 768, 1024, 768, 1024);
        graphics.blit(borderLoc, 0, 0, w, h, 0, 0, 768, 1024, 768, 1024);
        RenderSystem.disableBlend();

        float textScale = (w / 100.0f) * 1.2f;
        if (textScale < 0.6f) textScale = 0.6f;
        float margin = 8.0f;

        renderCardStat(graphics, String.valueOf(def.cost()), margin, margin, 0x00FFFF, textScale);
        renderCardStat(graphics, String.valueOf(def.damage()), margin, h - margin - 12, 0xFFAA00, textScale);

        String hpText = String.valueOf(def.health());
        int hpWidth = (int)(font.width(hpText) * textScale);
        renderCardStat(graphics, hpText, w - margin - hpWidth, h - margin - 12, 0xFF5555, textScale);
    }

    private void renderCardStat(GuiGraphics graphics, String text, float x, float y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 5.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }

    // 【修改 2】卡包渲染逻辑修复
    private void renderPack(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        float scale = 1.0f;

        // 1. 定义屏幕上的显示尺寸 (3:4)
        int packDisplayWidth = 120;
        int packDisplayHeight = 160;

        long timeSinceOpen = System.currentTimeMillis() - creationTime;
        boolean safeToHover = timeSinceOpen > 200;

        boolean isHovered = safeToHover &&
                Math.abs(mouseX - x) < packDisplayWidth/2 &&
                Math.abs(mouseY - y) < packDisplayHeight/2;

        if (isHovered) scale *= 1.1f;
        if (packOpened) scale *= (1.0f - openAnimationProgress);

        if (scale > 0.01f) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            if (isHovered && !packOpened) {
                renderPackGlow(graphics, packDisplayWidth, packDisplayHeight);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            // 【核心修正】使用 11 个参数的 blit 方法进行缩放绘制
            // 参数含义:
            // texture, x, y,
            // destWidth (屏幕宽), destHeight (屏幕高),
            // uOffset (贴图起始u), vOffset (贴图起始v),
            // uWidth (采样宽), vHeight (采样高),
            // textureWidth (贴图总宽), textureHeight (贴图总高)

            graphics.blit(PACK_TEXTURE,
                    -packDisplayWidth/2, -packDisplayHeight/2, // 坐标 (居中后)
                    packDisplayWidth, packDisplayHeight,       // 目标大小 (120x160)
                    0, 0,                                      // 起始 UV
                    768, 1024,                                 // 采样大小 (整张图)
                    768, 1024                                  // 贴图真实大小
            );

            graphics.pose().popPose();
        }
    }

    private void renderPackGlow(GuiGraphics graphics, int w, int h) {
        float r = ((maxRarityColor >> 16) & 0xFF) / 255.0f;
        float g = ((maxRarityColor >> 8) & 0xFF) / 255.0f;
        float b = (maxRarityColor & 0xFF) / 255.0f;

        float margin = 12.0f;
        float rectW = w + margin * 2;
        float rectH = h + margin * 2;
        float perimeter = 2 * (rectW + rectH);
        float left = -rectW / 2.0f;
        float top = -rectH / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyLinearFiltering(PACK_GLOW_TEXTURE);

        long time = System.currentTimeMillis();
        float speed = 0.4f;
        float headDistance = ((time % 100000L) * speed) % perimeter;
        int trailLength = 30;
        float gap = 10.0f;

        for (int i = 0; i < trailLength; i++) {
            float currentDist = headDistance - (i * gap);
            if (currentDist < 0) currentDist += perimeter;

            float[] pos = getPointOnRect(currentDist, left, top, rectW, rectH);

            float alpha = Math.max(0.0f, (1.0f - (float)i / trailLength) * 0.8f);
            float scale = 1.0f - (float)i / trailLength * 0.6f;
            float pointSize = 48.0f * scale;

            RenderSystem.setShaderColor(r, g, b, alpha);
            graphics.pose().pushPose();
            graphics.pose().translate(pos[0], pos[1], 0);
            graphics.blit(PACK_GLOW_TEXTURE, (int)(-pointSize/2), (int)(-pointSize/2), 0, 0, (int)pointSize, (int)pointSize, 512, 512);
            graphics.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private float[] getPointOnRect(float dist, float left, float top, float w, float h) {
        if (dist <= w) return new float[] { left + dist, top };
        dist -= w;
        if (dist <= h) return new float[] { left + w, top + dist };
        dist -= h;
        if (dist <= w) return new float[] { left + w - dist, top + h };
        dist -= w;
        return new float[] { left, top + h - dist };
    }

    private void applyLinearFiltering(ResourceLocation loc) {
        AbstractTexture texture = this.minecraft.getTextureManager().getTexture(loc);
        if (texture != null) texture.setFilter(true, false);
    }

    private float backOut(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (!packOpened) {
                packOpened = true;
            } else if (openAnimationProgress > 0.8f) {
                if (hoveredIndex != -1 && !isFlipped[hoveredIndex]) {
                    isFlipped[hoveredIndex] = true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new PacketClaimRewards());
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}