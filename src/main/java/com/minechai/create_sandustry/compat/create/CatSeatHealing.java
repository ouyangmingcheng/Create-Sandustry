package com.minechai.create_sandustry.compat.create;

import com.minechai.create_sandustry.compat.minecraft.CatLitterConversion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Create 机械手对“坐在坐垫上的猫”执行专用喂食治疗。
 *
 * <p>背景：机械手调用 {@code entity.interact} 时，{@code Cat#mobInteract} 的治疗分支被
 * {@code isOwnedBy(player)}（对象恒等比较）门控，机械手假玩家永远不等于真实所有者，于是落到
 * 繁殖/催熟或“坐下/站起切换”分支，导致猫反复蹲起且不真正治疗。因此不再走普通交互，而是当猫
 * 坐在 Create 坐垫（{@code create:seat} 实体）上且受伤时，在进入 {@code mobInteract} 之前直接
 * 执行一次明确的治疗。</p>
 *
 * <p>本类通过实体类型 ID（{@code create:seat}）识别坐垫，通过类名识别
 * {@code DeployerFakePlayer}，因此对 Create 保持可选依赖：不安装 Create 时两个判断都会失败，
 * 不会抛出 {@link NoClassDefFoundError}。</p>
 */
public final class CatSeatHealing {
    private static final String DEPLOYER_FAKE_PLAYER_CLASS =
            "com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer";

    private CatSeatHealing() {
    }

    /**
     * 尝试执行一次机械手专用喂食治疗。
     *
     * @return {@code true} 表示已执行专用治疗，调用方应终止本次普通交互；
     *         {@code false} 表示不满足专用条件，应继续原版交互。
     */
    public static boolean tryHeal(Cat cat, Player player, InteractionHand hand) {
        // 仅在服务端执行。
        if (cat.level().isClientSide) {
            return false;
        }
        // 必须由 Create 机械手发起，普通玩家走原版逻辑。
        if (!isDeployerFakePlayer(player)) {
            return false;
        }
        // 只处理已驯服的猫，不对野猫执行驯服。
        if (!cat.isTame()) {
            return false;
        }
        // 猫必须真实坐在 Create 坐垫上（乘坐 create:seat 实体）。
        if (!isCatOnCreateSeat(cat)) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        // 使用原版猫食物判断，不硬编码鱼类列表。
        if (!cat.isFood(stack)) {
            return false;
        }
        // 只有受伤才治疗；满血不消耗食物、不治疗。
        if (!(cat.getHealth() < cat.getMaxHealth())) {
            return false;
        }

        // 复用原版治疗数值：food.nutrition()，与 Cat#mobInteract 的治疗分支一致。
        FoodProperties food = stack.getFoodProperties(cat);
        float nutrition = food != null ? (float) food.nutrition() : 1.0F;
        cat.heal(nutrition);

        // 消耗机械手真实持有的物品（DeployerFakePlayer 的主手就是机械手当前物品）。
        stack.consume(1, player);

        // 与原版 usePlayerItem 相同的进食反馈。
        cat.playSound(SoundEvents.CAT_EAT, 1.0F, 1.0F);

        // 治疗成功后触发一次猫砂转换判定。
        CatLitterConversion.tryConvertSandToCatLitter(cat);
        return true;
    }

    private static boolean isDeployerFakePlayer(Player player) {
        if (!(player instanceof FakePlayer)) {
            return false;
        }
        return DEPLOYER_FAKE_PLAYER_CLASS.equals(player.getClass().getName());
    }

    private static boolean isCatOnCreateSeat(Cat cat) {
        Entity vehicle = cat.getVehicle();
        if (vehicle == null) {
            return false;
        }
        ResourceLocation id = EntityType.getKey(vehicle.getType());
        return "create".equals(id.getNamespace()) && "seat".equals(id.getPath());
    }
}
