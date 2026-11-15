package com.minechai.create_sandustry.content.moonhop;

import java.util.ArrayList;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class MoonhopPressGameTests {
    private MoonhopPressGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void firstPressUsesPressYAndDimensionTarget(GameTestHelper helper) {
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, -64),
                MoonhopPressLogic.ResultType.DESTROY, "主世界 Y=-64 必须销毁");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, -1),
                MoonhopPressLogic.ResultType.DESTROY, "主世界 Y=-1 必须销毁");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, 0),
                MoonhopPressLogic.ResultType.UPDATE, "主世界 Y=0 必须更新");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, 150),
                MoonhopPressLogic.ResultType.UPDATE, "主世界 Y=150 必须更新");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, 319),
                MoonhopPressLogic.ResultType.UPDATE, "主世界 Y=319 必须更新");

        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 0),
                MoonhopPressLogic.ResultType.UPDATE, "下界 Y=0 必须更新");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 43),
                MoonhopPressLogic.ResultType.UPDATE, "下界 Y=43 必须更新");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 127),
                MoonhopPressLogic.ResultType.UPDATE, "下界 Y=127 必须更新");
        assertResult(helper, MoonhopPressLogic.evaluateFirstPress(Level.END, -999),
                MoonhopPressLogic.ResultType.COMPLETE, "末地首次压制必须直接完成");

        MoonhopPressLogic.Result initial = MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, 150);
        helper.assertTrue(initial.nextState() != null, "首次 UPDATE 必须带新状态");
        helper.assertTrue(initial.nextState().target() == 320, "主世界目标必须是 320");
        helper.assertTrue(initial.nextState().progress() == 150, "首次进度必须等于压机 Y");
        helper.assertTrue(initial.nextState().usedY().equals(List.of(150)), "首次 usedY 必须记录压机 Y");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void cumulativeArithmeticAndDuplicateY(GameTestHelper helper) {
        MoonhopProcessState state = MoonhopPressLogic.evaluateFirstPress(Level.OVERWORLD, 150).nextState();
        MoonhopPressLogic.Result afterNegative = MoonhopPressLogic.evaluateIncomplete(
                MoonhopPressLogic.createIncompleteStack(state), -20);
        assertResult(helper, afterNegative, MoonhopPressLogic.ResultType.UPDATE,
                "150 + (-20) 必须继续更新");
        helper.assertTrue(afterNegative.nextState().progress() == 130, "150 + (-20) 必须等于 130");
        helper.assertTrue(afterNegative.nextState().usedY().equals(List.of(150, -20)),
                "usedY 必须保持实际加工顺序");

        MoonhopPressLogic.Result complete = MoonhopPressLogic.evaluateIncomplete(
                MoonhopPressLogic.createIncompleteStack(afterNegative.nextState()), 190);
        assertResult(helper, complete, MoonhopPressLogic.ResultType.COMPLETE,
                "130 + 190 必须精确完成");

        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(state), 190),
                MoonhopPressLogic.ResultType.DESTROY, "150 + 190 必须超限销毁");
        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(state), -200),
                MoonhopPressLogic.ResultType.DESTROY, "150 + (-200) 必须负数销毁");
        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(state), 150),
                MoonhopPressLogic.ResultType.PASS, "重复 Y 必须放行");

        MoonhopProcessState nether43 = MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 43).nextState();
        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(nether43), 85),
                MoonhopPressLogic.ResultType.COMPLETE, "43 + 85 必须完成");

        MoonhopProcessState nether63 = MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 63).nextState();
        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(nether63), 65),
                MoonhopPressLogic.ResultType.COMPLETE, "63 + 65 必须完成");

        MoonhopProcessState nether64 = MoonhopPressLogic.evaluateFirstPress(Level.NETHER, 64).nextState();
        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(nether64), 64),
                MoonhopPressLogic.ResultType.PASS, "64 + 64 的第二次 Y 必须放行");

        assertResult(helper, MoonhopPressLogic.evaluateIncomplete(
                        MoonhopPressLogic.createIncompleteStack(nether43), 100),
                MoonhopPressLogic.ResultType.DESTROY, "下界 143 必须超限销毁");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void stateValidationAndCodecRoundTrip(GameTestHelper helper) {
        MoonhopProcessState valid = new MoonhopProcessState(320, 130, List.of(150, -20));
        helper.assertTrue(valid.isValid(), "合法状态必须通过校验");
        helper.assertTrue(valid.usedY().equals(List.of(150, -20)), "usedY 必须保持输入顺序");

        MoonhopProcessState duplicate = new MoonhopProcessState(320, 200, List.of(100, 100));
        helper.assertTrue(!duplicate.isValid(), "重复 usedY 必须非法");
        MoonhopProcessState wrongSum = new MoonhopProcessState(320, 200, List.of(100, 101));
        helper.assertTrue(!wrongSum.isValid(), "usedY 总和与 progress 不一致必须非法");

        var encoded = MoonhopProcessState.CODEC.encodeStart(NbtOps.INSTANCE, valid).getOrThrow();
        MoonhopProcessState decoded = MoonhopProcessState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.equals(valid), "Codec 往返必须保持完整状态");
        helper.assertTrue(MoonhopProcessState.CODEC.encodeStart(NbtOps.INSTANCE, duplicate).error().isPresent(),
                "非法状态不得被 Codec 接受");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void outputConstructionSplitsIncompleteBoards(GameTestHelper helper) {
        ItemStack moonhop = new ItemStack(ModItems.MOONHOP.get(), 5);
        MoonhopProcessState state = new MoonhopProcessState(320, 150, List.of(150));
        MoonhopPressLogic.Result result = new MoonhopPressLogic.Result(MoonhopPressLogic.ResultType.UPDATE, state);

        List<ItemStack> single = MoonhopPressLogic.createOutputs(moonhop, result, 1);
        helper.assertTrue(single.size() == 1, "非 bulk 输出必须只有一个中间物品");
        helper.assertTrue(single.getFirst().is(ModItems.INCOMPLETE_MOONHOP_BOARD.get()),
                "非 bulk 输出必须是未完成月跃石板");

        List<ItemStack> bulk = MoonhopPressLogic.createOutputs(moonhop, result, 5);
        helper.assertTrue(bulk.size() == 5, "bulk 输出必须拆成五个 count=1 的中间物品");
        for (ItemStack stack : bulk) {
            helper.assertTrue(stack.getCount() == 1, "未完成月跃石板输出必须 count=1");
            helper.assertTrue(stack.get(ModDataComponents.MOONHOP_PROCESS_STATE.get()) != null,
                    "每个中间物品必须携带独立状态组件");
        }

        MoonhopPressLogic.Result complete = new MoonhopPressLogic.Result(
                MoonhopPressLogic.ResultType.COMPLETE, null);
        List<ItemStack> board = MoonhopPressLogic.createOutputs(moonhop, complete, 5);
        helper.assertTrue(board.size() == 1 && board.getFirst().getCount() == 5,
                "月跃石板应保持普通堆叠规则");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void destroyResultsPlayItemBreakOnce(GameTestHelper helper) {
        SoundCapture capture = new SoundCapture();
        NeoForge.EVENT_BUS.register(capture);
        try {
            MechanicalPressBlockEntity negativePress = placePress(helper, -20);
            TransportedItemStack normalStack = new TransportedItemStack(
                    new ItemStack(ModItems.MOONHOP.get(), 4));
            helper.assertTrue(negativePress.tryProcessOnBelt(normalStack, null, true),
                    "首次负数销毁必须在模拟阶段启动压制");
            helper.assertTrue(capture.itemBreakSounds == 0, "模拟阶段不得播放物品损坏音效");
            helper.assertTrue(negativePress.tryProcessOnBelt(normalStack, new ArrayList<>(), false),
                    "首次负数销毁必须在实际阶段执行");
            helper.assertTrue(capture.itemBreakSounds == 1, "首次负数销毁必须播放一次物品损坏音效");

            ItemStack underflow = incomplete(320, 10, List.of(10));
            TransportedItemStack underflowStack = new TransportedItemStack(underflow);
            helper.assertTrue(negativePress.tryProcessOnBelt(underflowStack, null, true),
                    "后续负数销毁必须在模拟阶段启动压制");
            helper.assertTrue(capture.itemBreakSounds == 1, "后续负数模拟不得播放音效");
            helper.assertTrue(negativePress.tryProcessOnBelt(underflowStack, new ArrayList<>(), false),
                    "后续负数销毁必须在实际阶段执行");
            helper.assertTrue(capture.itemBreakSounds == 2, "后续负数销毁必须播放一次音效");

            MechanicalPressBlockEntity overshootPress = placePress(helper, 100);
            ItemStack overshoot = incomplete(320, 300, List.of(300));
            TransportedItemStack overshootStack = new TransportedItemStack(overshoot);
            helper.assertTrue(overshootPress.tryProcessOnBelt(overshootStack, null, true),
                    "超目标销毁必须在模拟阶段启动压制");
            helper.assertTrue(capture.itemBreakSounds == 2, "超目标模拟不得播放音效");
            helper.assertTrue(overshootPress.tryProcessOnBelt(overshootStack, new ArrayList<>(), false),
                    "超目标销毁必须在实际阶段执行");
            helper.assertTrue(capture.itemBreakSounds == 3, "超目标销毁必须播放一次音效");

            MechanicalPressBlockEntity updatePress = placePress(helper, 20);
            ItemStack update = incomplete(320, 10, List.of(10));
            helper.assertTrue(updatePress.tryProcessOnBelt(
                            new TransportedItemStack(update), new ArrayList<>(), false),
                    "正常累计必须在实际阶段执行");
            ItemStack complete = incomplete(320, 300, List.of(300));
            helper.assertTrue(updatePress.tryProcessOnBelt(
                            new TransportedItemStack(complete), new ArrayList<>(), false),
                    "精确完成必须在实际阶段执行");
            helper.assertTrue(capture.itemBreakSounds == 3, "正常累计和精确完成不得播放损坏音效");

            ItemStack duplicate = incomplete(320, 10, List.of(10));
            helper.assertTrue(!placePress(helper, 10).tryProcessOnBelt(
                            new TransportedItemStack(duplicate), null, true),
                    "重复 Y 必须在模拟阶段放行");
            helper.assertTrue(capture.itemBreakSounds == 3, "重复 Y 不得播放损坏音效");

            ItemStack invalid = new ItemStack(ModItems.INCOMPLETE_MOONHOP_BOARD.get());
            helper.assertTrue(!overshootPress.tryProcessOnBelt(
                            new TransportedItemStack(invalid), null, true),
                    "非法组件必须在模拟阶段放行");
            helper.assertTrue(capture.itemBreakSounds == 3, "非法组件不得播放损坏音效");
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
        }
        helper.succeed();
    }


    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void incompleteStateSurvivesContainerAndSaveLoad(GameTestHelper helper) {
        MoonhopProcessState state = new MoonhopProcessState(320, 130, List.of(150, -20));
        ItemStack original = MoonhopPressLogic.createIncompleteStack(state);

        SimpleContainer container = new SimpleContainer(1);
        ItemStack remainder = container.addItem(original.copy());
        helper.assertTrue(remainder.isEmpty(), "箱式容器必须接受合法中间物品");
        MoonhopProcessState stored = container.getItem(0).get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        helper.assertTrue(state.equals(stored), "容器存取后状态组件必须保持不变");

        var tag = original.saveOptional(helper.getLevel().registryAccess());
        ItemStack loaded = ItemStack.parse(helper.getLevel().registryAccess(), tag).orElse(ItemStack.EMPTY);
        MoonhopProcessState reloaded = loaded.get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        helper.assertTrue(state.equals(reloaded), "保存并重新加载后状态组件必须保持不变");
        helper.assertTrue(MoonhopProgress.barWidth(reloaded.progress(), reloaded.target()) == 5,
                "重新加载后进度条宽度必须与 Tooltip 使用同一状态");
        helper.succeed();
    }


    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void progressBarMathMatchesCreateStyle(GameTestHelper helper) {
        helper.assertTrue(MoonhopProgress.barWidth(0, 320) == 0, "0/320 进度条应为 0px");
        helper.assertTrue(MoonhopProgress.barWidth(1, 320) == 1, "1/320 进度条应至少为 1px");
        helper.assertTrue(MoonhopProgress.barWidth(80, 320) == 3, "80/320 进度条应为 3px");
        helper.assertTrue(MoonhopProgress.barWidth(150, 320) == 6, "150/320 进度条应为 6px");
        helper.assertTrue(MoonhopProgress.barWidth(160, 320) == 6, "160/320 进度条应为 6px");
        helper.assertTrue(MoonhopProgress.barWidth(240, 320) == 9, "240/320 进度条应为 9px");
        helper.assertTrue(MoonhopProgress.barWidth(319, 320) == 12, "319/320 进度条不得提前填满");
        helper.assertTrue(MoonhopProgress.barWidth(43, 128) == 4, "43/128 进度条应为 4px");
        helper.assertTrue(MoonhopProgress.barWidth(127, 128) == 12, "127/128 进度条应为 12px");
        helper.assertTrue(MoonhopProgress.barColor(0.0F) == 0xFFFFC074, "0% 颜色必须是起始橙色");
        helper.assertTrue(MoonhopProgress.barColor(1.0F) == 0xFF46FFE0, "100% 颜色必须是结束青色");
        helper.succeed();
    }


    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void tooltipShowsOnlyValidIncompleteState(GameTestHelper helper) {
        MoonhopProcessState state = new MoonhopProcessState(320, 130, List.of(150, -20));
        ItemStack incomplete = MoonhopPressLogic.createIncompleteStack(state);
        List<Component> tooltip = new ArrayList<>();
        ItemTooltipEvent event = new ItemTooltipEvent(
                incomplete, null, tooltip, TooltipFlag.Default.NORMAL, Item.TooltipContext.EMPTY);
        MoonhopTooltipEvents.onItemTooltip(event);

        helper.assertTrue(tooltip.size() == 7, "合法中间物品必须追加空行、标题、进度、下一步和高度列表");
        helper.assertTrue(tooltip.getFirst().equals(CommonComponents.EMPTY), "Tooltip 首行必须是空行");
        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.GRAY)
                        .equals(tooltip.get(1).getStyle().getColor()),
                "加工标题必须使用 Create 风格灰色");
        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)
                        .equals(tooltip.get(2).getStyle().getColor()),
                "进度文本必须使用 Create 风格深灰色");

        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.AQUA)
                        .equals(tooltip.get(3).getStyle().getColor()),
                "下一步提示必须使用 Create 风格青色");
        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA)
                        .equals(tooltip.get(4).getStyle().getColor()),
                "已使用高度标题必须使用深青色");
        helper.assertTrue(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA)
                        .equals(tooltip.get(5).getStyle().getColor()),
                "已使用高度条目必须使用深青色");
        helper.assertTrue(usedHeight(tooltip.get(5)) == 150, "第一项已使用高度必须是 150");
        helper.assertTrue(usedHeight(tooltip.get(6)) == -20, "第二项已使用高度必须是 -20");
        helper.assertTrue(MoonhopTooltipEvents.percentage(0, 320) == 0, "0/320 必须显示 0%");
        helper.assertTrue(MoonhopTooltipEvents.percentage(130, 320) == 41,
                "Math.round 下 130/320 必须显示 41%");
        helper.assertTrue(MoonhopTooltipEvents.percentage(150, 320) == 47, "Math.round 下 150/320 必须显示 47%");
        helper.assertTrue(MoonhopTooltipEvents.percentage(319, 320) == 100, "319/320 必须显示 100%");
        helper.assertTrue(MoonhopTooltipEvents.percentage(43, 128) == 34, "43/128 必须显示 34%");
        helper.assertTrue(MoonhopTooltipEvents.percentage(127, 128) == 99, "127/128 必须显示 99%");

        List<Component> missingStateTooltip = new ArrayList<>();
        MoonhopTooltipEvents.onItemTooltip(new ItemTooltipEvent(
                new ItemStack(ModItems.INCOMPLETE_MOONHOP_BOARD.get()), null, missingStateTooltip,
                TooltipFlag.Default.NORMAL, Item.TooltipContext.EMPTY));
        helper.assertTrue(missingStateTooltip.isEmpty(), "缺失状态组件时不能追加 Tooltip");

        List<Component> moonhopTooltip = new ArrayList<>();
        MoonhopTooltipEvents.onItemTooltip(new ItemTooltipEvent(
                new ItemStack(ModItems.MOONHOP.get()), null, moonhopTooltip,
                TooltipFlag.Default.NORMAL, Item.TooltipContext.EMPTY));
        helper.assertTrue(moonhopTooltip.isEmpty(), "普通月跃石不能追加加工 Tooltip");
        helper.succeed();
    }


    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void beltMixinTransformsAndPassesDuplicateY(GameTestHelper helper) {
        MechanicalPressBlockEntity press = placePress(helper);
        int pressY = press.getBlockPos().getY();
        helper.assertTrue(pressY > 0 && pressY < 320, "测试压机 Y 必须适合主世界首次压制");

        TransportedItemStack transported = new TransportedItemStack(new ItemStack(ModItems.MOONHOP.get(), 3));
        helper.assertTrue(press.tryProcessOnBelt(transported, null, true),
                "首次压制模拟必须返回 true");

        ArrayList<ItemStack> outputs = new ArrayList<>();
        helper.assertTrue(press.tryProcessOnBelt(transported, outputs, false),
                "首次压制实际阶段必须返回 true");
        int expected = press.canProcessInBulk() ? 3 : 1;
        helper.assertTrue(outputs.size() == expected,
                "输出数量必须遵循 bulkPressing，实际=" + outputs.size() + "，期望=" + expected);
        for (ItemStack output : outputs) {
            helper.assertTrue(output.is(ModItems.INCOMPLETE_MOONHOP_BOARD.get()),
                    "首次输出必须是未完成月跃石板");
            MoonhopProcessState state = output.get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
            helper.assertTrue(state != null && state.progress() == pressY,
                    "首次进度必须使用压机方块 Y");
        }

        TransportedItemStack repeat = new TransportedItemStack(outputs.getFirst().copy());
        helper.assertTrue(!press.tryProcessOnBelt(repeat, null, true),
                "重复 Y 必须在模拟阶段直接放行");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void worldMixinTransformsEntityAtPressY(GameTestHelper helper) {
        MechanicalPressBlockEntity press = placePress(helper);
        int pressY = press.getBlockPos().getY();
        helper.assertTrue(pressY > 0 && pressY < 320, "测试压机 Y 必须适合主世界首次压制");

        ItemEntity entity = new ItemEntity(
                helper.getLevel(), press.getBlockPos().getX() + 0.5D, press.getBlockPos().getY() - 1.0D, press.getBlockPos().getZ() + 0.5D,
                new ItemStack(ModItems.MOONHOP.get()));
        helper.getLevel().addFreshEntity(entity);

        helper.assertTrue(press.tryProcessInWorld(entity, true), "世界物品首次模拟必须返回 true");
        helper.assertTrue(press.tryProcessInWorld(entity, false), "世界物品首次实际加工必须返回 true");
        helper.assertTrue(entity.isAlive(), "转换后原 ItemEntity 必须继续存在");
        helper.assertTrue(entity.getItem().is(ModItems.INCOMPLETE_MOONHOP_BOARD.get()),
                "世界物品必须转换为未完成月跃石板");
        MoonhopProcessState state = entity.getItem().get(ModDataComponents.MOONHOP_PROCESS_STATE.get());
        helper.assertTrue(state != null && state.progress() == pressY,
                "世界物品必须使用压机方块 Y");
        helper.assertTrue(!press.tryProcessInWorld(entity, true),
                "同一实体再次经过相同 Y 必须直接放行");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void jeiBackingRecipeExists(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                CreateSandustry.MODID, "pressing/moonhop_board");
        boolean found = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.PRESSING.getType())
                .stream()
                .anyMatch(holder -> holder.id().equals(recipeId));
        helper.assertTrue(found, "Create 压制配方 data/createsandustry/recipe/pressing/moonhop_board.json 必须加载");
        helper.succeed();
    }

    private static MechanicalPressBlockEntity placePress(GameTestHelper helper) {
        return placePress(helper, 100);
    }

    private static MechanicalPressBlockEntity placePress(GameTestHelper helper, int absoluteY) {
        // GameTest structures are placed deep underground. Use an explicit
        // absolute Y so press arithmetic and destroy cases are reproducible.
        BlockPos localPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos absolutePos = new BlockPos(localPos.getX(), absoluteY, localPos.getZ());
        helper.getLevel().setBlockAndUpdate(absolutePos, AllBlocks.MECHANICAL_PRESS.getDefaultState());
        MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) helper.getLevel().getBlockEntity(absolutePos);
        helper.assertTrue(press != null, "测试位置必须创建机械压机 BlockEntity");
        return press;
    }

    private static ItemStack incomplete(int target, int progress, List<Integer> usedY) {
        return MoonhopPressLogic.createIncompleteStack(new MoonhopProcessState(target, progress, usedY));
    }

    private static final class SoundCapture {
        private int itemBreakSounds;

        @SubscribeEvent
        public void onSound(PlayLevelSoundEvent.AtPosition event) {
            if (event.getSound() != null && event.getSound().value() == SoundEvents.ITEM_BREAK) {
                itemBreakSounds++;
            }
        }
    }


    private static int usedHeight(Component line) {
        for (Component sibling : line.getSiblings()) {
            if (sibling.getContents() instanceof TranslatableContents contents
                    && contents.getArgs().length == 1 && contents.getArgs()[0] instanceof Integer y) {
                return y;
            }
        }
        throw new IllegalArgumentException("Tooltip line does not contain a used height");
    }

    private static void assertResult(GameTestHelper helper, MoonhopPressLogic.Result result,
                                     MoonhopPressLogic.ResultType expected, String message) {
        helper.assertTrue(result.type() == expected, message + "，实际=" + result.type());
    }
}





