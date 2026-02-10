package com.kaedevicky.nicecard.item;

import com.kaedevicky.nicecard.client.gui.GameBinderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class GameBinderItem extends Item {

    public GameBinderItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC)); // 卡组盒通常比较珍贵
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;

            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, p) -> new GameBinderMenu(id, inv, slotIndex),
                            Component.literal("Deck Builder (0/30)") // 标题可以动态显示数量，但这里先静态
                    ),
                    (buffer) -> buffer.writeInt(slotIndex)
            );
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}