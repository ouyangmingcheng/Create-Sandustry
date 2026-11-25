package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sand.SandifiedOreDefinitions;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class SandifiedOreRuleGameTests {
    private SandifiedOreRuleGameTests() {
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void mapsEveryOreToRegisteredSandifiedVariant(GameTestHelper helper) {
        Map<Block, Block> mapping = SandifiedOreRule.replacements();
        helper.assertTrue(mapping.size() == SandifiedOreDefinitions.ENTRIES.size(),
                "沙化矿石世界生成映射数量与定义数量不一致");

        for (SandifiedOreDefinitions.Entry entry : SandifiedOreDefinitions.ENTRIES) {
            Block source = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.sourceBlock()));
            Block target = ModBlocks.SANDIFIED_ORES.get(entry.id()).get();
            helper.assertTrue(mapping.get(source) == target,
                    "错误映射：" + entry.sourceBlock() + " -> " + entry.targetBlock());
        }
        helper.succeed();
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void replacesNormalAndDeepslateSources(GameTestHelper helper) {
        BlockPos normalOre = new BlockPos(0, 0, 0);
        BlockPos absolute = helper.absolutePos(normalOre);
        // GameTest randomizes its world origin. This rule scans ONE chunk, so keep
        // both fixtures in that chunk even when the template starts at its east edge.
        BlockPos deepslateOre = new BlockPos((absolute.getX() & 15) == 15 ? -1 : 1, 0, 0);
        helper.setBlock(normalOre, Blocks.COAL_ORE);
        helper.setBlock(deepslateOre, Blocks.DEEPSLATE_COAL_ORE);

        GeologyGenerationContext context = new GeologyGenerationContext(
                helper.getLevel(),
                RandomSource.create(2L),
                absolute.getX() >> 4,
                absolute.getZ() >> 4,
                200000);

        helper.assertTrue(new SandifiedOreRule().applyMappedOres(context),
                "沙化矿石替换规则没有执行任何替换");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(normalOre))
                        .is(ModBlocks.SANDIFIED_ORES.get("hardened_sand_coal_ore").get()),
                "普通煤矿石没有转换为硬化沙煤矿石");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(deepslateOre))
                        .is(ModBlocks.SANDIFIED_ORES.get("petrified_sand_coal_ore").get()),
                "深板岩煤矿石没有转换为石化沙煤矿石");
        helper.succeed();
    }
}
