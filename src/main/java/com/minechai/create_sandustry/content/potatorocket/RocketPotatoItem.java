package com.minechai.create_sandustry.content.potatorocket;

import com.minechai.create_sandustry.compat.create.RocketPotatoIntegration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RocketPotatoItem extends Item {
    public RocketPotatoItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel serverLevel) {
            RocketPotatoIntegration.detonateConsumed(serverLevel, entity.position());
        }
        return result;
    }
}
