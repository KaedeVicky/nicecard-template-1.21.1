package com.kaedevicky.nicecard.client.gui;

import com.kaedevicky.nicecard.NiceCard;
import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.card.CardDefinition;
import com.kaedevicky.nicecard.game.CardInstance;
import com.kaedevicky.nicecard.game.PlayerState;
import com.kaedevicky.nicecard.item.CardItem;
import com.kaedevicky.nicecard.network.PacketTableAction;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class GameTableScreen extends Screen {

    private final GameTableBlockEntity tableEntity;

    private Button btnJoinP1;
    private Button btnJoinP2;
    private Button btnStart;
    private Button btnEndTurn;

    private static final int CARD_WIDTH = 90;
    private static final int CARD_HEIGHT = 120;
    private static final int MINI_CARD_WIDTH = 60;
    private static final int MINI_CARD_HEIGHT = 80;
    private static final int HERO_SIZE = 64;

    private int selectedHandIndex = -1;
    private int selectedBoardIndex = -1;

    private static final ResourceLocation CARD_BACK = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/card_back_high_res.png");

    public GameTableScreen(GameTableBlockEntity tableEntity) {
        super(Component.literal("NiceCard Table"));
        this.tableEntity = tableEntity;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 直接调用 sendAction，不再进行客户端检查，防止误判导致按钮“失效”
        this.btnJoinP1 = this.addRenderableWidget(Button.builder(Component.literal("Join P1"), b -> sendAction(1))
                .bounds(centerX - 120, centerY + 30, 80, 20).build());

        this.btnJoinP2 = this.addRenderableWidget(Button.builder(Component.literal("Join P2"), b -> sendAction(2))
                .bounds(centerX + 40, centerY + 30, 80, 20).build());

        this.btnStart = this.addRenderableWidget(Button.builder(Component.literal("Start Game"), b -> sendAction(3))
                .bounds(centerX - 40, centerY + 60, 80, 20).build());

        this.btnEndTurn = this.addRenderableWidget(Button.builder(Component.literal("End Turn"), b -> sendAction(4))
                .bounds(this.width - 100, this.height / 2 - 10, 80, 20).build());

        updateButtonVisibility();
    }

    @Override
    public void tick() {
        super.tick();
        // 每一帧都检查状态，确保服务器同步数据后，按钮立即变灰/隐藏
        updateButtonVisibility();
    }

    // 禁用默认模糊背景
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void updateButtonVisibility() {
        if (tableEntity == null) return;

        boolean started = tableEntity.isGameStarted;
        this.btnEndTurn.visible = started;

        if (started) {
            this.btnJoinP1.visible = false;
            this.btnJoinP2.visible = false;
            this.btnStart.visible = false;
        } else {
            // 使用 BlockEntity 的 Getter 获取实时同步的数据
            boolean p1Taken = tableEntity.getPlayer1() != null;
            boolean p2Taken = tableEntity.getPlayer2() != null;

            this.btnJoinP1.visible = !p1Taken;
            this.btnJoinP2.visible = !p2Taken;

            this.btnStart.visible = true;
            this.btnStart.active = p1Taken && p2Taken;

            if (!this.btnStart.active) {
                this.btnStart.setMessage(Component.literal("Waiting...").withStyle(ChatFormatting.GRAY));
            } else {
                this.btnStart.setMessage(Component.literal("Start Game").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private void sendAction(int type) {
        // 只要 blockPos 存在就发包，可靠性最高
        if (tableEntity != null) {
            PacketDistributor.sendToServer(new PacketTableAction(tableEntity.getBlockPos(), type));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先处理按钮点击 (super)
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (!tableEntity.isGameStarted) return false;

        boolean amIP1 = minecraft.player != null && minecraft.player.getUUID().equals(tableEntity.gameData.player1.uuid);
        PlayerState myState = amIP1 ? tableEntity.gameData.player1 : tableEntity.gameData.player2;

        int handIdx = getHandHoverIndex(myState.hand, true, (int)mouseX, (int)mouseY);
        if (handIdx != -1) {
            if (selectedHandIndex == handIdx) {
                selectedHandIndex = -1;
            } else {
                selectedHandIndex = handIdx;
                selectedBoardIndex = -1;
            }
            return true;
        }
        selectedHandIndex = -1;
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. 手动画背景 (覆盖模糊)
        graphics.fill(0, 0, this.width, this.height, 0xFF1A1A1A);

        // 2. 画界面
        if (tableEntity.isGameStarted) {
            renderGameInterface(graphics, mouseX, mouseY, partialTick);
        } else {
            renderLobbyInterface(graphics, mouseX, mouseY, partialTick);
        }

        // 3. 画按钮 (super.render 必须调用)
        super.render(graphics, mouseX, mouseY, partialTick);

        // 4. 画 Tooltip
        if (tableEntity.isGameStarted) {
            renderCardTooltips(graphics, mouseX, mouseY);
        }
    }

    private void renderLobbyInterface(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.drawCenteredString(this.font, "--- NiceCard Lobby ---", centerX, centerY - 80, 0xFFD700);

        boolean p1Ready = tableEntity.getPlayer1() != null;
        String p1Name = !tableEntity.getPlayer1Name().isEmpty() ? tableEntity.getPlayer1Name() : "Empty";
        int p1Color = p1Ready ? 0x00FF00 : 0xAAAAAA;
        String p1Status = p1Ready ? "[Ready]" : "[Waiting]";

        graphics.drawCenteredString(this.font, "Player 1", centerX - 80, centerY - 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, p1Name, centerX - 80, centerY - 5, p1Color);
        graphics.drawCenteredString(this.font, p1Status, centerX - 80, centerY + 8, p1Color);

        boolean p2Ready = tableEntity.getPlayer2() != null;
        String p2Name = !tableEntity.getPlayer2Name().isEmpty() ? tableEntity.getPlayer2Name() : "Empty";
        int p2Color = p2Ready ? 0x00FF00 : 0xAAAAAA;
        String p2Status = p2Ready ? "[Ready]" : "[Waiting]";

        graphics.drawCenteredString(this.font, "Player 2", centerX + 80, centerY - 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, p2Name, centerX + 80, centerY - 5, p2Color);
        graphics.drawCenteredString(this.font, p2Status, centerX + 80, centerY + 8, p2Color);
    }

    private void renderGameInterface(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean amIP1 = minecraft.player != null && minecraft.player.getUUID().equals(tableEntity.gameData.player1.uuid);
        PlayerState myState = amIP1 ? tableEntity.gameData.player1 : tableEntity.gameData.player2;
        PlayerState enemyState = amIP1 ? tableEntity.gameData.player2 : tableEntity.gameData.player1;

        graphics.fill(0, this.height / 2 - 1, this.width, this.height / 2 + 1, 0xFF222222);

        renderBoard(graphics, enemyState.board, false, mouseX, mouseY);
        renderBoard(graphics, myState.board, true, mouseX, mouseY);

        renderHeroZone(graphics, enemyState, false, this.width / 2, 60);
        renderHeroZone(graphics, myState, true, this.width / 2, this.height - 70);

        renderHand(graphics, enemyState.hand, false, mouseX, mouseY);
        renderHand(graphics, myState.hand, true, mouseX, mouseY);

        renderManaBar(graphics, myState);

        // 按钮位置在 init 更新，这里不需要每帧更新
    }

    private void renderCardTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.minecraft.player == null) return;
        boolean amIP1 = this.minecraft.player.getUUID().equals(tableEntity.gameData.player1.uuid);
        PlayerState myState = amIP1 ? tableEntity.gameData.player1 : tableEntity.gameData.player2;
        PlayerState enemyState = amIP1 ? tableEntity.gameData.player2 : tableEntity.gameData.player1;

        int handIdx = getHandHoverIndex(myState.hand, true, mouseX, mouseY);
        if (handIdx != -1) {
            renderTooltipForCard(graphics, myState.hand.get(handIdx), mouseX, mouseY);
            return;
        }

        int myBoardIdx = getBoardHoverIndex(myState.board, true, mouseX, mouseY);
        if (myBoardIdx != -1) {
            renderTooltipForCard(graphics, myState.board.get(myBoardIdx), mouseX, mouseY);
            return;
        }
        int enemyBoardIdx = getBoardHoverIndex(enemyState.board, false, mouseX, mouseY);
        if (enemyBoardIdx != -1) {
            renderTooltipForCard(graphics, enemyState.board.get(enemyBoardIdx), mouseX, mouseY);
        }
    }

    private void renderTooltipForCard(GuiGraphics graphics, CardInstance card, int mouseX, int mouseY) {
        if (card == null || card.getDefinition() == null) return;
        ItemStack stack = CardItem.createCard(card.getDefinition());
        graphics.renderTooltip(this.font, stack, mouseX, mouseY);
    }

    // --- 渲染辅助方法 ---
    private void renderHand(GuiGraphics graphics, List<CardInstance> hand, boolean isMe, int mouseX, int mouseY) {
        if (hand == null || hand.isEmpty()) return;
        int count = hand.size();
        int maxTotalWidth = this.width - 200;
        int spacing = 70;
        if (count > 1) {
            int neededWidth = (count - 1) * spacing + CARD_WIDTH;
            if (neededWidth > maxTotalWidth) spacing = (maxTotalWidth - CARD_WIDTH) / (count - 1);
        }
        int totalWidth = (count - 1) * spacing + CARD_WIDTH;
        int startX = (this.width - totalWidth) / 2;
        int y = isMe ? (this.height - CARD_HEIGHT + 40) : -60;

        for (int i = 0; i < count; i++) {
            int x = startX + i * spacing;
            boolean isHovered = isMe && isHovered(mouseX, mouseY, x, y, CARD_WIDTH, CARD_HEIGHT);
            boolean isSelected = isMe && (selectedHandIndex == i);
            int drawY = (isHovered || isSelected) ? (this.height - CARD_HEIGHT - 10) : y;
            if (isMe) {
                renderCardFace(graphics, hand.get(i), x, drawY, CARD_WIDTH, CARD_HEIGHT);
                if (isSelected) graphics.renderOutline(x - 2, drawY - 2, CARD_WIDTH + 4, CARD_HEIGHT + 4, 0xFF00FF00);
            } else {
                drawSquareTextureCropped(graphics, CARD_BACK, x, drawY, CARD_WIDTH, CARD_HEIGHT);
            }
        }
    }

    private void renderBoard(GuiGraphics graphics, List<CardInstance> board, boolean isMe, int mouseX, int mouseY) {
        if (board == null) return;
        int count = board.size();
        if (count == 0) return;
        int spacing = MINI_CARD_WIDTH + 10;
        int startX = (this.width - count * spacing) / 2;
        int y = isMe ? (this.height / 2 + 15) : (this.height / 2 - 15 - MINI_CARD_HEIGHT);
        for (int i = 0; i < count; i++) {
            CardInstance minion = board.get(i);
            int x = startX + i * spacing;
            renderCardFace(graphics, minion, x, y, MINI_CARD_WIDTH, MINI_CARD_HEIGHT);
            if (minion.canAttack() && isMe) graphics.renderOutline(x - 1, y - 1, MINI_CARD_WIDTH + 2, MINI_CARD_HEIGHT + 2, 0xFF00FF00);
        }
    }

    private void renderCardFace(GuiGraphics graphics, CardInstance card, int x, int y, int w, int h) {
        CardDefinition def = card.getDefinition();
        if (def == null) return;
        ResourceLocation bgLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/bg/" + def.category().name().toLowerCase() + ".png");
        String fullPath = def.textureLocation().getPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1).replace(".png", "");
        ResourceLocation artLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/art/" + fileName + ".png");
        ResourceLocation borderLoc = ResourceLocation.fromNamespaceAndPath(NiceCard.MODID, "textures/gui/border/" + def.rarity().name().toLowerCase() + ".png");

        drawTexture3x4(graphics, bgLoc, x, y, w, h);
        drawTexture3x4(graphics, artLoc, x, y, w, h);
        drawTexture3x4(graphics, borderLoc, x, y, w, h);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        float scale = (float)w / 90.0f;
        if (scale < 0.6f) scale = 0.6f;
        graphics.pose().scale(scale, scale, 1.0f);
        int tx = (int)(x / scale);
        int ty = (int)(y / scale);
        int margin = 6;
        int tw = (int)(w / scale);

        graphics.drawString(this.font, String.valueOf(card.getCurrentCost()), tx + margin, ty + margin, 0xFF00FFFF, true);
        int atkColor = (card.getAttack() > def.damage()) ? 0xFF00FF00 : 0xFFFFFF00;
        graphics.drawString(this.font, String.valueOf(card.getAttack()), tx + margin, ty + (int)(h/scale) - margin - 8, atkColor, true);
        String hpText = String.valueOf(card.getHealth());
        int hpColor = (card.getHealth() < def.health()) ? 0xFFFF0000 : 0xFFFFFFFF;
        graphics.drawString(this.font, hpText, tx + tw - margin - this.font.width(hpText), ty + (int)(h/scale) - margin - 8, hpColor, true);
        graphics.pose().popPose();
    }

    private void drawTexture3x4(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
        graphics.blit(texture, x, y, w, h, 0, 0, 768, 1024, 768, 1024);
    }

    private void drawSquareTextureCropped(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
        float textureSize = 1024f;
        float srcWidth = textureSize * 0.75f;
        float uMin = (textureSize - srcWidth) / 2.0f;
        graphics.blit(texture, x, y, w, h, (int)uMin, 0, (int)srcWidth, (int)textureSize, (int)textureSize, (int)textureSize);
    }

    private int getHandHoverIndex(List<CardInstance> hand, boolean isMe, int mx, int my) {
        if (hand == null || !isMe) return -1;
        int count = hand.size();
        int maxTotalWidth = this.width - 200;
        int spacing = 70;
        if (count > 1) {
            int neededWidth = (count - 1) * spacing + CARD_WIDTH;
            if (neededWidth > maxTotalWidth) spacing = (maxTotalWidth - CARD_WIDTH) / (count - 1);
        }
        int totalWidth = (count - 1) * spacing + CARD_WIDTH;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height - CARD_HEIGHT + 40;
        for (int i = count - 1; i >= 0; i--) {
            int x = startX + i * spacing;
            int drawY = (selectedHandIndex == i) ? this.height - CARD_HEIGHT - 10 : y;
            if (isHovered(mx, my, x, drawY, CARD_WIDTH, CARD_HEIGHT)) return i;
        }
        return -1;
    }

    private int getBoardHoverIndex(List<CardInstance> board, boolean isMe, int mx, int my) {
        if (board == null || board.isEmpty()) return -1;
        int count = board.size();
        int spacing = MINI_CARD_WIDTH + 10;
        int startX = (this.width - count * spacing) / 2;
        int y = isMe ? (this.height / 2 + 15) : (this.height / 2 - 15 - MINI_CARD_HEIGHT);
        for (int i = 0; i < count; i++) {
            int x = startX + i * spacing;
            if (isHovered(mx, my, x, y, MINI_CARD_WIDTH, MINI_CARD_HEIGHT)) return i;
        }
        return -1;
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void renderHeroZone(GuiGraphics graphics, PlayerState state, boolean isMe, int centerX, int centerY) {
        if (state == null) return;
        int heroX = centerX - HERO_SIZE / 2;
        int heroY = centerY - HERO_SIZE / 2;
        graphics.fill(heroX, heroY, heroX + HERO_SIZE, heroY + HERO_SIZE, 0xFF444444);
        graphics.renderOutline(heroX, heroY, HERO_SIZE, HERO_SIZE, 0xFFFFFFFF);
        String name = state.name == null ? "Unknown" : state.name;
        graphics.drawCenteredString(this.font, name, centerX, heroY - 12, 0xFFFFFFFF);
        graphics.drawString(this.font, "HP:" + state.health, heroX + HERO_SIZE + 2, heroY + HERO_SIZE - 10, 0xFFFF0000);
    }

    private void renderManaBar(GuiGraphics graphics, PlayerState state) {
        if (state == null) return;
        String manaText = state.currentMana + "/" + state.maxMana;
        graphics.drawString(this.font, manaText, this.width - 40, this.height - 30, 0xFF00FFFF);
    }
}