package com.minechai.create_sandustry.content.melody;

import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class MelodyPuzzleGameTests {
    private static final ResourceLocation OPENING = ResourceLocation.fromNamespaceAndPath(
            CreateSandustry.MODID, "opening");
    private static final ResourceLocation AWAKENING = ResourceLocation.fromNamespaceAndPath(
            CreateSandustry.MODID, "awakening");
    private static final ResourceLocation HEALING = ResourceLocation.fromNamespaceAndPath(
            CreateSandustry.MODID, "healing");
    private static final ResourceLocation BIRDS = ResourceLocation.fromNamespaceAndPath(
            CreateSandustry.MODID, "birds");
    private static final List<Integer> OPENING_ANSWER = List.of(
            69, 69, 69, 66, 64, 66, 69, 64, 61,
            64, 66, 69, 68, 64, 66);
    private static final List<Integer> AWAKENING_ANSWER = List.of(64, 66);
    private static final List<Integer> HEALING_ANSWER = List.of(59, 62, 59);
    private static final List<Integer> BIRDS_ANSWER = List.of(69, 68, 69);

    private MelodyPuzzleGameTests() {
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void loadsOpeningDefinitionAndExactAnswer(GameTestHelper helper) {
        MelodyPuzzleDefinition definition = MelodyPuzzleManager.definitionForTesting(OPENING);
        helper.assertTrue(definition != null, "opening 谜题定义必须成功加载");
        helper.assertTrue(definition.answer().equals(OPENING_ANSWER), "opening 答案必须是锁定的 15 音序列");
        helper.assertTrue(definition.inputTimeoutTicks() == 400, "opening 输入超时应为 400 tick（20 秒）");
        helper.assertTrue(definition.range() == 16, "默认监听范围应为 16 格");
        helper.assertTrue(definition.audioRange() == 16, "谜面/谜底音频范围应为 16 格");
        helper.assertTrue(definition.rewards().size() == 1, "opening 应有一个奖励条目");
        helper.assertTrue(definition.rewards().getFirst().item() == ModItems.SAND_SWORD.get(),
                "opening 奖励必须是沙金剑");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void correctNoteAdvancesProgress(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.queueForSessionForTesting(session, 69, true, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.state == MelodyPuzzleState.WAITING_INPUT, "正确首音后应继续等待输入");
        helper.assertTrue(session.progress == 1, "正确首音应推进到 1，实际 " + session.progress);
        cleanup(helper, level, pos, jukebox);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void wrongNoteFailsAndEjectsDisc(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.queueForSessionForTesting(session, 70, true, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.state == MelodyPuzzleState.FAILED, "错误音符必须立即失败");
        helper.assertTrue(jukebox.getTheItem().isEmpty(), "失败后唱片机内不应残留谜面唱片");
        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_OPENING.get(), pos, 2.0D, 1);
        helper.assertItemEntityCountIs(ModItems.SAND_SWORD.get(), pos, 2.0D, 0);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 30)
    public static void fullCorrectMelodyRewardsOnlyAfterSolution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long baseTick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, baseTick);

        for (int i = 0; i < OPENING_ANSWER.size(); i++) {
            long tick = baseTick + i;
            MelodyPuzzleManager.queueForSessionForTesting(session, OPENING_ANSWER.get(i), true, tick);
            MelodyPuzzleManager.tickSession(session, level, tick);
        }

        helper.assertTrue(session.state == MelodyPuzzleState.PLAYING_SOLUTION,
                "完整正确旋律后应进入 PLAYING_SOLUTION，实际 " + session.state);
        helper.assertItemEntityCountIs(ModItems.SAND_SWORD.get(), pos, 2.0D, 0);

        jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
        MelodyPuzzleManager.tickSession(session, level, baseTick + 40);

        helper.assertTrue(jukebox.getTheItem().isEmpty(), "成功后应消耗谜面唱片");
        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_OPENING.get(), pos, 2.0D, 0);
        helper.assertItemEntityCountIs(ModItems.SAND_SWORD.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void inaudibleWhistleDoesNotAdvance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.queueForSessionForTesting(session, 69, false, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.progress == 0, "无实际发声条件的触发不能推进音符");
        helper.assertTrue(session.state == MelodyPuzzleState.WAITING_INPUT, "无发声触发也不能直接判错");
        cleanup(helper, level, pos, jukebox);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void timeoutFails(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.tickSession(session, level, tick + session.definition.inputTimeoutTicks());

        helper.assertTrue(session.state == MelodyPuzzleState.FAILED, "超时后必须失败");
        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_OPENING.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void sameTickMultipleNotesFail(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.queueForSessionForTesting(session, 69, true, tick);
        MelodyPuzzleManager.queueForSessionForTesting(session, 69, true, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.state == MelodyPuzzleState.FAILED, "同一 tick 多音符必须失败");
        helper.assertTrue(session.progress == 0, "同 tick 多音符不能依靠回调顺序推进进度");
        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_OPENING.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void otherDimensionCannotAdvance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "测试服务器应存在下界维度");
        MelodyPuzzleManager.queueNoteInput(nether, helper.absolutePos(pos), 69, true, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.progress == 0, "不同维度的蒸汽笛不能影响谜题");
        cleanup(helper, level, pos, jukebox);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void outOfRangeCannotAdvance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, tick);

        MelodyPuzzleManager.queueNoteInput(level, helper.absolutePos(pos).offset(64, 0, 64), 69, true, tick);
        MelodyPuzzleManager.tickSession(session, level, tick);

        helper.assertTrue(session.progress == 0, "监听范围外蒸汽笛不能推进谜题");
        cleanup(helper, level, pos, jukebox);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void nearestJukeboxOwnsSharedWhistleEvent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos firstPos = new BlockPos(1, 1, 1);
        BlockPos secondPos = new BlockPos(3, 1, 3);
        JukeboxBlockEntity first = placeOpeningJukebox(helper, firstPos);
        JukeboxBlockEntity second = placeOpeningJukebox(helper, secondPos);
        MelodyPuzzleManager.PuzzleSession firstSession = requireSession(helper, level, firstPos);
        MelodyPuzzleManager.PuzzleSession secondSession = requireSession(helper, level, secondPos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(firstSession, level, tick);
        MelodyPuzzleManager.forceWaitingForTesting(secondSession, level, tick);

        BlockPos whistleNearSecond = helper.absolutePos(new BlockPos(3, 1, 4));
        MelodyPuzzleManager.queueNoteInput(level, whistleNearSecond, 69, true, tick);
        MelodyPuzzleManager.tickSession(firstSession, level, tick);
        MelodyPuzzleManager.tickSession(secondSession, level, tick);

        helper.assertTrue(firstSession.progress == 0, "同一事件不能让较远唱片机推进");
        helper.assertTrue(secondSession.progress == 1, "同一事件必须只归最近唱片机");
        cleanup(helper, level, firstPos, first);
        cleanup(helper, level, secondPos, second);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void twoJukeboxesProgressIndependently(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos firstPos = new BlockPos(1, 1, 1);
        BlockPos secondPos = new BlockPos(3, 1, 3);
        JukeboxBlockEntity first = placeOpeningJukebox(helper, firstPos);
        JukeboxBlockEntity second = placeOpeningJukebox(helper, secondPos);
        MelodyPuzzleManager.PuzzleSession firstSession = requireSession(helper, level, firstPos);
        MelodyPuzzleManager.PuzzleSession secondSession = requireSession(helper, level, secondPos);
        long tick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(firstSession, level, tick);
        MelodyPuzzleManager.forceWaitingForTesting(secondSession, level, tick);

        MelodyPuzzleManager.queueForSessionForTesting(firstSession, 69, true, tick);
        MelodyPuzzleManager.queueForSessionForTesting(secondSession, 69, true, tick + 1);
        MelodyPuzzleManager.tickSession(firstSession, level, tick);
        MelodyPuzzleManager.tickSession(secondSession, level, tick + 1);

        helper.assertTrue(firstSession.progress == 1 && secondSession.progress == 1,
                "两个唱片机都应独立推进到 1");
        MelodyPuzzleManager.queueForSessionForTesting(firstSession, 69, true, tick + 2);
        MelodyPuzzleManager.tickSession(firstSession, level, tick + 2);
        helper.assertTrue(firstSession.progress == 2, "第一台应继续独立推进");
        helper.assertTrue(secondSession.progress == 1, "第一台推进不能影响第二台");

        cleanup(helper, level, firstPos, first);
        cleanup(helper, level, secondPos, second);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void breakingJukeboxDropsDiscNotReward(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        placeOpeningJukebox(helper, pos);
        requireSession(helper, level, pos);

        level.destroyBlock(helper.absolutePos(pos), true);

        helper.assertTrue(MelodyPuzzleManager.sessionForTesting(level, helper.absolutePos(pos)) == null,
                "破坏唱片机后必须释放谜题会话");
        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_OPENING.get(), pos, 2.0D, 1);
        helper.assertItemEntityCountIs(ModItems.SAND_SWORD.get(), pos, 2.0D, 0);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void reloadRecoveryRestartsClueInsteadOfSticking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeOpeningJukebox(helper, pos);
        requireSession(helper, level, pos);

        BlockPos absolutePos = jukebox.getBlockPos();
        MelodyPuzzleManager.removeForTesting(level, absolutePos);
        helper.assertTrue(MelodyPuzzleManager.sessionForTesting(level, absolutePos) == null,
                "模拟重载前应先移除内存会话");
        MelodyPuzzleManager.recoverJukebox(level, jukebox);

        MelodyPuzzleManager.PuzzleSession recovered = MelodyPuzzleManager.sessionForTesting(level, absolutePos);
        helper.assertTrue(recovered != null, "重载恢复必须重新建立谜题会话");
        helper.assertTrue(recovered.state == MelodyPuzzleState.PLAYING_CLUE,
                "无法恢复进度时必须安全地重新开始谜面");
        helper.assertTrue(jukebox.getSongPlayer().isPlaying(), "恢复后谜面音频必须重新播放");

        cleanup(helper, level, pos, jukebox);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void loadsAwakeningDefinitionAndExactAnswer(GameTestHelper helper) {
        MelodyPuzzleDefinition definition = MelodyPuzzleManager.definitionForTesting(AWAKENING);
        helper.assertTrue(definition != null, "awakening 谜题定义必须成功加载");
        helper.assertTrue(definition.answer().equals(AWAKENING_ANSWER), "awakening 答案必须是 E4 -> F#4");
        helper.assertTrue(definition.inputTimeoutTicks() == 200, "awakening 输入超时应为 200 tick");
        helper.assertTrue(definition.audioRange() == 16, "awakening 音频范围应为 16 格");
        helper.assertTrue(definition.rewards().size() == 1, "awakening 应有一个奖励条目");
        helper.assertTrue(definition.rewards().getFirst().item() == ModItems.DISC_FRAGMENT_AWAKENING.get(),
                "awakening 奖励必须是唱片残片");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 30)
    public static void awakeningMelodyRewardsDiscFragmentAfterSolution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeAwakeningJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long baseTick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, baseTick);

        MelodyPuzzleManager.queueForSessionForTesting(session, 64, true, baseTick);
        MelodyPuzzleManager.tickSession(session, level, baseTick);
        MelodyPuzzleManager.queueForSessionForTesting(session, 66, true, baseTick + 1);
        MelodyPuzzleManager.tickSession(session, level, baseTick + 1);

        helper.assertTrue(session.state == MelodyPuzzleState.PLAYING_SOLUTION,
                "awakening 正确旋律后应播放谜底");
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_AWAKENING.get(), pos, 2.0D, 0);

        jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
        MelodyPuzzleManager.tickSession(session, level, baseTick + 20);

        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_AWAKENING.get(), pos, 2.0D, 0);
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_AWAKENING.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void loadsHealingDefinitionAndExactAnswer(GameTestHelper helper) {
        MelodyPuzzleDefinition definition = MelodyPuzzleManager.definitionForTesting(HEALING);
        helper.assertTrue(definition != null, "healing 谜题定义必须成功加载");
        helper.assertTrue(definition.answer().equals(HEALING_ANSWER), "healing 答案必须是 B3 -> D4 -> B3");
        helper.assertTrue(definition.inputTimeoutTicks() == 200, "healing 输入超时应为 200 tick");
        helper.assertTrue(definition.audioRange() == 16, "healing 音频范围应为 16 格");
        helper.assertTrue(definition.rewards().size() == 1, "healing 应有一个奖励条目");
        helper.assertTrue(definition.rewards().getFirst().item() == ModItems.DISC_FRAGMENT_HEALING.get(),
                "healing 奖励必须是唱片残片");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 30)
    public static void healingMelodyRewardsDiscFragmentAfterSolution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeHealingJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long baseTick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, baseTick);

        for (int i = 0; i < HEALING_ANSWER.size(); i++) {
            long tick = baseTick + i;
            MelodyPuzzleManager.queueForSessionForTesting(session, HEALING_ANSWER.get(i), true, tick);
            MelodyPuzzleManager.tickSession(session, level, tick);
        }

        helper.assertTrue(session.state == MelodyPuzzleState.PLAYING_SOLUTION,
                "healing 正确旋律后应播放谜底");
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_HEALING.get(), pos, 2.0D, 0);

        jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
        MelodyPuzzleManager.tickSession(session, level, baseTick + 20);

        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_HEALING.get(), pos, 2.0D, 0);
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_HEALING.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void loadsBirdsDefinitionAndExactAnswer(GameTestHelper helper) {
        MelodyPuzzleDefinition definition = MelodyPuzzleManager.definitionForTesting(BIRDS);
        helper.assertTrue(definition != null, "birds 谜题定义必须成功加载");
        helper.assertTrue(definition.answer().equals(BIRDS_ANSWER), "birds 答案必须是 A4 -> G#4 -> A4");
        helper.assertTrue(definition.inputTimeoutTicks() == 200, "birds 输入超时应为 200 tick");
        helper.assertTrue(definition.audioRange() == 16, "birds 音频范围应为 16 格");
        helper.assertTrue(definition.rewards().size() == 1, "birds 应有一个奖励条目");
        helper.assertTrue(definition.rewards().getFirst().item() == ModItems.DISC_FRAGMENT_BIRDS.get(),
                "birds 奖励必须是唱片残片");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 30)
    public static void birdsMelodyRewardsDiscFragmentAfterSolution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        JukeboxBlockEntity jukebox = placeBirdsJukebox(helper, pos);
        MelodyPuzzleManager.PuzzleSession session = requireSession(helper, level, pos);
        long baseTick = level.getGameTime() + 1;
        MelodyPuzzleManager.forceWaitingForTesting(session, level, baseTick);

        for (int i = 0; i < BIRDS_ANSWER.size(); i++) {
            long tick = baseTick + i;
            MelodyPuzzleManager.queueForSessionForTesting(session, BIRDS_ANSWER.get(i), true, tick);
            MelodyPuzzleManager.tickSession(session, level, tick);
        }

        helper.assertTrue(session.state == MelodyPuzzleState.PLAYING_SOLUTION,
                "birds 正确旋律后应播放谜底");
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_BIRDS.get(), pos, 2.0D, 0);

        jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
        MelodyPuzzleManager.tickSession(session, level, baseTick + 20);

        helper.assertItemEntityCountIs(ModItems.MUSIC_DISC_BIRDS.get(), pos, 2.0D, 0);
        helper.assertItemEntityCountIs(ModItems.DISC_FRAGMENT_BIRDS.get(), pos, 2.0D, 1);
        helper.succeed();
    }

    private static JukeboxBlockEntity placeBirdsJukebox(GameTestHelper helper, BlockPos localPos) {
        return placeJukeboxWithDisc(helper, localPos, ModItems.MUSIC_DISC_BIRDS.get());
    }
    private static JukeboxBlockEntity placeHealingJukebox(GameTestHelper helper, BlockPos localPos) {
        return placeJukeboxWithDisc(helper, localPos, ModItems.MUSIC_DISC_HEALING.get());
    }
    private static JukeboxBlockEntity placeAwakeningJukebox(GameTestHelper helper, BlockPos localPos) {
        return placeJukeboxWithDisc(helper, localPos, ModItems.MUSIC_DISC_AWAKENING.get());
    }
    private static JukeboxBlockEntity placeOpeningJukebox(GameTestHelper helper, BlockPos localPos) {
        return placeJukeboxWithDisc(helper, localPos, ModItems.MUSIC_DISC_OPENING.get());
    }

    private static JukeboxBlockEntity placeJukeboxWithDisc(
            GameTestHelper helper, BlockPos localPos, Item disc) {
        helper.setBlock(localPos, Blocks.JUKEBOX);
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(localPos));
        if (jukebox == null) {
            helper.fail("无法在测试位置创建唱片机方块实体");
            return null;
        }
        jukebox.setTheItem(new ItemStack(disc));
        return jukebox;
    }

    private static MelodyPuzzleManager.PuzzleSession requireSession(
            GameTestHelper helper, ServerLevel level, BlockPos localPos) {
        MelodyPuzzleManager.PuzzleSession session = MelodyPuzzleManager.sessionForTesting(
                level, helper.absolutePos(localPos));
        helper.assertTrue(session != null, "谜面唱片插入后必须创建谜题会话");
        return session;
    }

    private static void cleanup(GameTestHelper helper, ServerLevel level, BlockPos localPos, JukeboxBlockEntity jukebox) {
        MelodyPuzzleManager.removeForTesting(level, helper.absolutePos(localPos));
        if (jukebox != null && jukebox.getSongPlayer().isPlaying()) {
            jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
        }
    }
}
