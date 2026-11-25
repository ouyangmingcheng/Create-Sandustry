package com.minechai.create_sandustry.infrastructure;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.client.SandustryClient;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.content.catlitter.CatLitterBlock;
import com.minechai.create_sandustry.compat.create.CreateHeatCompat;
import com.minechai.create_sandustry.compat.create.RocketPotatoIntegration;
import com.minechai.create_sandustry.compat.createstuffadditions.BrassArmorFlorinolCompat;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordPedestalEvents;
import com.minechai.create_sandustry.network.ModNetwork;
import com.minechai.create_sandustry.registry.ModAdvancementTriggers;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModCapabilities;
import com.minechai.create_sandustry.registry.ModCreativeTabs;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.registry.ModFeatures;
import com.minechai.create_sandustry.registry.ModFluids;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.registry.ModSounds;
import com.minechai.create_sandustry.registry.ModStats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

/** Registration and lifecycle wiring; gameplay belongs to content modules. */
public final class ModBootstrap {
    public static void register(IEventBus modEventBus) {
        ModConfigs.register();
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModAdvancementTriggers.register(modEventBus);
        ModMobEffects.register(modEventBus);
        if (FMLEnvironment.dist.isClient()) {
            SandustryClient.register(modEventBus);
        }

        modEventBus.addListener(ModBootstrap::commonSetup);
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
        ModSounds.register(modEventBus);
        ModRecipeTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        SandustryEvents.register();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        ModBlocks.registerEncasingVariants(event);
        event.enqueueWork(CreateHeatCompat::registerBoilerHeater);
        event.enqueueWork(BrassArmorFlorinolCompat::registerTools);
        CreateSandustry.LOGGER.info("Create Sandustry common setup complete");
    }

    private ModBootstrap() {}
}
