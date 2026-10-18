package com.minechai.create_candustry.interaction;

import com.minechai.create_candustry.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 猫成功进食后，把猫脚下承载的普通沙子转化为猫砂。
 */
public final class CatLitterConversion {
    /** 猫砂转化概率。 */
    private static final float CONVERSION_CHANCE = 0.30F;

    private CatLitterConversion() {
    }

    /**
     * 在猫成功接受喂食后调用一次：检查猫脚下承载方块是否为普通沙子，若是则以
     * {@value #CONVERSION_CHANCE} 的概率原地替换为猫砂。
     *
     * <p>仅在服务端执行；不生成沙子掉落物，不触发挖掘/放置流程。</p>
     */
    public static void tryConvertSandToCatLitter(Cat cat) {
        Level level = cat.level();
        if (level.isClientSide) {
            return;
        }

        BlockPos below = cat.blockPosition().below();
        BlockState state = level.getBlockState(below);
        // 精确方块身份比较：只匹配 minecraft:sand，绝不匹配 minecraft:red_sand。
        if (!state.is(Blocks.SAND)) {
            return;
        }

        if (level.random.nextFloat() < CONVERSION_CHANCE) {
            level.setBlock(below, ModBlocks.CAT_LITTER.get().defaultBlockState(), 3);
        }
    }
}
