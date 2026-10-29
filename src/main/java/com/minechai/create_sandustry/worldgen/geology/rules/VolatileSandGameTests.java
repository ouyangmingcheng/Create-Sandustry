package com.minechai.create_sandustry.worldgen.geology.rules;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.worldgen.geology.GeologyConfigSnapshot;
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

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void radiatesThroughHardenedSandNearLava(GameTestHelper helper) {
        BlockPos target = new BlockPos(2, 0, 2);
        helper.setBlock(target, ModBlocks.HARDENED_SAND.get());
        helper.setBlock(target.north(), Blocks.LAVA);
        helper.setBlock(target.south(), Blocks.LAVA);
        helper.setBlock(target.east(), Blocks.LAVA);
        helper.setBlock(target.west(), Blocks.LAVA);

        BlockPos absolute = helper.absolutePos(target);
        GeologyGenerationContext context = new GeologyGenerationContext(
                helper.getLevel(),
                RandomSource.create(3L),
                absolute.getX() >> 4,
                absolute.getZ() >> 4,
                200000);

        helper.assertTrue(new VolatileSandRule().apply(context, GeologyConfigSnapshot.current()),
                "岩浆湖辐射规则没有执行任何替换");
        helper.assertTrue(
                helper.getLevel().getBlockState(absolute).is(ModBlocks.VOLATILE_SAND.get()),
                "岩浆湖周围的硬化沙子没有转化为烈性沙子");
        helper.succeed();
    }
}
