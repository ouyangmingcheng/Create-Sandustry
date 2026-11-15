package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class BlockTags {
        public static final TagKey<Block> FLORINOL_SOIL_CATALYZER =
                createBlock("florinol_soil_catalyzer");

        public static final TagKey<Block> KINETIC_PRESS_INPUTS =
                createBlock("kinetic_press_inputs");

        private static TagKey<Block> createBlock(String path) {
            return TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, path));
        }
    }

    public static final class ItemTags {
        public static final TagKey<Item> FLORINOL_SOIL_CATALYZER =
                createItem("florinol_soil_catalyzer");

        public static final TagKey<Item> KINETIC_PRESS_INPUTS =
                createItem("kinetic_press_inputs");

        private static TagKey<Item> createItem(String path) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, path));
        }
    }

    private ModTags() {
    }
}