package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.item.SandSwordForms;
import com.minechai.create_sandustry.content.item.SandMasterSwordForms;
import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateSandustry.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createsandustry"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> ModItems.WET_SAND.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WET_SAND.get());
                        output.accept(ModItems.SAND_ALLOY.get());
                        output.accept(ModItems.MOONCRYSTAL.get());
                        output.accept(ModItems.MOONHOP.get());
                        output.accept(ModItems.CHOCOLATE_MOONHOP.get());
                        output.accept(ModItems.MOONHOP_BOARD.get());
                        output.accept(ModItems.SAND_CASING.get());
                        output.accept(ModItems.HARDENED_SAND.get());
                        output.accept(ModItems.PETRIFIED_SAND.get());
                        ModItems.SANDIFIED_ORE_ITEMS.values().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.SAND_MASTER_SWORD_PEDESTAL.get());
                        output.accept(ModItems.VOLATILE_SAND.get());
                        output.accept(ModItems.CAT_LITTER.get());
                        output.accept(ModItems.THUNDER_SAND.get());
                        output.accept(ModItems.LIVING_SAND.get());
                        output.accept(ModItems.QUNYOU_SPAWN_EGG.get());
                        output.accept(ModItems.SAND_BALL.get());
                        output.accept(ModItems.SAND_HEART.get());
                        output.accept(ModItems.GOLDEN_HEART.get());
                        output.accept(ModItems.POTATO_ROCKET.get());
                        output.accept(ModItems.SAND_SOUL.get());
                        output.accept(ModItems.SAND_MASTER_SWORD.get());
                        // 直接给“刚断裂”的那一份（耐久 0），避免创造栏里出现正好处于修复完成状态的破损剑。
                        output.accept(SandMasterSwordForms.createBrokenInstance());
                        output.accept(ModItems.SAND_SWORD.get());
                        output.accept(ModItems.MUSIC_DISC_OPENING.get());
                        output.accept(ModItems.MUSIC_DISC_AWAKENING.get());
                        output.accept(ModItems.DISC_FRAGMENT_AWAKENING.get());
                        output.accept(ModItems.MUSIC_DISC_HEALING.get());
                        output.accept(ModItems.DISC_FRAGMENT_HEALING.get());
                        output.accept(ModItems.MUSIC_DISC_BIRDS.get());
                        output.accept(ModItems.DISC_FRAGMENT_BIRDS.get());
                        output.accept(SandSwordForms.createBrokenInstance());
                        output.accept(ModItems.RESIDUE.get());
                        output.accept(ModItems.MYSTERIOUS_LUMP.get());
                        output.accept(ModItems.FLORINOL_BUCKET.get());
                        output.accept(ModItems.BURNT_RESIDUE.get());
                        output.accept(ModItems.FLORINOL_BATTERY.get());
                        output.accept(ModItems.FLORINOL_SOIL.get());
                        output.accept(ModItems.THERMAL_RELAY.get());
                        output.accept(ModItems.GOLDEN_DIRT.get());
                        output.accept(ModItems.GOLDEN_FARMLAND.get());
                        output.accept(ModItems.AMETHYST_FLOWER_SEEDS.get());
                        output.accept(ModItems.DORMANT_SEEDS.get());
                        output.accept(ModItems.AMETHYST_PETAL.get());
                        output.accept(ModItems.GOLDEN_OAK_SAPLING.get());
                        output.accept(ModItems.GOLDEN_OAK_LEAVES.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private ModCreativeTabs() {
    }
}





