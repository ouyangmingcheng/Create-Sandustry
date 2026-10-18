package com.minechai.create_candustry.item;

import java.util.List;
import javax.annotation.Nullable;

import com.minechai.create_candustry.effect.ModMobEffects;
import com.minechai.create_candustry.sandheart.SandHeartManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.Tags;

/**
 * 沙噬剑（完整形态）。
 *
 * <p>一把普通的原版剑（{@link SwordItem}，因此攻击判定、冷却、挥砍动画、耐久条、附魔、物品栏行为
 * 全部走原版），在此基础上加了两条特殊规则：</p>
 * <ol>
 *     <li><b>命中施加“沙化”</b>：{@link #hurtEnemy} 只在<b>真正命中</b>时被调用（原版 {@code Player#attack}
 *         在目标 {@code hurt()} 成功后才调用它，且只在服务端），因此挥空、未命中、仅开始攻击都不会触发。
 *         普通目标是<b>二级</b>沙化（6 秒），<b>boss 生物</b>（{@code #c:bosses}：末影龙、凋零以及加入该标签的
 *         模组 boss）则是<b>强制施加的三级</b>沙化（10 秒）——所谓“强制”是因为原版末影龙与凋零都重写了
 *         {@code addEffect} 并恒返回 {@code false}（它们完全无视状态效果），必须走
 *         {@link SandHeartManager#applyEffect} 的 {@code forceAddEffect} 回退路径才写得进去；</li>
 *     <li><b>耐久耗尽不会销毁物品</b>：{@link #postHurtEnemy} 先在<b>副本</b>上跑一遍原版耐久结算
 *         （含耐久附魔减免、创造模式免消耗），因此原版“把物品 shrink 掉”的那一步只会发生在这份副本上；
 *         真实物品要么同步副本的结果（普通损耗），要么在“本应碎掉”时被换成破损形态并放回同一个手部槽位。</li>
 * </ol>
 *
 * <p>物品属性见 {@link #applyProperties(Item.Properties)}：攻击力与原版石剑相同（面板 5 点）、攻击速度 -2.4、
 * 耐久上限 {@value #MAX_DURABILITY}（由 {@link ModTiers#SAND_MASTER_SWORD} 提供）。</p>
 */
public class SandMasterSwordItem extends SwordItem {
    /** 最大耐久：40 点。 */
    public static final int MAX_DURABILITY = 40;
    /** 命中普通目标时施加的“沙化”时长：6 秒 = 120 tick。 */
    public static final int SANDIFICATION_TICKS = 120;
    /** 命中普通目标时施加的“沙化”等级：二级（amplifier = 1）⇒ 每 5 tick 判定一次。 */
    public static final int SANDIFICATION_AMPLIFIER = 1;
    /** 命中 boss 时施加的“沙化”等级：三级（amplifier = 2）⇒ 每 4 tick 判定一次、每次 50% 概率。 */
    public static final int BOSS_SANDIFICATION_AMPLIFIER = 2;
    /** 命中 boss 时施加的“沙化”时长：10 秒 = 200 tick。 */
    public static final int BOSS_SANDIFICATION_TICKS = 200;
    /** 攻击伤害加成：3 + 材质加成 1 = 4，叠加空手基础的 1 点 ⇒ 面板 5 点，与原版石剑一致。 */
    public static final int ATTACK_DAMAGE_BONUS = 3;
    /** 攻击速度修正：-2.4，与原版剑一致。 */
    public static final float ATTACK_SPEED = -2.4F;

    public SandMasterSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /** 完整形态的物品属性：不可堆叠、攻击力/速度同原版石剑、耐久由材质提供。 */
    public static Item.Properties applyProperties(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(ModTiers.SAND_MASTER_SWORD, ATTACK_DAMAGE_BONUS, ATTACK_SPEED));
    }

    /**
     * 固定消耗 1 点耐久（剑气属于固定代价，不经过耐久附魔减免）。
     *
     * <p>如果这一下会把耐久耗尽，则复用现有的破损形态转换：物品被换成
     * {@code createsandustry:sand_master_sword_broken} 并放回同一只手，不会消失。</p>
     */
    public static void consumeOneDurability(ItemStack stack, LivingEntity holder) {
        if (stack.isEmpty()) {
            return;
        }
        int next = stack.getDamageValue() + 1;
        if (next >= stack.getMaxDamage()) {
            holder.setItemInHand(handOf(holder, stack), SandMasterSwordForms.toBroken(stack));
        } else {
            stack.setDamageValue(next);
        }
    }
    /**
     * 成功命中目标后施加“沙化”。
     *
     * <p>返回 {@code true} 与原版剑一致，用于让原版记录“使用物品”统计，并让原版继续调用
     * {@link #postHurtEnemy} 处理耐久。</p>
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            // 近战与剑气共用同一套沙化规则（见 applySandification）。
            applySandification(target, attacker, SANDIFICATION_TICKS, BOSS_SANDIFICATION_TICKS);
        }
        return true;
    }

    /**
     * 沙噬剑“近战与剑气共用”的沙化施加规则，避免两条路径以后出现不同步：
     *
     * <ul>
     *     <li>普通目标：{@code createsandustry:sandification}，amplifier {@value #SANDIFICATION_AMPLIFIER}（二级），
     *         持续时间由调用方传入；</li>
     *     <li>boss 目标（NeoForge 通用标签 {@code #c:bosses}：原版末影龙/凋零 + 自愿加入的模组 boss）：
     *         amplifier {@value #BOSS_SANDIFICATION_AMPLIFIER}（三级），持续时间同样由调用方传入。</li>
     * </ul>
     *
     * <p>施加统一走 {@link SandHeartManager#applyEffect}：普通目标用原版 {@code addEffect}；
     * 对把状态效果整个无视掉的目标（原版末影龙、凋零等，其 {@code addEffect} 恒返回 {@code false}）
     * 会自动回退到 NeoForge 的 {@code forceAddEffect}，因此 boss 的“强制施加”与近战剑完全一致。
     * 免疫判定仍然经过 {@code canBeAffected} 与 {@code MobEffectEvent.Applicable}。</p>
     *
     * <p><b>时长可叠加</b>：如果目标身上已有沙化，本次时长会<b>加到剩余时长上</b>
     * （例如近战每刀 120 tick：第 1 刀 120、第 2 刀 240、第 3 刀 360……），而不是原版那种“取较长者”。
     * 等级取“本次等级”和“目标当前等级”中更高的那个，因此低级命中不会把高级沙化顶掉；
     * 同时因为最终 instance 的等级总是不低于现有等级，原版 {@code MobEffectInstance#update} 会走
     * “同等级刷新时长 / 更高等级升级”分支，不会产生隐藏效果。</p>
     *
     * @param target      命中目标
     * @param source      效果来源（可为 {@code null}；近战传攻击者，剑气传发射者）
     * @param normalTicks 普通目标的持续时间（tick）
     * @param bossTicks   boss 目标的持续时间（tick）
     */
    public static void applySandification(LivingEntity target, @Nullable Entity source, int normalTicks, int bossTicks) {
        boolean boss = target.getType().is(Tags.EntityTypes.BOSSES);
        int amplifier = boss ? BOSS_SANDIFICATION_AMPLIFIER : SANDIFICATION_AMPLIFIER;
        int duration = boss ? bossTicks : normalTicks;

        MobEffectInstance existing = target.getEffect(ModMobEffects.SANDIFICATION);
        if (existing != null) {
            amplifier = Math.max(amplifier, existing.getAmplifier());
            // 无限时长的沙化（命令等方式）保持无限，否则在剩余时长上累加本次时长。
            duration = existing.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION
                    : existing.getDuration() + duration;
        }

        MobEffectInstance effect = new MobEffectInstance(ModMobEffects.SANDIFICATION, duration, amplifier, false, true,
                true);
        SandHeartManager.applyEffect(target, effect, source);
    }

    /**
     * 攻击命中后的耐久结算：规则与原版剑相同，唯一区别是“耐久耗尽”时转为破损形态而不是销毁物品。
     */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (stack.isEmpty() || !(attacker.level() instanceof ServerLevel)) {
            return;
        }
        // 在副本上跑原版结算：耐久附魔的减免、创造模式免消耗、统计等全部按原版执行，
        // 但即使副本被判为“碎掉”，真实物品也不会被原版从槽位里移除。
        ItemStack probe = stack.copy();
        probe.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        if (!probe.isEmpty()) {
            // 普通损耗（或被耐久附魔完全挡下）：把结果写回真实物品。
            stack.setDamageValue(probe.getDamageValue());
            return;
        }
        // 原版会在这里把剑销毁 —— 改为换成破损形态，放回同一只手的槽位，附魔与自定义数据全部保留。
        attacker.setItemInHand(handOf(attacker, stack), SandMasterSwordForms.toBroken(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.createsandustry.sand_master_sword.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    /** 判断这次命中用的是主手还是副手，与沙噬剑所在槽位保持一致。 */
    private static InteractionHand handOf(LivingEntity attacker, ItemStack stack) {
        return attacker.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}