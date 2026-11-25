package com.minechai.create_sandustry.infrastructure;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sifting.SiftingBeltEvents;
import com.minechai.create_sandustry.content.sand.SandFortuneDrops;
import com.minechai.create_sandustry.content.sand.SpecialSandCauldronConversion;
import com.minechai.create_sandustry.content.sand.SpecialSandEffects;
import com.minechai.create_sandustry.content.sand.ThunderSandEffects;
import com.minechai.create_sandustry.content.florinolbattery.FlorinolBatteryBlockEntity;
import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;
import com.minechai.create_sandustry.infrastructure.command.SandustryCommands;
import com.minechai.create_sandustry.compat.create.HardenedSandSandpaperInteraction;
import com.minechai.create_sandustry.compat.create.RocketPotatoIntegration;
import com.minechai.create_sandustry.compat.create.SandSwordDeployerEvents;
import com.minechai.create_sandustry.compat.createstuffadditions.BrassArmorFlorinolCompat;
import com.minechai.create_sandustry.compat.createstuffadditions.CreateStuffAdditionsDiggingCompat;
import com.minechai.create_sandustry.compat.createstuffadditions.PortableDrillFlorinolCompat;
import com.minechai.create_sandustry.content.florinolsoil.FlorinolSoilEvents;
import com.minechai.create_sandustry.content.mooncrystal.MoonCrystalDepotEvents;
import com.minechai.create_sandustry.content.goldenselection.GoldenSelectionEffectEvents;
import com.minechai.create_sandustry.content.moonhop.MoonWorshipEvents;
import com.minechai.create_sandustry.content.sandification.SandHardcoreEvents;
import com.minechai.create_sandustry.content.livingsand.LivingSandEvents;
import com.minechai.create_sandustry.content.qunyou.QunyouEvents;
import com.minechai.create_sandustry.content.florinolbattery.FlorinolToolBindingEvents;
import com.minechai.create_sandustry.content.goldenselection.GoldenSelectionEvents;
import com.minechai.create_sandustry.content.gravity.GravityBouncePhysics;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordEvents;
import com.minechai.create_sandustry.content.sandsword.SandSwordEvents;
import com.minechai.create_sandustry.content.melody.MelodyPuzzleManager;
import com.minechai.create_sandustry.content.moonhop.MoonhopTooltipEvents;
import com.minechai.create_sandustry.content.sandheart.SandHeartEvents;
import com.minechai.create_sandustry.content.sandmastersword.SandSwordPullManager;
import com.minechai.create_sandustry.content.steamwhistle.SteamWhistlePerformanceProbe;
import com.minechai.create_sandustry.content.timebearing.TimeBearingEvents;
import com.minechai.create_sandustry.registry.ModStats;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/** Explicit common event subscriptions, in their original order and priority. */
public final class SandustryEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(SandustryCommands::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GravityBouncePhysics::onPreTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GravityBouncePhysics::onPostTick);
        // 时间轴承：服务端状态、区块生命周期和世界时间增量。
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onLevelUnload);
        NeoForge.EVENT_BUS.register(new SandustryEvents());
        NeoForge.EVENT_BUS.addListener(KineticPressBlockEntity::onNeighborNotify);
        NeoForge.EVENT_BUS.addListener(ThunderSandEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(GoldenSelectionEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(MoonhopTooltipEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(GoldenSelectionEffectEvents::onItemUseFinish);
        NeoForge.EVENT_BUS.addListener(GoldenSelectionEffectEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onLivingBreathe);
        NeoForge.EVENT_BUS.addListener(LivingSandEvents::onLivingDrown);
        NeoForge.EVENT_BUS.addListener(SpecialSandEffects::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(RocketPotatoIntegration::onProjectileTick);
        // Create: Stuff 'N Additions Digging 附魔兼容铲子可挖掘方块。
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PortableDrillFlorinolCompat::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(CreateStuffAdditionsDiggingCompat::onBlockBreak);
        // 弗洛林电池工具绑定：右键总控绑定，Shift+右键解绑，Tooltip 显示绑定信息。
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FlorinolToolBindingEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FlorinolToolBindingEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(FlorinolToolBindingEvents::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(FlorinolToolBindingEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BrassArmorFlorinolCompat::onCriticalHit);
        // 沙金剑 + 沙团：机械手装配时按实际耐久生成动态修复结果。
        NeoForge.EVENT_BUS.addListener(SandSwordDeployerEvents::onRecipeSearch);

        NeoForge.EVENT_BUS.addListener(SpecialSandCauldronConversion::onItemTick);
        NeoForge.EVENT_BUS.addListener(FlorinolSoilEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(MoonCrystalDepotEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(FlorinolSoilEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(FlorinolSoilEvents::onServerStopped);
        // 弗洛林电池：破坏/放置骨架或填充壁时，让总控在下一 tick 及时重新校验。
        NeoForge.EVENT_BUS.addListener(FlorinolBatteryBlockEntity::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(FlorinolBatteryBlockEntity::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, HardenedSandSandpaperInteraction::onRightClickBlock);
        // 机壳传送带 + 溜槽安装筛振筛网。
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, SiftingBeltEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ModStats::onArchaeologyCompleted);
        NeoForge.EVENT_BUS.addListener(ModStats::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(SandFortuneDrops::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(SandHardcoreEvents::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(SandHardcoreEvents::onItemUseFinished);
        NeoForge.EVENT_BUS.addListener(MoonWorshipEvents::onLivingJump);
        NeoForge.EVENT_BUS.addListener(MoonWorshipEvents::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(MoonWorshipEvents::onEffectRemoved);
        NeoForge.EVENT_BUS.addListener(MoonWorshipEvents::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onPlayerClone);
        // 沙化之兆期间被不死图腾救下：将沙心转化为沙之心掉落，并立即清空沙心。
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SandHeartEvents::onTotemUsed);
        // 沙心：生物进入世界（含区块重载）后重施一次临时属性修饰符。
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onEntityJoinLevel);
        // 沙噬剑：破损形态的服务端自动修复计时（每 10 秒自动恢复 1 点耐久，不需要材料）。
        NeoForge.EVENT_BUS.addListener(SandMasterSwordEvents::onPlayerTick);
        // 沙噬剑：破损形态无法进行近战攻击（在 Player#attack 最开始取消）。
        NeoForge.EVENT_BUS.addListener(SandMasterSwordEvents::onAttackEntity);
        // 沙金剑：破损形态同样不能进行近战攻击。
        NeoForge.EVENT_BUS.addListener(SandSwordEvents::onAttackEntity);
        // 无主沙噬剑：服务端拔剑会话、断开/死亡清理，以及持久化生命上限惩罚。
        NeoForge.EVENT_BUS.addListener(SandSwordPullManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SandSwordPullManager::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(SandSwordPullManager::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(SandSwordPullManager::onServerStopped);
        // 群友：受到攻击时打断睡眠/胡萝卜召唤，并在伤害结算前处理原有闪避。
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, QunyouEvents::onIncomingDamage);
        // 技术验证：捕获服务端蒸汽笛上升沿；后续谜题系统应监听 SteamWhistlePerformanceEvent。
        NeoForge.EVENT_BUS.addListener(SteamWhistlePerformanceProbe::onPerformance);
        NeoForge.EVENT_BUS.addListener(MelodyPuzzleManager::onPerformance);
        NeoForge.EVENT_BUS.addListener(MelodyPuzzleManager::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(MelodyPuzzleManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(MelodyPuzzleManager::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(MelodyPuzzleManager::onServerStopped);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CreateSandustry.LOGGER.info("Create Sandustry server starting");
    }

    private SandustryEvents() {}
}
