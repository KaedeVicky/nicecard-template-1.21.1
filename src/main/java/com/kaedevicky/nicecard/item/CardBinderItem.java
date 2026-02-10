package com.kaedevicky.nicecard.item;

import com.kaedevicky.nicecard.client.gui.CardBinderMenu;
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

public class CardBinderItem extends Item {

    public CardBinderItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 获取卡册所在的格子索引
            int slotIndex = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;

            // 修复：使用 openMenu 的重载方法，传入一个 Consumer<FriendlyByteBuf> 来写数据
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, p) -> new CardBinderMenu(id, inv, slotIndex),
                            Component.literal("Card Binder")
                    ),
                    // 这里的 lambda 就是负责写包的：把 slotIndex 写进去
                    (buffer) -> buffer.writeInt(slotIndex)
            );
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}