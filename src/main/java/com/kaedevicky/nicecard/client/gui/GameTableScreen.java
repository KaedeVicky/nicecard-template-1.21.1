package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.game.CardInstance;
import com.kaedevicky.nicecard.game.PlayerState;
import com.kaedevicky.nicecard.network.PacketTableAction;
import com.kaedevicky.nicecard.registry.ModDataComponents;
import com.kaedevicky.nicecard.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class GameTableScreen extends Screen {

    private final GameTableBlockEntity tableEntity;

    // 按钮定义为成员变量
    private Button btnJoinP1;
    private Button btnJoinP2;
    private Button btnStart;
    private Button btnEndTurn;

    // 布局常量
    private static final int CARD_WIDTH = 60;
    private static final int CARD_HEIGHT = 80;
    private static final int MINI_CARD_WIDTH = 45;
    private static final int MINI_CARD_HEIGHT = 60;

    private static final int HERO_SIZE = 64;       // 英雄头像大小
    private static final int WEAPON_SIZE = 32;     // 武器框大小
    private static final int POWER_SIZE = 32;      // 技能框大小
    private static final int MANA_CRYSTAL_SIZE = 12; // 法力水晶大小

    // 资源路径
    private static final ResourceLocation BOARD_BG = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/board_background.png");
    private static final ResourceLocation CARD_BACK = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_back.png");

    public GameTableScreen(GameTableBlockEntity tableEntity) {
        super(Component.literal("NiceCard Table"));
        this.tableEntity = tableEntity;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. 初始化大厅按钮
        // 注意：addRenderableWidget 既注册了点击事件，也把它们加入了默认渲染列表。
        // 但因为我们下面会移除 super.render()，所以我们需要手动渲染它们。
        this.btnJoinP1 = this.addRenderableWidget(Button.builder(Component.literal("Join P1"), button -> {
            sendAction(1);
        }).bounds(centerX - 100, centerY + 30, 80, 20).build());

        this.btnJoinP2 = this.addRenderableWidget(Button.builder(Component.literal("Join P2"), button -> {
            sendAction(2);
        }).bounds(centerX + 20, centerY + 30, 80, 20).build());

        this.btnStart = this.addRenderableWidget(Button.builder(Component.literal("Start Game"), button -> {
            sendAction(3);
        }).bounds(centerX - 40, centerY + 60, 80, 20).build());

        // 2. 初始化游戏中按钮
        this.btnEndTurn = this.addRenderableWidget(Button.builder(Component.literal("End Turn"), button -> {
            sendAction(4);
        }).bounds(this.width - 100, this.height / 2 - 10, 80, 20).build());

        // 默认全部隐藏，完全由 render 方法控制
        this.btnJoinP1.visible = false;
        this.btnJoinP2.visible = false;
        this.btnStart.visible = false;
        this.btnEndTurn.visible = false;
    }

    private void sendAction(int type) {
        if (tableEntity != null && tableEntity.getLevel() != null) {
            PacketDistributor.sendToServer(new PacketTableAction(tableEntity.getBlockPos(), type));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 绘制世界背景 (模糊/变暗层)
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 2. 根据游戏状态分流渲染
        if (tableEntity.isGameStarted) {
            renderGameInterface(graphics, mouseX, mouseY, partialTick);
        } else {
            renderLobbyInterface(graphics, mouseX, mouseY, partialTick);
        }

        // ⚠️ 注意：这里删除了 super.render(...)
        // 这样按钮就不会乱跑，完全听我们在上面两个方法里的指挥

        // 3. 最后绘制 Tooltip (最顶层)
        if (tableEntity.isGameStarted) {
            renderCardTooltips(graphics, mouseX, mouseY);
        }
    }

    // =================================================================================
    // 大厅渲染逻辑
    // =================================================================================
    private void renderLobbyInterface(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. 背景板
        graphics.fill(centerX - 130, centerY - 90, centerX + 130, centerY + 110, 0xDD000000);
        graphics.renderOutline(centerX - 130, centerY - 90, 260, 200, 0xFFFFD700);

        // 2. 渲染文字 (高清无模糊方案)
        RenderSystem.disableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250.0F); // 抬高 Z 轴

        // 标题
        graphics.pose().pushPose();
        float titleScale = 2.0f;
        graphics.pose().scale(titleScale, titleScale, 1.0f);
        graphics.drawCenteredString(this.font, "GAME LOBBY", (int)(centerX / titleScale), (int)((centerY - 70) / titleScale), 0xFFFFD700);
        graphics.pose().popPose();

        // 玩家名字
        String p1Name = tableEntity.getPlayer1Name();
        String p2Name = tableEntity.getPlayer2Name();
        if (p1Name == null || p1Name.isEmpty()) p1Name = "--- Empty ---";
        if (p2Name == null || p2Name.isEmpty()) p2Name = "--- Empty ---";

        graphics.pose().pushPose();
        float nameScale = 1.5f;
        graphics.pose().scale(nameScale, nameScale, 1.0f);
        int scaledCenterY = (int)(centerY / nameScale);

        // P1 信息
        int p1X = (int)((centerX - 65) / nameScale);
        graphics.drawCenteredString(this.font, "Player 1", p1X, scaledCenterY - 20, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, p1Name, p1X, scaledCenterY - 5, 0xFFFFFFFF);

        // P2 信息
        int p2X = (int)((centerX + 65) / nameScale);
        graphics.drawCenteredString(this.font, "Player 2", p2X, scaledCenterY - 20, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, p2Name, p2X, scaledCenterY - 5, 0xFFFFFFFF);

        graphics.pose().popPose(); // 结束文字缩放
        graphics.pose().popPose(); // 结束 Z 轴位移
        RenderSystem.enableDepthTest();

        // 3. 【手动渲染大厅按钮】
        // 它们会画在背景板之上
        if (this.btnJoinP1 != null) {
            this.btnJoinP1.visible = true;
            this.btnJoinP1.active = tableEntity.getPlayer1Name().isEmpty();
            this.btnJoinP1.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.btnJoinP2 != null) {
            this.btnJoinP2.visible = true;
            this.btnJoinP2.active = tableEntity.getPlayer2Name().isEmpty();
            this.btnJoinP2.render(graphics, mouseX, mouseY, partialTick);
        }
        if (this.btnStart != null) {
            this.btnStart.visible = true;
            boolean p1Ready = !tableEntity.getPlayer1Name().isEmpty();
            boolean p2Ready = !tableEntity.getPlayer2Name().isEmpty();
            this.btnStart.active = p1Ready && p2Ready;
            this.btnStart.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    // =================================================================================
    // 游戏界面渲染逻辑
    // =================================================================================
    private void renderGameInterface(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // A. 确定视角
        boolean amIP1 = minecraft.player != null && minecraft.player.getUUID().equals(tableEntity.gameData.player1.uuid);
        PlayerState myState = amIP1 ? tableEntity.gameData.player1 : tableEntity.gameData.player2;
        PlayerState enemyState = amIP1 ? tableEntity.gameData.player2 : tableEntity.gameData.player1;

        // B. 绘制桌面背景
        graphics.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
        // 中线 (战场分割线)
        graphics.fill(0, this.height / 2 - 1, this.width, this.height / 2 + 1, 0xFF222222);

        // C. 绘制游戏元素
        // 注意顺序：先画棋盘内容，再画英雄和手牌，防止遮挡

        // 1. 战场随从 (Board)
        renderBoard(graphics, enemyState.board, false, mouseX, mouseY);
        renderBoard(graphics, myState.board, true, mouseX, mouseY);

        // 2. 英雄区域 (Hero + Weapon + Power)
        // 敌方在顶部中心
        renderHeroZone(graphics, enemyState, false, this.width / 2, 60);
        // 我方在底部中心 (留出空位给手牌)
        renderHeroZone(graphics, myState, true, this.width / 2, this.height - 70);

        // 3. 手牌 (Hand)
        // 敌方手牌在最顶部
        renderHand(graphics, enemyState.hand, false, mouseX, mouseY);
        // 我方手牌在最底部
        renderHand(graphics, myState.hand, true, mouseX, mouseY);

        // 4. 法力水晶 (只画自己的)
        renderManaBar(graphics, myState);

        // D. 按钮渲染 (End Turn) - 放在右侧中间，模仿炉石的位置
        if (this.btnEndTurn != null) {
            this.btnEndTurn.visible = true;
            boolean isMyTurn = (amIP1 && tableEntity.gameData.currentPlayer == 1) || (!amIP1 && tableEntity.gameData.currentPlayer == 2);
            this.btnEndTurn.active = isMyTurn;
            this.btnEndTurn.setMessage(Component.literal(isMyTurn ? "End Turn" : "Enemy Turn"));

            // 重新设置按钮位置到屏幕右侧中间
            this.btnEndTurn.setX(this.width - 90);
            this.btnEndTurn.setY(this.height / 2 - 10);

            this.btnEndTurn.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderHeroZone(GuiGraphics graphics, PlayerState state, boolean isMe, int centerX, int centerY) {
        if (state == null) return;

        // 1. 绘制英雄头像 (正中心)
        int heroX = centerX - HERO_SIZE / 2;
        int heroY = centerY - HERO_SIZE / 2;

        // 头像背景 (灰色占位，以后换成皮肤)
        graphics.fill(heroX, heroY, heroX + HERO_SIZE, heroY + HERO_SIZE, 0xFF444444);

        // 边框 (金色表示当前回合，白色表示普通)
        // 这里简单处理，统一白色边框
        graphics.renderOutline(heroX, heroY, HERO_SIZE, HERO_SIZE, 0xFFFFFFFF);

        // 名字 (头像上方)
        String name = state.name == null ? "Unknown" : state.name;
        int nameWidth = this.font.width(name);
        graphics.drawString(this.font, name, centerX - nameWidth / 2, heroY - 12, 0xFFFFFFFF);

        // 血量 (头像右下角，像炉石一样是一个血滴形状)
        // 我们画一个红色方块代替
        int hpX = heroX + HERO_SIZE - 10;
        int hpY = heroY + HERO_SIZE - 10;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 50); // 浮起
        graphics.fill(hpX - 5, hpY - 5, hpX + 15, hpY + 15, 0xFFFF0000); // 红底
        graphics.renderOutline(hpX - 5, hpY - 5, 20, 20, 0xFF000000);    // 黑边
        graphics.drawCenteredString(this.font, String.valueOf(state.health), hpX + 5, hpY, 0xFFFFFFFF);
        graphics.pose().popPose();

        // 护甲 (如果有，画在血量上方，灰色盾牌)
        // if (state.armor > 0) ...

        // 2. 绘制武器框 (头像左侧)
        int weaponX = heroX - WEAPON_SIZE - 5;
        int weaponY = centerY - WEAPON_SIZE / 2 + 10; // 稍微靠下一点

        // 画个圆框 (用 fill 模拟)
        graphics.fill(weaponX, weaponY, weaponX + WEAPON_SIZE, weaponY + WEAPON_SIZE, 0xFF222222); // 深灰底
        graphics.renderOutline(weaponX, weaponY, WEAPON_SIZE, WEAPON_SIZE, 0xFF555555); // 灰边框
        // 武器图标占位符
        graphics.drawCenteredString(this.font, "W", weaponX + WEAPON_SIZE / 2, weaponY + 8, 0xFF777777);
        // 如果有武器数据：显示攻击力和耐久度 (Attack/Durability)

        // 3. 绘制技能框 (头像右侧)
        int powerX = heroX + HERO_SIZE + 5;
        int powerY = centerY - POWER_SIZE / 2 + 5;

        graphics.fill(powerX, powerY, powerX + POWER_SIZE, powerY + POWER_SIZE, 0xFF222222);
        graphics.renderOutline(powerX, powerY, POWER_SIZE, POWER_SIZE, 0xFF00AA00); // 绿色边框表示技能
        // 技能图标占位符
        graphics.drawCenteredString(this.font, "S", powerX + POWER_SIZE / 2, powerY + 8, 0xFF777777);
        // 费用提示 (技能通常是2费)
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 50);
        graphics.drawString(this.font, "2", powerX + POWER_SIZE / 2 - 2, powerY - 8, 0xFF00FFFF);
        graphics.pose().popPose();
    }

    private void renderManaBar(GuiGraphics graphics, PlayerState state) {
        if (state == null) return;

        // 位置：屏幕右下角
        int startX = this.width - 25;
        int startY = this.height - 40;

        // 绘制文字 "Mana: 5/10"
        String manaText = state.currentMana + "/" + state.maxMana;
        // 右对齐文字
        graphics.drawString(this.font, manaText, this.width - this.font.width(manaText) - 10, startY - 15, 0xFF00FFFF);

        // 绘制水晶可视化 (简单的蓝色格子)
        // 这里画成竖条或者横条都可以，为了省空间我们画一排横着的水晶
        int barWidth = 10 * (MANA_CRYSTAL_SIZE + 2); // 10个水晶的总宽
        int barStartX = this.width - barWidth - 10;
        int barY = startY;

        for (int i = 1; i <= 10; i++) {
            int x = barStartX + (i - 1) * (MANA_CRYSTAL_SIZE + 2);

            // 逻辑：
            // i <= currentMana -> 亮蓝色 (可用)
            // i <= maxMana -> 暗蓝色 (空槽)
            // i > maxMana -> 黑色/灰色 (未解锁)

            int color;
            if (i <= state.currentMana) {
                color = 0xFF00FFFF; // 亮蓝
            } else if (i <= state.maxMana) {
                color = 0xFF004444; // 暗蓝 (空水晶)
            } else {
                color = 0xFF222222; // 锁住
            }

            graphics.fill(x, barY, x + MANA_CRYSTAL_SIZE, barY + MANA_CRYSTAL_SIZE, color);
        }
    }

    private void renderHand(GuiGraphics graphics, List<CardInstance> hand, boolean isMe, int mouseX, int mouseY) {
        if (hand == null || hand.isEmpty()) return;

        int spacing = 50;
        int totalWidth = (hand.size() - 1) * spacing + CARD_WIDTH;
        int startX = (this.width - totalWidth) / 2;
        int y = isMe ? (this.height - CARD_HEIGHT + 30) : -50;

        for (int i = 0; i < hand.size(); i++) {
            int x = startX + i * spacing;
            boolean isHovered = isMe && isHovered(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
            int drawY = isHovered ? (this.height - CARD_HEIGHT - 10) : y;

            if (isMe) {
                // 唯一合法的渲染入口
                renderCardFace(graphics, hand.get(i), x, drawY, CARD_WIDTH, CARD_HEIGHT);
            } else {
                // 对手背面，不需要贴图，直接画红块
                graphics.fill(x, drawY, x + CARD_WIDTH, drawY + CARD_HEIGHT, 0xFF8B0000);
                graphics.renderOutline(x, drawY, CARD_WIDTH, CARD_HEIGHT, 0xFF000000);
            }
        }
    }

    private void renderBoard(GuiGraphics graphics, List<CardInstance> board, boolean isMe, int mouseX, int mouseY) {
        if (board == null) return;
        int count = board.size();
        if (count == 0) return;

        int spacing = MINI_CARD_WIDTH + 10;
        int totalWidth = count * spacing;
        int startX = (this.width - totalWidth) / 2;
        int y = isMe ? (this.height / 2 + 10) : (this.height / 2 - 10 - MINI_CARD_HEIGHT);

        for (int i = 0; i < count; i++) {
            CardInstance minion = board.get(i);
            int x = startX + i * spacing;
            renderCardFace(graphics, minion, x, y, MINI_CARD_WIDTH, MINI_CARD_HEIGHT);

            if (minion.canAttack() && isMe) {
                graphics.renderOutline(x - 1, y - 1, MINI_CARD_WIDTH + 2, MINI_CARD_HEIGHT + 2, 0xFF00FF00);
            }
        }
    }

    private void renderCardFace(GuiGraphics graphics, CardInstance card, int x, int y, int w, int h) {
        CardDefinition def = card.getDefinition();
        if (def == null) return;

        // =================================================================
        // 第一步：计算三层贴图路径 (复用 CardItem 的逻辑)
        // =================================================================

        // 1. 背景路径 (根据 Category)
        // 路径: textures/gui/bg/overworld.png
        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/bg/" + def.category().name().toLowerCase() + ".png");

        // 2. 插画路径 (从 textureLocation 中提取文件名)
        // 逻辑: "nicecard:textures/gui/art/creeper.png" -> "creeper" -> "textures/gui/art/creeper.png"
        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath;
        if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        if (fileName.endsWith(".png")) fileName = fileName.substring(0, fileName.length() - 4);

        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/art/" + fileName + ".png");

        // 3. 边框路径 (根据 Rarity)
        // 路径: textures/gui/border/rare.png
        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID,
                "textures/gui/border/" + def.rarity().name().toLowerCase() + ".png");

        // =================================================================
        // 第二步：分层绘制 (背景 -> 插画 -> 边框)
        // =================================================================

        // (1) 画背景
        graphics.blit(bgLoc, x, y, w, h, 0, 0, w, h, w, h);

        // (2) 画插画
        // 插画通常需要稍微缩进一点，或者根据你的图片设计直接铺满
        // 这里假设插画也是标准尺寸，直接铺满
        graphics.blit(artLoc, x, y, w, h, 0, 0, w, h, w, h);

        // (3) 画边框 (最上层)
        graphics.blit(borderLoc, x, y, w, h, 0, 0, w, h, w, h);

        // =================================================================
        // 第三步：绘制数值 (攻击、生命、费用)
        // =================================================================
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100); // 让文字浮在图片上方

        // 计算字体缩放 (根据卡牌当前的渲染宽度 w 动态调整)
        // 假设标准宽度是 60，如果 w=60 则 scale=0.8，如果 w=45 则 scale=0.6
        float scale = (float)w / 75.0f;
        if (scale < 0.5f) scale = 0.5f; // 最小缩放限制

        graphics.pose().scale(scale, scale, 1.0f);

        // 反算缩放后的坐标
        int tx = (int)(x / scale);
        int ty = (int)(y / scale);
        int tw = (int)(w / scale);
        int th = (int)(h / scale);

        // 边距调整
        int margin = 4;

        // A. 左上角：费用 (Cost) - 青色
        String costText = String.valueOf(card.getCurrentCost()); // 使用动态费用
        graphics.drawString(this.font, costText, tx + margin, ty + margin, 0xFF00FFFF, true);

        // B. 左下角：攻击力 (Attack) - 金色
        String atkText = String.valueOf(card.getAttack());
        // 如果攻击力被 buff 了(大于原始值)，显示绿色；否则黄色
        int atkColor = (card.getAttack() > def.damage()) ? 0xFF00FF00 : 0xFFFFFF00;
        graphics.drawString(this.font, atkText, tx + margin, ty + th - margin - 8, atkColor, true);

        // C. 右下角：血量 (Health) - 红色
        String hpText = String.valueOf(card.getHealth());
        // 如果受伤(小于上限)，显示红色；否则白色
        int hpColor = (card.getHealth() < def.health()) ? 0xFFFF0000 : 0xFFFFFFFF;
        // 计算文字宽度以便右对齐
        int hpWidth = this.font.width(hpText);
        graphics.drawString(this.font, hpText, tx + tw - margin - hpWidth, ty + th - margin - 8, hpColor, true);

        graphics.pose().popPose();
    }

    private void renderHero(GuiGraphics graphics, PlayerState state, boolean isMe) {
        if (state == null) return;
        int x = isMe ? this.width - 120 : 20;
        int y = isMe ? this.height - 60 : 20;

        graphics.fill(x, y, x + 40, y + 40, 0xFF444444);
        graphics.renderOutline(x, y, 40, 40, 0xFFFFFFFF);

        String name = state.name == null ? "Unknown" : state.name;
        graphics.drawString(this.font, name, x, y - 10, 0xFFFFFFFF);
        graphics.drawString(this.font, "HP: " + state.health, x + 5, y + 15, 0xFFFF0000);
        graphics.drawString(this.font, "Mana: " + state.currentMana + "/" + state.maxMana, x + 45, y + 15, 0xFF0000FF);
    }

    private void renderCardTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean amIP1 = minecraft.player != null && minecraft.player.getUUID().equals(tableEntity.gameData.player1.uuid);
        PlayerState myState = amIP1 ? tableEntity.gameData.player1 : tableEntity.gameData.player2;

        if (myState == null || myState.hand == null) return;

        int count = myState.hand.size();
        int spacing = 50;
        int totalWidth = (count - 1) * spacing + CARD_WIDTH;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height - CARD_HEIGHT + 20;

        for (int i = 0; i < count; i++) {
            int x = startX + i * spacing;
            // 检测是否覆盖 (如果覆盖了，y坐标会抬高20，所以这里也要算进去)
            boolean hovered = isHovered(mouseX, mouseY, x, y - (isHovered(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT) ? 20 : 0), CARD_WIDTH, CARD_HEIGHT);

            if (hovered) {
                CardInstance card = myState.hand.get(i);
                if (card != null && card.getDefinition() != null) {
                    ItemStack fakeStack = new ItemStack(ModItems.BASE_CARD.get());
                    fakeStack.set(ModDataComponents.CARD_DEF, card.getDefinition());
                    graphics.renderTooltip(this.font, fakeStack, mouseX, mouseY);
                }
                return;
            }
        }
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}