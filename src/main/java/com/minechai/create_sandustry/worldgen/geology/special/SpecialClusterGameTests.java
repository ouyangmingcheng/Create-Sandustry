package com.minechai.create_sandustry.worldgen.geology.special;

import com.minechai.create_sandustry.CreateSandustry;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class SpecialClusterGameTests {
    private SpecialClusterGameTests() {
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void structureFootprintProtectionAndRadius(GameTestHelper helper) {
        BoundingBox village = new BoundingBox(0, 0, 0, 10, 10, 10);
        NearbyStructureStarts structures = new NearbyStructureStarts(List.of(village), List.of(village), List.of());

        helper.assertFalse(structures.isOutsideAllFootprints(5, 5),
                "结构包围盒内部必须受保护");
        helper.assertFalse(structures.isWithinTargetRadius(5, 5, 16),
                "结构内部不能被识别为外围生成位置");
        helper.assertTrue(structures.isWithinTargetRadius(12, 5, 2),
                "距离结构包围盒两格的位置应处于影响半径内");
        helper.assertFalse(structures.isWithinTargetRadius(13, 5, 2),
                "超出影响半径的位置不应命中");
        helper.assertTrue(structures.isOutsideAllFootprints(12, 5),
                "结构包围盒外的沙地可作为生成位置");
        helper.succeed();
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void roadsAreSampleableButBuildingsRemainProtected(GameTestHelper helper) {
        BoundingBox house = new BoundingBox(0, 0, 0, 10, 10, 10);
        BoundingBox road = new BoundingBox(12, 0, 2, 14, 0, 8);
        NearbyStructureStarts structures = new NearbyStructureStarts(
                List.of(house), List.of(house), List.of(road));

        helper.assertFalse(structures.isWithinTargetRadius(5, 5, 16, true),
                "道路采样不能绕开建筑保护");
        helper.assertFalse(structures.isWithinTargetRadius(13, 5, 1, false),
                "关闭道路采样时不应命中道路包围盒");
        helper.assertTrue(structures.isWithinTargetRadius(13, 5, 1, true),
                "开启道路采样后应允许村庄道路位置");

        SpecialClusterConfigSnapshot config = SpecialClusterConfigSnapshot.current();
        helper.assertTrue(config.catLitter().includeTargetRoads(),
                "猫砂规则没有启用村庄道路采样");
        helper.assertTrue(config.catLitter().roadTargets().contains(Blocks.SMOOTH_SANDSTONE),
                "猫砂道路目标缺少沙漠村庄道路方块 smooth_sandstone");
        helper.assertTrue(config.catLitter().roadTargets().contains(Blocks.DIRT_PATH),
                "猫砂道路目标缺少普通村庄道路方块 dirt_path");
        helper.succeed();
    }
}
