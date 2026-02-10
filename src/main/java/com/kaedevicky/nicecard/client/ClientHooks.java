package com.kaedevicky.nicecard.client;

import com.kaedevicky.nicecard.block.GameTableBlockEntity;
import com.kaedevicky.nicecard.client.gui.CardOpenScreen;
import com.kaedevicky.nicecard.client.gui.GameTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class ClientHooks {
    public static void openCardPackScreen(List<ItemStack> cards) {
        Minecraft.getInstance().setScreen(new CardOpenScreen(cards));
    }

    public static void openGameTableScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof GameTableBlockEntity table) {
                mc.setScreen(new GameTableScreen(table));
            }
        }
    }
}