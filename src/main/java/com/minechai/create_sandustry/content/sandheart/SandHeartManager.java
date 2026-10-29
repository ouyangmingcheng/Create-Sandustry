package com.minechai.create_sandustry.content.sandheart;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModMobEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 沙心系统的核心：数量、单位换算、有效生命上限与“填满”判定。
 *
 * <p><b>单位</b>：内部统一以“半颗沙心”为一个单位，1 个完整沙心 = {@value #UNITS_PER_HEART} 个单位。
 * 由于原版 1 颗心 = 2 点生命值，因此 1 个内部单位恰好等于 1 点生命值，沙心占用的生命上限 = 单位数。</p>
 *
 * <p><b>有效生命上限</b>：使用一个独立的、可识别的属性修饰符
 * {@value #MODIFIER_ID_PATH} 在 {@code minecraft:generic.max_health} 上施加
 * {@code -沙心占用量}（{@link AttributeModifier.Operation#ADD_VALUE}），而不是改写基础值。
 * 计算“未受沙心影响的外部最大生命值”时会把该修饰符排除掉，因此与其他真正修改最大生命值的模组兼容。
 * 原版 {@code max_health} 属性的取值范围下限为 1，因此有效上限天然不会降到 0 以下；
 * 这里再用 {@link #MIN_EFFECTIVE_MAX_HEALTH} 作为显式安全下限。</p>
 *
 * <p><b>适用范围</b>：玩家与生物通用——生物同样用一个附件记录“虚拟沙心”，并同样通过
 * {@code max_health} 上的负向修饰符挤占生命上限（生物没有心形 HUD，所以这份沙心只体现在数值上）。</p>
 *
 * <p>本类不依赖任何客户端类；服务端与客户端都可以读取（客户端读的是同步过来的附件值）。</p>
 */
public final class SandHeartManager {
    /** 1 个完整沙心 = 2 个内部单位（半颗沙心）。 */
    public static final int UNITS_PER_HEART = 2;

    /** 有效生命上限的安全下限（与原版 max_health 属性下限一致）。 */
    public static final double MIN_EFFECTIVE_MAX_HEALTH = 1.0D;

    /** 沙心占位修饰符的 id（用于识别并排除自身）。 */
    public static final String MODIFIER_ID_PATH = "sand_hearts";
    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, MODIFIER_ID_PATH);

    /** 沙化之兆持续时间：2 分钟 = 2400 tick。 */
    public static final int SAND_HARDCORE_DURATION = 2400;

    private SandHeartManager() {
    }

    /** 读取沙心数量（内部单位）。 */
    public static int getUnits(LivingEntity entity) {
        return entity.getData(ModAttachments.SAND_HEARTS);
    }

    /** 直接设置沙心数量（内部单位，自动夹到 [0, 外部最大生命值]），并刷新有效生命上限。 */
    public static void setUnits(LivingEntity entity, int units) {
        int clamped = Math.max(0, Math.min(units, capacityUnits(entity)));
        entity.setData(ModAttachments.SAND_HEARTS, clamped);
        refresh(entity);
    }

    /**
     * 施加状态效果：先走原版 {@link LivingEntity#addEffect(MobEffectInstance, net.minecraft.world.entity.Entity)}，
     * 若该实体把状态效果整个无视掉（例如原版末影龙的 {@code addEffect} 恒返回 false），
     * 再用 NeoForge 的 {@link LivingEntity#forceAddEffect(MobEffectInstance, net.minecraft.world.entity.Entity)} 强制写入。
     *
     * <p>免疫判定不会因此被绕过：两条路径都会经过 {@code canBeAffected} 与 {@code MobEffectEvent.Applicable}。</p>
     *
     * @param source 效果来源（可为 {@code null}），仅用于其它模组的归因/事件
     */
    public static void applyEffect(LivingEntity entity, MobEffectInstance instance,
            @javax.annotation.Nullable net.minecraft.world.entity.Entity source) {
        if (entity.level().isClientSide) {
            return;
        }
        if (!entity.addEffect(instance, source)) {
            entity.forceAddEffect(instance, source);
        }
    }

    /** 增加沙心（内部单位）。 */
    public static void addUnits(LivingEntity entity, int delta) {
        if (delta == 0) {
            return;
        }
        setUnits(entity, getUnits(entity) + delta);
    }


    /** 沙心占据的生命条容量（内部单位）= 外部最大生命值对应的点数。 */
    public static int capacityUnits(LivingEntity entity) {
        return Math.max(0, (int) Math.floor(getExternalMaxHealth(entity)));
    }

    /** 沙心是否已经填满生命条。 */
    public static boolean isFilled(LivingEntity entity) {
        return getUnits(entity) >= capacityUnits(entity);
    }

    /**
     * 计算“未受沙心影响”的外部最大生命值：按照原版 {@code AttributeInstance#calculateValue}
     * 的算法重新计算，但排除沙心自身的修饰符。这样即使其它模组增删最大生命值，
     * 沙心占用与 HUD 仍会重新按外部值计算。
     */
    public static double getExternalMaxHealth(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) {
            return 20.0D;
        }
        double base = instance.getBaseValue();
        double addValue = 0.0D;
        double multipliedBase = 0.0D;
        List<Double> multipliedTotal = new ArrayList<>();
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (MODIFIER_ID.equals(modifier.id())) {
                continue;
            }
            switch (modifier.operation()) {
                case ADD_VALUE -> addValue += modifier.amount();
                case ADD_MULTIPLIED_BASE -> multipliedBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> multipliedTotal.add(modifier.amount());
            }
        }
        double value = base + addValue;
        value += value * multipliedBase;
        for (double amount : multipliedTotal) {
            value *= 1.0D + amount;
        }
        return value;
    }

    /** 当前有效最大生命值（外部值减去沙心占用，遵守安全下限）。 */
    public static double getEffectiveMaxHealth(LivingEntity entity) {
        return Math.max(MIN_EFFECTIVE_MAX_HEALTH, getExternalMaxHealth(entity) - getUnits(entity));
    }

    /**
     * 刷新沙心施加的属性修饰符：把沙心占用量写成 {@code max_health} 上的负向修饰符，
     * 使用 {@code addOrUpdateTransientModifier} 保证不会重复添加；
     * 同时把当前生命值夹到新的有效上限，并在“填满 / 不再填满”时同步沙化之兆（后者立刻安全解除）。
     */
    public static void refresh(LivingEntity entity) {
        // 附件数据以服务端为准（客户端读的是镜像），因此只在服务端真正改写属性与生命值。
        if (entity.level().isClientSide) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            double external = getExternalMaxHealth(entity);
            double reduction = Math.min(getUnits(entity), Math.max(0.0D, external - MIN_EFFECTIVE_MAX_HEALTH));
            if (reduction <= 0.0D) {
                instance.removeModifier(MODIFIER_ID);
            } else {
                instance.addOrUpdateTransientModifier(
                        new AttributeModifier(MODIFIER_ID, -reduction, AttributeModifier.Operation.ADD_VALUE));
            }
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
        syncSandHardcore(entity);
    }

    /**
     * 让“沙化之兆”与沙心占满状态保持一致。
     *
     * <ul>
     *     <li><b>沙心刚刚填满</b> → 施加沙化之兆（已拥有该效果时不重复叠加）；</li>
     *     <li><b>沙心不再完全占据生命条</b>（饮用减少沙心、其它模组提高最大生命值、数据被修正等）
     *         → <b>立刻安全解除</b>沙化之兆。</li>
     * </ul>
     *
     * <p>“安全”指的是通过 {@link LivingEntity#removeEffect(Holder)} 主动移除：这会走 NeoForge 的
     * {@code MobEffectEvent.Remove} 路径，而不是自然耗尽的 {@code MobEffectEvent.Expired}，
     * 因此不会触发沙化死亡，也不会留下任何待执行的死亡标记（本模组本来就不存在延迟死亡任务）。</p>
     */
    public static void syncSandHardcore(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        Holder<net.minecraft.world.effect.MobEffect> effect = ModMobEffects.SAND_HARDCORE;
        if (isFilled(entity)) {
            if (!entity.hasEffect(effect)) {
                applyEffect(entity, new MobEffectInstance(effect, SAND_HARDCORE_DURATION, 0, false, true, true), null);
            }
        } else if (entity.hasEffect(effect)) {
            // 立刻安全解除：Remove 路径不会触发沙化死亡。
            entity.removeEffect(effect);
        }
    }
}
