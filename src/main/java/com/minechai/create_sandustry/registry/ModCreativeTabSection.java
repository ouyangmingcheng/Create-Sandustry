package com.minechai.create_sandustry.registry;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum ModCreativeTabSection {
    MACHINES("machines", () -> ModItems.KINETIC_PRESS.get()),
    MATERIALS("materials", () -> ModItems.SAND_ALLOY.get()),
    NATURE("nature", () -> ModItems.GOLDEN_OAK_SAPLING.get());

    private final Component title;
    private final Supplier<? extends Item> icon;

    ModCreativeTabSection(String id, Supplier<? extends Item> icon) {
        this.title = Component.translatable("creative_tab.createsandustry.section." + id);
        this.icon = icon;
    }

    public Component title() {
        return title;
    }

    public ItemStack icon() {
        return icon.get().getDefaultInstance();
    }
}
