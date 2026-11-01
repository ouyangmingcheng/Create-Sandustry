package com.minechai.create_sandustry.content.steamwhistle;

import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock.WhistleExtenderShape;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class SteamWhistlePerformanceGameTests {
    private SteamWhistlePerformanceGameTests() {
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void capturesEachServerRisingEdgeWithMidi(GameTestHelper helper) {
        SteamWhistlePerformanceProbe.startCapture();

        BlockPos magmaPos = new BlockPos(1, 0, 1);
        BlockPos tankPos = magmaPos.above();
        BlockPos whistlePos = tankPos.above();
        BlockPos sourcePos = new BlockPos(0, 2, 1);
        ServerLevel level = helper.getLevel();
        BlockPos absoluteWhistlePos = helper.absolutePos(whistlePos);

        helper.setBlock(magmaPos, Blocks.MAGMA_BLOCK);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        helper.setBlock(whistlePos, AllBlocks.STEAM_WHISTLE.get());

        // 第一次上升沿；保持通电数 tick，验证不会按“持续鸣响”重复上报。
        helper.runAfterDelay(2, () -> helper.setBlock(sourcePos, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(8, () -> {
            List<SteamWhistlePerformanceEvent> events = SteamWhistlePerformanceProbe.eventsAt(level, absoluteWhistlePos);
            helper.assertTrue(events.size() == 1, "持续通电期间应只捕获一次演奏，实际 " + events.size());
            SteamWhistlePerformanceEvent event = events.getFirst();
            helper.assertTrue(event.getPitch() == 0, "初次演奏音高应为 0，实际 " + event.getPitch());
            helper.assertTrue(event.getMidiNote() == 66, "MEDIUM/0 应映射到 MIDI 66，实际 " + event.getMidiNote());
            helper.assertTrue(!event.getLevel().isClientSide(), "事件必须来自逻辑服务端");
            helper.assertTrue(event.getSourcePos().equals(helper.absolutePos(sourcePos)), "应记录邻居更新来源坐标");
            helper.assertTrue(event.getSourceState().is(Blocks.REDSTONE_BLOCK), "来源坐标当前应为红石块");
            helper.assertTrue(event.isAudibleOnServer(), "岩浆供热的锅炉应被服务端判定为可发声");
            helper.setBlock(sourcePos, Blocks.AIR);
        });

        // 增加一个单格延伸器，使 pitch 变为 1，再制造第二次上升沿。
        helper.runAfterDelay(14, () -> {
            BlockState extension = AllBlocks.STEAM_WHISTLE_EXTENSION.get()
                    .defaultBlockState()
                    .setValue(WhistleExtenderBlock.SIZE, WhistleSize.MEDIUM)
                    .setValue(WhistleExtenderBlock.SHAPE, WhistleExtenderShape.SINGLE);
            helper.setBlock(whistlePos.above(), extension);
        });
        helper.runAfterDelay(18, () -> helper.setBlock(sourcePos, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(26, () -> {
            List<SteamWhistlePerformanceEvent> events = SteamWhistlePerformanceProbe.eventsAt(level, absoluteWhistlePos);
            helper.assertTrue(events.size() == 2, "两次独立上升沿应捕获两次演奏，实际 " + events.size());
            SteamWhistlePerformanceEvent second = events.get(1);
            helper.assertTrue(second.getPitch() == 1, "延伸一格后音高应为 1，实际 " + second.getPitch());
            helper.assertTrue(second.getMidiNote() == 65, "MEDIUM/1 应映射到 MIDI 65，实际 " + second.getMidiNote());
            SteamWhistlePerformanceProbe.stopCapture();
            helper.succeed();
        });
    }
}
