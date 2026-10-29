package com.minechai.create_sandustry.content.conversion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

final class FlorinolSoilConversionData extends SavedData {
    private static final String DATA_NAME = "createsandustry_florinol_soil_conversion";
    private static final String TICKERS_TAG = "Tickers";

    private final List<FlorinolSoilConversionManager.PendingConversion> tickers = new ArrayList<>();

    static FlorinolSoilConversionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FlorinolSoilConversionData::new, FlorinolSoilConversionData::load),
                DATA_NAME);
    }

    List<FlorinolSoilConversionManager.PendingConversion> tickers() {
        return this.tickers;
    }

    boolean hasTicker(BlockPos pos) {
        for (FlorinolSoilConversionManager.PendingConversion ticker : this.tickers) {
            if (ticker.pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FlorinolSoilConversionManager.PendingConversion ticker : this.tickers) {
            list.add(ticker.save());
        }
        tag.put(TICKERS_TAG, list);
        return tag;
    }

    private static FlorinolSoilConversionData load(CompoundTag tag, HolderLookup.Provider registries) {
        FlorinolSoilConversionData data = new FlorinolSoilConversionData();
        ListTag list = tag.getList(TICKERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            FlorinolSoilConversionManager.PendingConversion ticker =
                    FlorinolSoilConversionManager.PendingConversion.load(list.getCompound(i));
            if (ticker != null) {
                data.tickers.add(ticker);
            }
        }
        return data;
    }
}