package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.content.entity.SandMasterSwordBeam;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import org.joml.Vector3f;

/**
 * 破损沙噬剑的自动修复：以<b>玩家</b>为单位、在服务端计时，每 {@value #REPAIR_INTERVAL_TICKS} tick
 * （10 秒）检查一次，<b>无需任何材料</b>自动恢复 1 点耐久；耐久回满后自动变回完整形态。
 *
 * <p><b>计时</b>：使用玩家数据附件 {@code ModAttachments.SAND_MASTER_SWORD_REPAIR_TICKS}
 * （只序列化、不参与客户端同步），随存档持久化，因此：</p>
 * <ul>
 *     <li>退出游戏再登录不会获得“免费修复”——计时只在在线时累加，离线时间不算进度；</li>
 *     <li>退出重登也不会让计时彻底归零导致武器永远修不好；</li>
 *     <li>死亡重生、切换维度沿用同一个玩家实例/存档数据，不会复制物品。</li>
 * </ul>
 *
 * <p><b>每周期只处理一把剑</b>：一次检查最多修复 1 把破损沙噬剑、恢复 1 点耐久；玩家有多把破损沙噬剑时，
 * 固定选择“容器槽位序号最小”的那一把，先修满它，再轮到下一把。</p>
 *
 * <p>不扫描世界实体、不每 tick 遍历背包：只有计时到点时才会遍历一次玩家自己的物品栏。</p>
 */
public final class SandMasterSwordEvents {
    /** 修复周期：10 秒 = 200 tick。 */
    public static final int REPAIR_INTERVAL_TICKS = 200;
    /** 每次成功修复恢复的耐久点数。 */
    public static final int DURABILITY_PER_REPAIR = 1;

    private SandMasterSwordEvents() {
    }

    /**
     * 破损形态无法近战：在 {@code Player#attack} 的最开始就取消这次攻击。
     *
     * <p>只认“主手拿着破损沙噬剑”这一种情况，因此不会影响其它物品、也不会影响完整形态；
     * 取消后原版连伤害判定都不会执行，附魔（例如遗留下来的锋利）自然也带不出伤害。
     * 攻击冷却仍会被调用方重置，所以挥砍节奏不会有异常。</p>
     */
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().is(ModItems.SAND_MASTER_SWORD_BROKEN.get())) {
            event.setCanceled(true);
        }
    }
    /** 是否是受保护的沙噬剑（完整或破损）：这两种形态都不允许被玩家主动丢弃。 */
    public static boolean isProtectedSword(ItemStack stack) {
        return stack.getItem() == ModItems.SAND_MASTER_SWORD.get()
                || stack.getItem() == ModItems.SAND_MASTER_SWORD_BROKEN.get();
    }

    /**
     * 服务端：处理一次“发射剑气”请求（客户端只负责发请求，这里做全部权威校验）。
     *
     * <p>顺序很重要：先校验玩家正处于“沙化之兆”、主手仍是完整沙噬剑、且原版攻击冷却已经完整恢复；再生成剑气，生成成功后
     * 才扣 1 点耐久并重置原版攻击冷却。因此切换武器、武器已破损、冷却未好、实体生成失败等情况都不会扣耐久，
     * 而“扣掉最后一点耐久变成破损形态”时，已经合法生成的剑气会继续飞行。</p>
     */
    public static void fireBeam(ServerPlayer player) {
        if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 只有处于“沙化之兆”期间才能发射剑气；服务端重新校验，不信任客户端。
        if (!player.hasEffect(ModMobEffects.SAND_HARDCORE)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != ModItems.SAND_MASTER_SWORD.get()) {
            return;
        }
        // 与原版近战攻击共用同一套冷却：服务端重新校验，不信任客户端。
        if (player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }

        // 附魔联动：横扫之刃（剑气变大 + 群伤）、火焰附加（拖尾火焰粒子 + 命中点燃）、
        // 击退（命中击退）、锋利（命中结束时生成沙刃龙卷）。
        // 注意：发射瞬间不再生成火焰粒子（会糊住第一人称视线）；火焰粒子只跟随剑气飞行轨迹出现。
        int sweepingLevel = enchantmentLevel(level, stack, Enchantments.SWEEPING_EDGE);
        int fireAspectLevel = enchantmentLevel(level, stack, Enchantments.FIRE_ASPECT);
        int knockbackLevel = enchantmentLevel(level, stack, Enchantments.KNOCKBACK);
        int sharpnessLevel = enchantmentLevel(level, stack, Enchantments.SHARPNESS);

        SandMasterSwordBeam beam = SandMasterSwordBeam.fire(player, sweepingLevel, fireAspectLevel,
                knockbackLevel, sharpnessLevel);
        if (!level.addFreshEntity(beam)) {
            return;   // 发射失败：不扣耐久
        }

        SandMasterSwordItem.consumeOneDurability(stack, player);
        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 1.3F);
        Vec3 eye = player.getEyePosition().add(player.getLookAngle().scale(1.0D));
        level.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.74F, 0.50F), 1.0F), eye.x, eye.y, eye.z, 12,
                0.15D, 0.15D, 0.15D, 0.02D);

    }

    /** 读取物品上某个附魔的等级（附魔不存在或没有该附魔时返回 0）。 */
    private static int enchantmentLevel(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(enchantment).map(stack::getEnchantmentLevel).orElse(0);
    }
    /** 玩家 tick：累加计时，到点后尝试修复一次。 */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int ticks = player.getData(ModAttachments.SAND_MASTER_SWORD_REPAIR_TICKS) + 1;
        if (ticks < REPAIR_INTERVAL_TICKS) {
            player.setData(ModAttachments.SAND_MASTER_SWORD_REPAIR_TICKS, ticks);
            return;
        }
        // 到点：无论这次是否成功修复都重新计时，10 秒后再试一次。
        player.setData(ModAttachments.SAND_MASTER_SWORD_REPAIR_TICKS, 0);
        tryRepairOnce(player);
    }

    /**
     * 一次修复尝试：最多修复 1 把破损沙噬剑、恢复 1 点耐久，<b>不消耗任何物品</b>。
     *
     * @return 本次是否真的恢复了耐久
     */
    private static boolean tryRepairOnce(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int swordSlot = findBrokenSwordSlot(inventory);
        if (swordSlot < 0) {
            return false;
        }
        ItemStack sword = inventory.getItem(swordSlot);

        // 已经被别的途径修满（例如经验修补），或数据异常：直接恢复完整形态。
        if (sword.getDamageValue() <= 0) {
            inventory.setItem(swordSlot, SandMasterSwordForms.toRepaired(sword));
            return false;
        }

        int damage = sword.getDamageValue() - DURABILITY_PER_REPAIR;
        if (damage <= 0) {
            // 耐久回满（40 点）⇒ 变回完整形态，附魔/自定义名称等数据原样保留。
            inventory.setItem(swordSlot, SandMasterSwordForms.toRepaired(sword));
        } else {
            sword.setDamageValue(damage);
        }
        return true;
    }

    /** 找到第一把破损沙噬剑所在的容器槽位（含主手/副手/盔甲槽），找不到返回 -1。 */
    private static int findBrokenSwordSlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.SAND_MASTER_SWORD_BROKEN.get())) {
                return slot;
            }
        }
        return -1;
    }

}
