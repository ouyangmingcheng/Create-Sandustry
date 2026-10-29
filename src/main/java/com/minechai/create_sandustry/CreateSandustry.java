package com.minechai.create_sandustry;

import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModCreativeTabs;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModFluids;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModFeatures;
import com.minechai.create_sandustry.registry.ModAdvancementTriggers;
import org.slf4j.Logger;

import com.minechai.create_sandustry.registry.ModAdvancementTriggers;
import com.minechai.create_sandustry.content.belt.SiftingBeltEvents;
import com.minechai.create_sandustry.content.block.CatLitterBlock;
import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.content.block.entity.FlorinolBatteryBlockEntity;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.SandFortuneDrops;
import com.minechai.create_sandustry.content.block.SpecialSandCauldronConversion;
import com.minechai.create_sandustry.content.block.SpecialSandEffects;
import com.minechai.create_sandustry.content.block.ThunderSandBlock;
import com.minechai.create_sandustry.content.block.ThunderSandEffects;
import com.minechai.create_sandustry.content.client.effect.GoldenGlintRenderBuffers;
import com.minechai.create_sandustry.content.client.effect.GoldenSelectionEffectRenderer;
import com.minechai.create_sandustry.content.client.hud.SandHeartBossOverlay;
import com.minechai.create_sandustry.content.client.hud.SandHeartHudRenderer;
import com.minechai.create_sandustry.content.client.render.SandBladeCrescentRenderer;
import com.minechai.create_sandustry.content.client.render.SandMasterSwordBeamRenderer;
import com.minechai.create_sandustry.content.client.event.SandCasingClient;
import com.minechai.create_sandustry.content.client.event.SiftingBeltClientEvents;
import com.minechai.create_sandustry.content.client.event.ThermalBufferItemProperties;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.config.GeologyConfig;
import com.minechai.create_sandustry.content.timebearing.TimeBearingEvents;
import com.minechai.create_sandustry.content.conversion.FlorinolSoilEvents;
import com.minechai.create_sandustry.content.conversion.MoonCrystalDepotEvents;
import com.minechai.create_sandustry.content.compat.create.CreateHeatCompat;
import com.minechai.create_sandustry.content.compat.create.RocketPotatoIntegration;
import com.minechai.create_sandustry.content.compat.create.SandSwordDeployerEvents;
import com.minechai.create_sandustry.content.effect.GoldenSelectionEffectEvents;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.content.effect.MoonWorshipEvents;
import com.minechai.create_sandustry.content.effect.SandHardcoreEvents;
import com.minechai.create_sandustry.content.entity.LivingSandEvents;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.content.entity.QunyouEvents;
import com.minechai.create_sandustry.content.event.GoldenSelectionEvents;
import com.minechai.create_sandustry.content.event.SandMasterSwordPedestalEvents;
import com.minechai.create_sandustry.registry.ModFluids;
import com.minechai.create_sandustry.content.compat.create.HardenedSandSandpaperInteraction;
import com.minechai.create_sandustry.registry.ModCreativeTabs;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.content.item.SandMasterSwordEvents;
import com.minechai.create_sandustry.content.item.SandSwordEvents;
import com.minechai.create_sandustry.network.ModNetwork;
import com.minechai.create_sandustry.content.ponder.CreateSandustryPonderPlugin;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModCapabilities;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModStats;
import com.minechai.create_sandustry.content.command.SandustryCommands;
import com.minechai.create_sandustry.content.sandheart.SandHeartEvents;
import com.minechai.create_sandustry.content.sandheart.SandSwordPullManager;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(CreateSandustry.MODID)
public class CreateSandustry {
    public static final String MODID = "createsandustry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateSandustry(IEventBus modEventBus) {
        ModConfigs.register();
        GeologyConfig.register();
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAdvancementTriggers.register(modEventBus);
        ModMobEffects.register(modEventBus);
        if (FMLEnvironment.dist.isClient()) {
            SiftingBeltClientEvents.init();
            CreateSandustryPonderPlugin.register();
            modEventBus.addListener(GoldenGlintRenderBuffers::registerRenderBuffers);
            SandCasingClient.register(modEventBus);
            ThermalBufferItemProperties.register(modEventBus);
            GoldenSelectionEffectRenderer.register(modEventBus);
            modEventBus.addListener(SandHeartHudRenderer::register);
            // 剑气实体渲染器。
            modEventBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                    event -> event.registerEntityRenderer(ModEntityTypes.SAND_MASTER_SWORD_BEAM.get(),
                            SandMasterSwordBeamRenderer::new));
            // 锋利剑气生成的半月形剑气。
            modEventBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                    event -> event.registerEntityRenderer(ModEntityTypes.SAND_BLADE_CRESCENT.get(),
                            SandBladeCrescentRenderer::new));
            // 末影龙 boss 条上的“沙心挤占”叠加绘制（客户端）。
            NeoForge.EVENT_BUS.addListener(SandHeartBossOverlay::onBossBarProgress);


        }
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModBlocks::onBlockEntityTypeAddBlocks);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ModEntityTypes::registerAttributes);
        // 剑座避雷针特性：只把插有沙金剑的剑座状态加入原版避雷针 POI。
        modEventBus.addListener(SandMasterSwordPedestalEvents::extendPoiTypes);
        modEventBus.addListener(ModStats::onRegister);
        modEventBus.addListener(RocketPotatoIntegration::registerActions);

        // 客户端 → 服务端：发射沙噬剑气请求。
        modEventBus.addListener(ModNetwork::register);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(CatLitterBlock::addBlockEntityType);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(SandustryCommands::register);
        // 时间轴承：服务端状态、区块生命周期和世界时间增量。
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(TimeBearingEvents::onLevelUnload);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(ThunderSandEffects::onServerTick);
        NeoForge.EVENT_BUS.addListener(GoldenSelectionEvents::onItemTooltip);
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

    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModBlocks.registerEncasingVariants(event);
        event.enqueueWork(CreateHeatCompat::registerBoilerHeater);
        LOGGER.info("Create Sandustry common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Create Sandustry server starting");
    }
}
