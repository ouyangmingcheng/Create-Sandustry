package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModMobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 沙团：可正常食用的食物。
 *
 * <p>原版固定食物属性无法表达“随机饱食度”和“饱和度恢复到满值”，因此这里把食物属性中的
 * nutrition / saturation 都设为 0（既保证原版食用动画、音效、手部动作与消耗流程完全走原版，
 * 又不会与自定义恢复逻辑重复生效），在 {@link #finishUsingItem} 中完成自定义结算：</p>
 * <ul>
 *     <li>饱食度随机 +1~6（均匀，含两端，上限 20）；</li>
 *     <li>饱和度恢复到当前饱食度对应的满值；</li>
 *     <li>以 50% 的概率获得固定 {@value #SANDIFICATION_SECONDS} 秒的“沙化”
 *         （{@link ModMobEffects#SANDIFICATION}）；未命中则这次进食不产生沙化。</li>
 * </ul>
 *
 * <p><b>沙团不再直接增加沙心</b>：沙心的增长完全交给“沙化”状态处理（该状态每 10 tick 判定一次，
 * 每次 75% 概率增加 1 个内部单位 = 半颗沙心，见 {@code SandificationEffect}）。
 * 因此这里只负责按概率施加状态，绝不调用 {@code SandHeartManager.addUnits}，
 * 从源头上保证同一次进食不会重复增加沙心。</p>
 *
 * <p>只在服务端结算一次；客户端沿用原版进食表现。</p>
 */
public class SandBallItem extends Item {
    /** 每次进食给予“沙化”的概率：50%。 */
    public static final double SANDIFICATION_CHANCE = 0.5D;
    /** 沙化的固定持续时间：3 秒（= 60 tick）。 */
    public static final int SANDIFICATION_SECONDS = 3;
    /** 1 秒 = 20 tick。 */
    private static final int TICKS_PER_SECOND = 20;

    public SandBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 先走原版流程：播放进食音效、消耗物品、结算（nutrition/saturation 均为 0 的）食物属性。
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            RandomSource random = level.getRandom();
            FoodData foodData = player.getFoodData();
            int food = 1 + random.nextInt(6);
            foodData.setFoodLevel(Math.min(20, foodData.getFoodLevel() + food));
            foodData.setSaturation(foodData.getFoodLevel());
            // 50% 给予沙化；命中后固定 3 秒。按原版 addEffect 规则，已有更长的沙化不会被这次缩短。
            if (random.nextDouble() < SANDIFICATION_CHANCE) {
                player.addEffect(new MobEffectInstance(ModMobEffects.SANDIFICATION,
                        SANDIFICATION_SECONDS * TICKS_PER_SECOND, 0, false, true, true));
            }
        }
        return result;
    }
}
