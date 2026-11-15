package com.minechai.create_sandustry.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.item.SandMasterSwordForms;
import com.minechai.create_sandustry.content.item.SandSwordForms;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final int COLUMNS = 9;
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateSandustry.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createsandustry"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> ModItems.SAND_ALLOY.get().getDefaultInstance())
                    .displayItems((parameters, output) -> sections().forEach(section ->
                            section.items().stream()
                                    .filter(stack -> stack.getItem().isEnabled(parameters.enabledFeatures()))
                                    .forEach(output::accept)))
                    .build());

    private static List<SectionContents> sections() {
        List<ItemStack> machines = stacks(
                ModItems.SAND_CASING.get(),
                ModItems.KINETIC_PRESS.get(),
                ModItems.THERMAL_RELAY.get(),
                ModItems.FLORINOL_BATTERY.get());

        List<ItemStack> materials = stacks(
                ModItems.SAND_ALLOY.get(),
                ModItems.RESIDUE.get(),
                ModItems.BURNT_RESIDUE.get(),
                ModItems.MYSTERIOUS_LUMP.get(),
                ModItems.FLORINOL_BUCKET.get(),
                ModItems.MOONCRYSTAL.get(),
                ModItems.MOONHOP.get(),
                ModItems.CHOCOLATE_MOONHOP.get(),
                ModItems.MOONHOP_BLOCK.get(),
                ModItems.MOONHOP_BOARD.get(),
                ModItems.GRAVITY_MECHANISM.get(),
                ModItems.GOLDEN_EXPERIENCE_NUGGET.get(),
                ModItems.GOLDEN_EXPERIENCE_BLOCK.get(),
                ModItems.SAND_HEART.get(),
                ModItems.GOLDEN_HEART.get(),
                ModItems.SAND_SOUL.get(),
                ModItems.SAND_BALL.get(),
                ModItems.SAND_SWORD.get());
        materials.add(SandSwordForms.createBrokenInstance());
        materials.add(ModItems.SAND_MASTER_SWORD.get().getDefaultInstance());
        // Keep the freshly broken form (zero durability), rather than a repaired instance.
        materials.add(SandMasterSwordForms.createBrokenInstance());
        materials.addAll(stacks(
                ModItems.SAND_MASTER_SWORD_PEDESTAL.get(),
                ModItems.POTATO_ROCKET.get(),
                ModItems.QUNYOU_SPAWN_EGG.get(),
                ModItems.MUSIC_DISC_OPENING.get(),
                ModItems.MUSIC_DISC_AWAKENING.get(),
                ModItems.DISC_FRAGMENT_AWAKENING.get(),
                ModItems.MUSIC_DISC_HEALING.get(),
                ModItems.DISC_FRAGMENT_HEALING.get(),
                ModItems.MUSIC_DISC_BIRDS.get(),
                ModItems.DISC_FRAGMENT_BIRDS.get(),
                ModItems.MUSIC_DISC_BIG_NORTHEAST.get()));

        List<ItemStack> nature = stacks(
                ModItems.WET_SAND.get(),
                ModItems.HARDENED_SAND.get(),
                ModItems.PETRIFIED_SAND.get());
        ModItems.SANDIFIED_ORE_ITEMS.values().forEach(item -> nature.add(item.get().getDefaultInstance()));
        nature.addAll(stacks(
                ModItems.VOLATILE_SAND.get(),
                ModItems.THUNDER_SAND.get(),
                ModItems.LIVING_SAND.get(),
                ModItems.CAT_LITTER.get(),
                ModItems.FLORINOL_SOIL.get(),
                ModItems.GOLDEN_DIRT.get(),
                ModItems.GOLDEN_FARMLAND.get(),
                ModItems.GOLDEN_OAK_SAPLING.get(),
                ModItems.GOLDEN_OAK_LEAVES.get(),
                ModItems.DORMANT_SEEDS.get(),
                ModItems.AMETHYST_FLOWER_SEEDS.get(),
                ModItems.AMETHYST_PETAL.get()));

        return List.of(
                new SectionContents(ModCreativeTabSection.MACHINES, machines),
                new SectionContents(ModCreativeTabSection.MATERIALS, materials),
                new SectionContents(ModCreativeTabSection.NATURE, nature));
    }

    /** Only the screen gets empty banner/padding slots; search and JEI retain real items only. */
    public static ScreenLayout createScreenLayout(Collection<ItemStack> displayItems) {
        List<ItemStack> remaining = new ArrayList<>(displayItems);
        List<ItemStack> slots = new ArrayList<>();
        List<SectionBanner> banners = new ArrayList<>();
        for (SectionContents section : sections()) {
            List<ItemStack> matching = new ArrayList<>();
            for (ItemStack expected : section.items()) {
                for (int i = 0; i < remaining.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(expected, remaining.get(i))) {
                        matching.add(remaining.remove(i));
                        break;
                    }
                }
            }
            if (matching.isEmpty()) continue;
            banners.add(new SectionBanner(section.section(), slots.size() / COLUMNS));
            for (int i = 0; i < COLUMNS; i++) slots.add(ItemStack.EMPTY);
            slots.addAll(matching);
            while (slots.size() % COLUMNS != 0) slots.add(ItemStack.EMPTY);
        }
        // Preserve items inserted by other mods or creative-tab event listeners.
        slots.addAll(remaining);
        return new ScreenLayout(List.copyOf(slots), List.copyOf(banners));
    }

    private static List<ItemStack> stacks(ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>(items.length);
        for (ItemLike item : items) stacks.add(new ItemStack(item));
        return stacks;
    }

    private record SectionContents(ModCreativeTabSection section, List<ItemStack> items) {}

    public record SectionBanner(ModCreativeTabSection section, int row) {}

    public record ScreenLayout(List<ItemStack> items, List<SectionBanner> banners) {}

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private ModCreativeTabs() {}
}
