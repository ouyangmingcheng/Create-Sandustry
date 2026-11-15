package com.minechai.create_sandustry.content.compat.jade;

import java.util.Locale;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.block.machine.kinetic_press.KineticPressBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/** Compact, server-authoritative details without sending inventories or task results. */
public enum KineticPressJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "kinetic_press");
    static final String DATA_KEY = "createsandustry:kinetic_press";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof KineticPressBlockEntity press)) return;
        CompoundTag status = new CompoundTag();
        status.putInt("Energy", press.getEnergyStored());
        status.putInt("Capacity", press.getMaxEnergyStored());
        status.putInt("Height", press.getWorkingHeight());
        status.putString("State", press.getProcessor().getRunState().name().toLowerCase(Locale.ROOT));
        status.putInt("BatchCompleted", press.getProcessor().getBatchCompleted());
        status.putInt("BatchTotal", press.getProcessor().getBatchTotal());
        data.put(DATA_KEY, status);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DATA_KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag status = data.getCompound(DATA_KEY);
        // Replace the generic energy display with one compact line, including
        // when the looked-at face cannot expose the sided energy capability.
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
        tooltip.add(Component.translatable("gui.createsandustry.kinetic_press.goggles.energy",
                status.getInt("Energy"), status.getInt("Capacity")).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gui.createsandustry.kinetic_press.goggles.height",
                status.getInt("Height")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.createsandustry.kinetic_press.goggles.status",
                Component.translatable("gui.createsandustry.kinetic_press.state." + status.getString("State")))
                .withStyle(ChatFormatting.YELLOW));
        if (status.getInt("BatchTotal") > 0) {
            tooltip.add(Component.translatable("gui.createsandustry.kinetic_press.goggles.batch",
                    status.getInt("BatchCompleted"), status.getInt("BatchTotal")).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return 1100; // Jade's generic energy component runs at 1000.
    }
}
