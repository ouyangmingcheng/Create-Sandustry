package com.minechai.create_sandustry.content.sand;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class WetSandGameTests {
    private WetSandGameTests() {
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void supportsSugarcaneAndCactus(GameTestHelper helper) {
        BlockPos sugarcaneSoil = new BlockPos(0, 0, 2);
        BlockPos sugarcanePos = sugarcaneSoil.above();
        helper.setBlock(sugarcaneSoil, ModBlocks.WET_SAND.get());
        helper.setBlock(sugarcaneSoil.north(), Blocks.WATER);
        BlockState sugarcane = Blocks.SUGAR_CANE.defaultBlockState();
        helper.assertTrue(sugarcane.canSurvive(helper.getLevel(), helper.absolutePos(sugarcanePos)),
                "湿沙子旁有水源时应能种植甘蔗");

        helper.setBlock(sugarcaneSoil.north(), Blocks.AIR);
        helper.assertTrue(!sugarcane.canSurvive(helper.getLevel(), helper.absolutePos(sugarcanePos)),
                "湿沙子不应绕过甘蔗原有的水源要求");

        BlockPos cactusSoil = new BlockPos(1, 0, 1);
        BlockPos cactusPos = cactusSoil.above();
        helper.setBlock(cactusSoil, ModBlocks.WET_SAND.get());
        BlockState cactus = Blocks.CACTUS.defaultBlockState();
        helper.assertTrue(cactus.canSurvive(helper.getLevel(), helper.absolutePos(cactusPos)),
                "湿沙子上应能种植仙人掌");

        helper.setBlock(cactusPos.above(), Blocks.WATER);
        helper.assertTrue(!cactus.canSurvive(helper.getLevel(), helper.absolutePos(cactusPos)),
                "湿沙子上方有液体时仙人掌仍应无法存活");

        helper.succeed();
    }
}
