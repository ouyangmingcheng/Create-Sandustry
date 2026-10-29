package com.minechai.create_sandustry.mixin.aeronautics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.machine.ThermalBufferBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让航空学的浮空混合液（levitite blend）把正在供热的热能缓冲器识别为热源。
 *
 * <p>Aeronautics 的 {@code LevititeBlendHelper#getContextFromBlock} 只按方块标签判断热源，
 * 无法区分同一方块的热/冷状态。这里在方法开头补充状态判断：只有未被红石封闭且
 * 热等级不低于 {@link HeatLevel#SMOULDERING} 的热能缓冲器才返回标准结晶上下文。</p>
 *
 * <p>使用 {@link Pseudo} + 字符串目标，Aeronautics 未安装时该 mixin 会被跳过；
 * 上下文对象通过反射获取，避免为本模组引入 Aeronautics 的编译期依赖。</p>
 */
@Pseudo
@Mixin(targets = "dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendHelper", remap = false)
public abstract class LevititeBlendHelperMixin {
    private static volatile Object createsandustry$standardContext;
    private static volatile boolean createsandustry$warned;

    @Inject(method = "getContextFromBlock", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$thermalRelayAsCatalyzer(Level level, BlockPos pos,
                                                                CallbackInfoReturnable<Object> callback) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.THERMAL_RELAY.get()) || state.getValue(ThermalBufferBlock.POWERED)) {
            return;
        }

        HeatLevel heatLevel = state.getValue(ThermalBufferBlock.HEAT_LEVEL);
        if (!heatLevel.isAtLeast(HeatLevel.SMOULDERING)) {
            return;
        }

        Object context = createsandustry$getStandardContext();
        if (context != null) {
            callback.setReturnValue(context);
        }
    }

    private static Object createsandustry$getStandardContext() {
        Object cached = createsandustry$standardContext;
        if (cached != null) {
            return cached;
        }

        try {
            Class<?> holder = Class.forName(
                    "dev.eriksonn.aeronautics.index.AeroLevititeBlendPropagationContexts",
                    true, LevititeBlendHelperMixin.class.getClassLoader());
            Field field = holder.getField("STANDARD_CONTEXT");
            Object registryObject = field.get(null);
            Object context = createsandustry$resolveRegistryObject(registryObject);
            if (context != null) {
                createsandustry$standardContext = context;
            }
            return context;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!createsandustry$warned) {
                createsandustry$warned = true;
                CreateSandustry.LOGGER.warn(
                        "Unable to resolve Aeronautics levitite standard context; thermal relay heat integration disabled",
                        exception);
            }
            return null;
        }
    }

    private static Object createsandustry$resolveRegistryObject(Object registryObject) throws ReflectiveOperationException {
        if (registryObject instanceof Supplier<?> supplier) {
            return supplier.get();
        }
        Method get = registryObject.getClass().getMethod("get");
        get.setAccessible(true);
        return get.invoke(registryObject);
    }
}