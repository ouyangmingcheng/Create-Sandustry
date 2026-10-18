package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

import net.minecraft.resources.ResourceLocation;

/**
 * Direct port of Create's AllSpriteShifts entries for fluid_tank.
 *
 * The connected PNGs are exact byte-for-byte copies of Create's connected
 * texture sheets, just placed in the createsandustry namespace.
 */
public final class FlorinolBatterySprites {
    public static final CTSpriteShiftEntry SIDE = ct("florinol_battery_side", "florinol_battery_connected");
    public static final CTSpriteShiftEntry TOP = ct("florinol_battery_top", "florinol_battery_top_connected");
    public static final CTSpriteShiftEntry INNER = ct("florinol_battery_inner", "florinol_battery_inner_connected");

    private FlorinolBatterySprites() {
    }

    public static void init() {
        // Static initialization creates the Catnip sprite records before the block atlas stitch.
    }

    private static CTSpriteShiftEntry ct(String original, String connected) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, id(original), id(connected));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/" + path);
    }
}
