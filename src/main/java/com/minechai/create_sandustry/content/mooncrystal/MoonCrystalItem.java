package com.minechai.create_sandustry.content.mooncrystal;

import com.minechai.create_sandustry.content.moonhop.MoonWorshipEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MoonCrystalItem extends Item {
    public MoonCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            MoonWorshipEvents.onPlayerEat(player);
        }
        return result;
    }

}
