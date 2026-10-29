package com.minechai.create_sandustry.worldgen.geology.rules;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.worldgen.geology.GeologyGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class VolatileSandGameTests {
    private VolatileSandGameTests() {
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void detectsOnlyDirectlyAdjacentLava(GameTestHelper helper) {
        BlockPos adjacentStone = new BlockPos(0, 0, 0);
        helper.setBlock(adjacentStone, Blocks.STONE);
        helper.setBlock(adjacentStone.east(), Blocks.LAVA);

        BlockPos isolatedStone = new BlockPos(2, 0, 2);
        helper.setBlock(isolatedStone, Blocks.STONE);

        BlockPos absoluteAdjacent = helper.absolutePos(adjacentStone);
        BlockPos absoluteIsolated = helper.absolutePos(isolatedStone);
        GeologyGenerationContext context = new GeologyGenerationContext(
                helper.getLevel(),
                RandomSource.create(1L),
                absoluteAdjacent.getX() >> 4,
                absoluteAdjacent.getZ() >> 4,
                128);

        helper.assertTrue(
                VolatileSandRule.touchesLava(context, absoluteAdjacent, new BlockPos.MutableBlockPos()),
                "紧邻岩浆的石头应被识别为岩浆湖屏障");
        helper.assertFalse(
                VolatileSandRule.touchesLava(context, absoluteIsolated, new BlockPos.MutableBlockPos()),
                "未紧邻岩浆的石头不应被识别为岩浆湖屏障");
        helper.succeed();
    }
}
