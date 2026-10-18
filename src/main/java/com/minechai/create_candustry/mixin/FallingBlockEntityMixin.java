package com.minechai.create_candustry.mixin;

import com.minechai.create_candustry.block.BrushableFallProtection;
import com.minechai.create_candustry.block.FallingBlockDropRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版下落方块实体的两组补丁。
 *
 * <p><b>1. 被顶掉时的无掉落规则</b>（{@code no_drops_when_falling_block_displaced}）：
 * 让下落方块在“落点被阻挡、无法正常放置”时跳过原版把下落方块转化为物品掉落的
 * {@code spawnAtLocation} 调用（实体本身仍由原版逻辑正常移除）。</p>
 *
 * <p><b>2. 可刷方块防重力摔碎</b>（{@code prevent_brushable_blocks_breaking_from_falling}）：
 * 原版 {@code BrushableBlock#tick} 在让可疑沙子 / 可疑沙砾 / 猫砂掉落时会调用
 * {@code disableDrop()}，使下落方块落地即被销毁且不掉落（原版无法携带方块实体的考古数据）。
 * 配置开启时：</p>
 * <ul>
 *     <li>在 {@code fall(...)} 移除原方块之前读出考古方块实体数据（战利品表 / 已掷出的战利品 /
 *         刷掘次数），并在返回时挂到下落方块实体的 {@code blockData} 上，
 *         落地后由原版合并逻辑写回新的方块实体；</li>
 *     <li>对受保护方块跳过 {@code disableDrop()}，使原版正常走“落地放置”分支。</li>
 * </ul>
 * 作用范围仅限 {@code BrushableBlock}（可疑沙子、可疑沙砾、猫砂及同类方块）；普通沙子、沙砾、
 * 其它特殊沙子与铁砧完全不受影响。无法放置时仍按无掉落处理（不会转化成物品），与关闭配置时的
 * “落地即销毁”保持一致。
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;",
                    ordinal = 0))
    private ItemEntity createsandustry$skipDropWhenPlacementFailed(FallingBlockEntity self, ItemLike item) {
        return createsandustry$dropOrSkip(self, item);
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;",
                    ordinal = 1))
    private ItemEntity createsandustry$skipDropWhenDisplaced(FallingBlockEntity self, ItemLike item) {
        return createsandustry$dropOrSkip(self, item);
    }

    private static ItemEntity createsandustry$dropOrSkip(FallingBlockEntity self, ItemLike item) {
        if (FallingBlockDropRules.shouldSkipDisplacedDrop(self.getBlockState())) {
            return null;
        }
        return self.spawnAtLocation(item);
    }

    /**
     * 在 {@code fall(...)} 把原方块替换掉之前，读出其方块实体（考古）数据并暂存。
     * 仅对受保护的可刷方块生效；其它方块暂存为 {@code null}（不作任何改动）。
     */
    @Inject(method = "fall", at = @At("HEAD"))
    private static void createsandustry$captureBrushableData(Level level, BlockPos pos, BlockState state,
                                                             CallbackInfoReturnable<FallingBlockEntity> cir) {
        BrushableFallProtection.stashPendingBlockData(
                BrushableFallProtection.captureBlockEntityData(level, pos, state));
    }

    /**
     * 把暂存的考古数据交给新建的下落方块实体携带：落地放置后，原版会把它合并进新的方块实体，
     * 从而保留战利品表 / 战利品与刷掘进度。
     */
    @Inject(method = "fall", at = @At("RETURN"))
    private static void createsandustry$attachBrushableData(Level level, BlockPos pos, BlockState state,
                                                            CallbackInfoReturnable<FallingBlockEntity> cir) {
        CompoundTag data = BrushableFallProtection.takePendingBlockData();
        FallingBlockEntity entity = cir.getReturnValue();
        if (data != null && entity != null && BrushableFallProtection.isProtected(entity.getBlockState())) {
            entity.blockData = data;
        }
    }

    /** 配置开启时，受保护的可刷方块不再调用 {@code disableDrop()}，落地时按原版正常放置方块。 */
    @Inject(method = "disableDrop", at = @At("HEAD"), cancellable = true)
    private void createsandustry$keepBrushableIntact(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (BrushableFallProtection.isProtected(self.getBlockState())) {
            ci.cancel();
        }
    }
}
