package com.minechai.create_candustry;

import org.slf4j.Logger;

import com.minechai.create_candustry.advancement.ModAdvancementTriggers;
import com.minechai.create_candustry.block.CatLitterBlock;
import com.minechai.create_candustry.block.entity.ModBlockEntities;
import com.minechai.create_candustry.block.ModBlocks;
import com.minechai.create_candustry.block.SandFortuneDrops;
import com.minechai.create_candustry.block.SpecialSandCauldronConversion;
import com.minechai.create_candustry.block.SpecialSandEffects;
import com.minechai.create_candustry.block.ThunderSandBlock;
import com.minechai.create_candustry.block.ThunderSandEffects;
import com.minechai.create_candustry.client.GoldenGlintRenderBuffers;
import com.minechai.create_candustry.client.GoldenSelectionEffectRenderer;
import com.minechai.create_candustry.client.SandHeartBossOverlay;
import com.minechai.create_candustry.client.SandHeartHudRenderer;
import com.minechai.create_candustry.client.SandMasterSwordBeamRenderer;
import com.minechai.create_candustry.config.ModConfigs;
import com.minechai.create_candustry.effect.GoldenSelectionEffectEvents;
import com.minechai.create_candustry.effect.ModMobEffects;
import com.minechai.create_candustry.effect.SandHardcoreEvents;
import com.minechai.create_candustry.entity.LivingSandEvents;
import com.minechai.create_candustry.entity.ModEntityTypes;
import com.minechai.create_candustry.event.GoldenSelectionEvents;
import com.minechai.create_candustry.fluid.ModFluids;
import com.minechai.create_candustry.interaction.HardenedSandSandpaperInteraction;
import com.minechai.create_candustry.item.ModCreativeTabs;
import com.minechai.create_candustry.item.ModItems;
import com.minechai.create_candustry.item.SandMasterSwordEvents;
import com.minechai.create_candustry.network.ModNetwork;
import com.minechai.create_candustry.recipe.ModRecipeTypes;
import com.minechai.create_candustry.registry.ModAttachments;
import com.minechai.create_candustry.registry.ModCapabilities;
import com.minechai.create_candustry.registry.ModDataComponents;
import com.minechai.create_candustry.registry.ModStats;
import com.minechai.create_candustry.sandheart.SandHeartEvents;
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
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAdvancementTriggers.register(modEventBus);
        ModMobEffects.register(modEventBus);
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(GoldenGlintRenderBuffers::registerRenderBuffers);
            GoldenSelectionEffectRenderer.register(modEventBus);
            modEventBus.addListener(SandHeartHudRenderer::register);
            // 剑气实体渲染器。
            modEventBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                    event -> event.registerEntityRenderer(ModEntityTypes.SAND_MASTER_SWORD_BEAM.get(),
                            SandMasterSwordBeamRenderer::new));
            // 末影龙 boss 条上的“沙心挤占”叠加绘制（客户端）。
            NeoForge.EVENT_BUS.addListener(SandHeartBossOverlay::onBossBarProgress);
        }
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCapabilities::registerCapabilities);
        modEventBus.addListener(ModEntityTypes::registerAttributes);
        modEventBus.addListener(ModStats::onRegister);

        // 客户端 → 服务端：发射沙噬剑气请求。
        modEventBus.addListener(ModNetwork::register);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        modEventBus.addListener(CatLitterBlock::addBlockEntityType);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

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
        NeoForge.EVENT_BUS.addListener(SpecialSandCauldronConversion::onItemTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, HardenedSandSandpaperInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ModStats::onArchaeologyCompleted);
        NeoForge.EVENT_BUS.addListener(ModStats::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(SandFortuneDrops::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(SandHardcoreEvents::onEffectExpired);
        NeoForge.EVENT_BUS.addListener(SandHardcoreEvents::onItemUseFinished);
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onPlayerClone);
        // 沙心：生物进入世界（含区块重载）后重施一次临时属性修饰符。
        NeoForge.EVENT_BUS.addListener(SandHeartEvents::onEntityJoinLevel);
        // 沙噬剑：破损形态的服务端自动修复计时（每 10 秒自动恢复 1 点耐久，不需要材料）。
        NeoForge.EVENT_BUS.addListener(SandMasterSwordEvents::onPlayerTick);
        // 沙噬剑：破损形态无法进行近战攻击（在 Player#attack 最开始取消）。
        NeoForge.EVENT_BUS.addListener(SandMasterSwordEvents::onAttackEntity);

    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Create Sandustry common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Create Sandustry server starting");
    }
}











