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

    // 动画状态
    private boolean packOpened = false;
    private float openAnimationProgress = 0.0f;
    private int hoveredIndex = -1;

    // 最高品质光效颜色
    private final int maxRarityColor;

    // 资源
    private static final ResourceLocation PACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_pack.png");
    private static final ResourceLocation CARD_BACK = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_back_high_res.png");
    private static final ResourceLocation PACK_GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/pack_glow.png");

    // 【新增】预计算的五边形顶点标准化向量 (尖顶朝上，顺时针)
    // {x, y} 相对于中心的偏移比例
    private static final float[][] PENTAGON_VERTICES = {
            {0.0f, -1.0f},      // 0: 顶部
            {0.951f, -0.309f},  // 1: 右上
            {0.588f, 0.809f},   // 2: 右下
            {-0.588f, 0.809f},  // 3: 左下
            {-0.951f, -0.309f}  // 4: 左上
    };

    // 新增：防止调整窗口大小时重复移动鼠标
    private boolean hasMovedMouse = false;

    // 新增：记录界面打开的时间戳
    private final long creationTime;

    @Override
    protected void init() {
        super.init();

        // 仅在第一次打开界面时移动鼠标
        if (!hasMovedMouse) {
            hasMovedMouse = true;

            // 1. 获取窗口句柄
            long window = this.minecraft.getWindow().getWindow();

            // 2. 获取 GUI 缩放比例 (GLFW 使用物理像素，而 Minecraft 使用 GUI 坐标)
            double scale = this.minecraft.getWindow().getGuiScale();

            // 3. 计算目标位置：移动到屏幕中心偏下 100 像素的位置
            // 这样既避开了中心的卡包，又正好在 "Click to Open" 文字附近，引导性很好
            double targetX = (this.width / 2.0) * scale;
            double targetY = (this.height / 2.0 + 100.0) * scale;

            // 4. 强制设置鼠标位置
            GLFW.glfwSetCursorPos(window, targetX, targetY);
        }
    }

    public CardOpenScreen(List<ItemStack> cards) {
        super(Component.literal("Card Opening"));
        this.cards = cards;
        this.isFlipped = new boolean[cards.size()];
        this.flipProgress = new float[cards.size()];
        this.maxRarityColor = calculateMaxRarityColor(cards);

        // 【初始化时间戳】
        this.creationTime = System.currentTimeMillis();
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

        // --- 1. 响应式五边形布局计算 ---
        // 使用屏幕较短边的一定比例作为布局半径
        float minScreenDimension = Math.min(this.width, this.height);
        // 布局半径：卡牌中心到屏幕中心的距离 (调整 0.35f 来改变五边形大小)
        float layoutRadius = minScreenDimension * 0.25f;
        // 卡牌大小：根据半径动态调整，保证不重叠 (调整 0.65f 来改变卡牌自身大小)
        float baseCardSize = layoutRadius * 0.85f;

        // 动画进度更新
        if (packOpened && openAnimationProgress < 1.0f) {
            openAnimationProgress += 0.04f * partialTick;
            if (openAnimationProgress > 1.0f) openAnimationProgress = 1.0f;
        }

        if (!packOpened) {
            renderPack(graphics, (int)screenCenterX, (int)screenCenterY, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, "Click to Open!", (int)screenCenterX, (int)screenCenterY + 80, 0xFFFFFF);
        } else {
            float animT = backOut(openAnimationProgress);
            hoveredIndex = -1;

            // 用于存储每张卡当前计算出的左上角坐标，供渲染循环使用
            float[][] currentPos = new float[5][2];

            // --- 循环 1: 计算位置 & 检测悬停 ---
            for (int i = 0; i < 5; i++) {
                // 计算目标中心点坐标 (五边形顶点)
                float targetCenterX = screenCenterX + PENTAGON_VERTICES[i][0] * layoutRadius;
                float targetCenterY = screenCenterY + PENTAGON_VERTICES[i][1] * layoutRadius;

                // 动画插值：从屏幕中心飞向目标中心
                float currentCenterX = Mth.lerp(animT, screenCenterX, targetCenterX);
                float currentCenterY = Mth.lerp(animT, screenCenterY, targetCenterY);

                // 转换为左上角坐标供渲染使用
                currentPos[i][0] = currentCenterX - baseCardSize / 2.0f;
                currentPos[i][1] = currentCenterY - baseCardSize / 2.0f;

                if (isFlipped[i] && flipProgress[i] < 1.0f) {
                    flipProgress[i] += 0.04f;
                }

                // 悬停检测 (使用中心点检测更准确)
                if (openAnimationProgress > 0.8f) {
                    if (Math.abs(mouseX - currentCenterX) < baseCardSize/2 &&
                            Math.abs(mouseY - currentCenterY) < baseCardSize/2) {
                        hoveredIndex = i;
                    }
                }
            }

            // --- 循环 2: 渲染非悬停卡牌 ---
            for (int i = 0; i < 5; i++) {
                if (i != hoveredIndex) {
                    renderAnimatedCard(graphics, i, currentPos[i][0], currentPos[i][1], baseCardSize, animT, false);
                }
            }

            // --- 循环 3: 渲染悬停卡牌 (最后渲染，保证在最上层) ---
            if (hoveredIndex != -1) {
                renderAnimatedCard(graphics, hoveredIndex, currentPos[hoveredIndex][0], currentPos[hoveredIndex][1], baseCardSize, animT, true);
            }

            // --- 渲染中心提示文字 ---
            if (openAnimationProgress > 0.9f) {
                // 将文字放在屏幕正中心
                // 稍微向上偏移一点点(-9)，让文字中心对齐屏幕中心
                graphics.drawCenteredString(font, "Click to Flip", (int)screenCenterX, (int)screenCenterY + 100, 0xFFD700);
            }
        }
    }

    // --------------------------------------------------------
    // 渲染相关方法
    // --------------------------------------------------------

    private void renderPack(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
//        float breath = Mth.sin((System.currentTimeMillis() % 1000) / 1000.0f * (float)Math.PI * 2);
        float scale = 1.0f;
        int size = 128;

        long timeSinceOpen = System.currentTimeMillis() - creationTime;

        // 只有当界面打开超过 200ms 后，才允许触发悬停检测
        // 这给了鼠标足够的时间“瞬移”走，防止第一帧误触
        boolean safeToHover = timeSinceOpen > 200;

        boolean isHovered = safeToHover &&
                Math.abs(mouseX - x) < size/2 &&
                Math.abs(mouseY - y) < size/2;

        if (isHovered) scale *= 1.1f;
        if (packOpened) scale *= (1.0f - openAnimationProgress);

        if (scale > 0.01f) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);

            if (isHovered && !packOpened) {
                renderPackGlow(graphics, size);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.blit(PACK_TEXTURE, -size/2, -size/2, 0, 0, size, size, size, size);
            graphics.pose().popPose();
        }
    }

    private void renderPackGlow(GuiGraphics graphics, int baseSize) {
        float r = ((maxRarityColor >> 16) & 0xFF) / 255.0f;
        float g = ((maxRarityColor >> 8) & 0xFF) / 255.0f;
        float b = (maxRarityColor & 0xFF) / 255.0f;

        // 1. 定义矩形轨迹范围
        // margin: 光点距离卡包边缘的空隙
        float margin = 10.0f;
        float rectW = baseSize + margin * 2;
        float rectH = baseSize + margin * 2;
        float perimeter = 2 * (rectW + rectH); // 周长

        // 矩形左上角 (相对于中心)
        float left = -rectW / 2.0f;
        float top = -rectH / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyLinearFiltering(PACK_GLOW_TEXTURE);

        // 2. 【核心修复】解决精度丢失问题
        // 先对时间取模，保证参与运算的数字足够小，保留毫秒级精度
        long time = System.currentTimeMillis();
        float speed = 0.4f; // 速度

        // 计算当前“头光点”在周长上的位置 (0 ~ perimeter)
        // 注意：这里先模 100000 再乘速度，最后再模周长
        float headDistance = ((time % 100000L) * speed) % perimeter;

        // 3. 绘制拖尾 (30个光点)
        int trailLength = 30;
        // 间距：每个光点之间隔多少像素
        float gap = 10.0f;

        for (int i = 0; i < trailLength; i++) {
            // 计算当前光点的位置 (向后倒推)
            float currentDist = headDistance - (i * gap);

            // 处理循环衔接：如果位置小于0，就绕回周长末尾
            if (currentDist < 0) {
                currentDist += perimeter;
            }

            // 获取坐标
            float[] pos = getPointOnRect(currentDist, left, top, rectW, rectH);

            // 越靠后的点越透明、越小
            float alpha = 1.0f - (float)i / trailLength;
            // 限制一下最小透明度，不然尾巴看不见
            alpha = Math.max(0.0f, alpha * 0.8f);

            float scale = 1.0f - (float)i / trailLength * 0.6f;
            float pointSize = 48.0f * scale; // 调大一点基础尺寸，确保看得清

            RenderSystem.setShaderColor(r, g, b, alpha);

            graphics.pose().pushPose();
            // 移动到光点位置
            graphics.pose().translate(pos[0], pos[1], 0);

            // 绘制
            graphics.blit(PACK_GLOW_TEXTURE,
                    (int)(-pointSize/2), (int)(-pointSize/2),
                    0, 0,
                    (int)pointSize, (int)pointSize,
                    512, 512); // 假设你的原图是 512x512

            graphics.pose().popPose();
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * 数学辅助方法：
     * 给定矩形周长上的一个距离 (distance)，返回对应的 (x, y) 坐标
     * 顺序：上边 -> 右边 -> 下边 -> 左边 (顺时针)
     */
    private float[] getPointOnRect(float dist, float left, float top, float w, float h) {
        // 1. 上边 (Top)
        if (dist <= w) {
            return new float[] { left + dist, top };
        }
        dist -= w;

        // 2. 右边 (Right)
        if (dist <= h) {
            return new float[] { left + w, top + dist };
        }
        dist -= h;

        // 3. 下边 (Bottom) - 注意这里是从右向左
        if (dist <= w) {
            return new float[] { left + w - dist, top + h };
        }
        dist -= w;

        // 4. 左边 (Left) - 注意这里是从下向上
        // 剩下的距离就是左边的
        return new float[] { left, top + h - dist };
    }

    // 修改后的渲染方法，接收计算好的当前坐标
    private void renderAnimatedCard(GuiGraphics graphics, int index, float currentX, float currentY,
                                    float size, float animT, boolean isHovered) {
        // 飞行中的旋转 flair (保留这个效果在五边形展开时也会很酷)
        float flyRotation = (1.0f - animT) * (index - 2) * 20.0f;
        float flyScale = Mth.lerp(animT, 0.2f, 1.0f);

        renderSingleCard(graphics, index, currentX, currentY, size, isHovered, flyRotation, flyScale);
    }

    private void renderSingleCard(GuiGraphics graphics, int index, float x, float y, float size,
                                  boolean isHovered, float rotationZ, float globalScale) {
        graphics.pose().pushPose();
        float hoverScale = isHovered ? 1.5f : 1.0f;
        float flip = flipProgress[index];
        boolean showingBack = flip < 0.5f;
        float flipScaleX = Math.abs(Mth.cos(flip * (float)Math.PI));

        graphics.pose().translate(x + size/2, y + size/2, 0);
        graphics.pose().scale(globalScale, globalScale, 1.0f);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotationZ));
        graphics.pose().scale(hoverScale, hoverScale, 1.0f);
        graphics.pose().scale(flipScaleX, 1.0f, 1.0f);

        if (isHovered) graphics.pose().translate(0, 0, 200);

        graphics.pose().translate(-size/2, -size/2, 0);

        if (showingBack) {
            applyLinearFiltering(CARD_BACK);
            graphics.blit(CARD_BACK, 0, 0, (int)size, (int)size, 0, 0, 512, 512, 512, 512);
        } else {
            renderHighResCardFace(graphics, cards.get(index), (int)size, (int)size);
        }
        graphics.pose().popPose();
    }

    private float backOut(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    private void renderHighResCardFace(GuiGraphics graphics, ItemStack stack, int w, int h) {
        CardDefinition def = stack.get(ModDataComponents.CARD_DEF);
        if (def == null) return;

        // ... 获取 ResourceLocation 的代码 (保持不变) ...
        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/bg/" + def.category().name().toLowerCase() + ".png");
        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1).replace(".png", "");
        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/art/" + fileName + ".png");
        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/border/" + def.rarity().name().toLowerCase() + ".png");

        // 1. 绘制贴图 (保持不变)
        RenderSystem.enableBlend();
        applyLinearFiltering(bgLoc);
        applyLinearFiltering(artLoc);
        applyLinearFiltering(borderLoc);

        graphics.blit(bgLoc, 0, 0, w, h, 0, 0, 1024, 1024, 1024, 1024);
        graphics.blit(artLoc, 0, 0, w, h, 0, 0, 1024, 1024, 1024, 1024);
        graphics.blit(borderLoc, 0, 0, w, h, 0, 0, 1024, 1024, 1024, 1024);
        RenderSystem.disableBlend();

        // ---------------------------------------------------------
        // 2. 【核心修改】绘制属性数值
        // ---------------------------------------------------------

        // 计算字体缩放比例：
        // 假设卡牌高度为 180 时，字体缩放为 1.5 倍看起来比较舒服
        // 公式：scale = (当前高度 / 参考高度) * 基准缩放
        float textScale = (h / 180.0f) * 1.5f;

        // 防止文字太小看不见，设置一个最小值
        if (textScale < 0.5f) textScale = 0.5f;

        // 定义位置偏移量 (相对于卡牌宽高的百分比)
        // 比如左上角：x在 12% 处，y在 12% 处
        float margin = 0.12f;

        // A. 左上角：花费 (Cost) - 青色/蓝色
        renderCardStat(graphics, String.valueOf(def.cost()),
                w * margin, h * margin,
                0x00FFFF, textScale);

        // B. 左下角：攻击力 (Attack) - 黄色/橙色
        // y 坐标在底部 12% 处
        renderCardStat(graphics, String.valueOf(def.damage()),
                w * margin, h * (1.0f - margin),
                0xFFAA00, textScale);

        // C. 右下角：血量 (Health) - 红色
        // x 坐标在右侧 12% 处
        renderCardStat(graphics, String.valueOf(def.health()),
                w * (1.0f - margin), h * (1.0f - margin),
                0xFF5555, textScale);
    }

    /**
     * 辅助方法：在指定位置绘制缩放后的属性文字
     * @param cx 中心点 X 坐标
     * @param cy 中心点 Y 坐标
     * @param color 颜色 (RGB Hex)
     * @param scale 缩放倍数
     */
    private void renderCardStat(GuiGraphics graphics, String text, float cx, float cy, int color, float scale) {
        graphics.pose().pushPose();

        // 1. 移动到目标位置
        // 注意：Z轴偏移 5.0f 确保文字浮在卡牌贴图上面，不会被遮挡
        graphics.pose().translate(cx, cy, 5.0f);

        // 2. 应用缩放
        graphics.pose().scale(scale, scale, 1.0f);

        // 3. 绘制文字 (居中绘制)
        // 因为我们已经 translate 到了中心点，所以这里要把文字中心对齐到 (0,0)
        // 也就是 x = -width/2, y = -height/2
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;

        // 绘制带阴影的文字，增加可读性
        graphics.drawString(font, text, -textWidth / 2, -textHeight / 2 + 1, color, true); // +1 是微调垂直居中

        graphics.pose().popPose();
    }

    private void applyLinearFiltering(ResourceLocation loc) {
        AbstractTexture texture = this.minecraft.getTextureManager().getTexture(loc);
        if (texture != null) texture.setFilter(true, false);
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
    public boolean isPauseScreen() {
        return false;
    }
}