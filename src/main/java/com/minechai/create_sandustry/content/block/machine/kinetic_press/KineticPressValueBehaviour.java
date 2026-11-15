package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Create-native numeric settings for the persistent working height. */
public class KineticPressValueBehaviour extends ScrollValueBehaviour {
    public KineticPressValueBehaviour(Component label, SmartBlockEntity blockEntity, ValueBoxTransform slot) {
        super(label, blockEntity, slot);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        int visualMax = KineticPressStructure.MAX_WORKING_HEIGHT - KineticPressStructure.MIN_WORKING_HEIGHT;
        List<Component> rows = List.of(
                Component.translatable("gui.createsandustry.kinetic_press.working_height"));
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(settings ->
                Component.translatable("gui.createsandustry.kinetic_press.height_value",
                        settings.value() + KineticPressStructure.MIN_WORKING_HEIGHT));
        return new ValueSettingsBoard(label, visualMax, 5, rows, formatter);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
        int actualHeight = valueSetting.value() + KineticPressStructure.MIN_WORKING_HEIGHT;
        if (actualHeight == value) {
            return;
        }
        if (blockEntity instanceof KineticPressBlockEntity press
                && press.trySetWorkingHeight(player, actualHeight)) {
            playFeedbackSound(this);
        }
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value - KineticPressStructure.MIN_WORKING_HEIGHT);
    }

    @Override
    public boolean bypassesInput(ItemStack mainhandItem) {
        return !mainhandItem.isEmpty() || super.bypassesInput(mainhandItem);
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Persisted by KineticPressBlockEntity under the canonical workingHeight key.
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // Restored by KineticPressBlockEntity after its own NBT read.
    }

    @Override
    public String getClipboardKey() {
        return "KineticPressWorkingHeight";
    }
}