package com.minechai.create_sandustry.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

/** Exposes only real storage through Jade's standard energy bar and item display. */
public final class KineticPressJadeProvider {
    private KineticPressJadeProvider() {}

    public enum Energy implements IServerExtensionProvider<CompoundTag>,
            IClientExtensionProvider<CompoundTag, EnergyView> {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "kinetic_press_energy");

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof KineticPressBlockEntity press)) return null;
            // Inspection is independent of the four faces that accept FE.
            ViewGroup<CompoundTag> group = new ViewGroup<>(List.of(
                    EnergyView.of(press.getEnergyStored(), press.getMaxEnergyStored())));
            group.getExtraData().putString("Unit", "FE");
            return List.of(group);
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor,
                                                                List<ViewGroup<CompoundTag>> groups) {
            return ClientViewGroup.map(groups, tag -> EnergyView.read(tag, "FE"), null);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return -1000;
        }
    }

    public enum Items implements IServerExtensionProvider<ItemStack>,
            IClientExtensionProvider<ItemStack, ItemView> {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "kinetic_press_items");

        @Override
        public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            if (!(accessor.getTarget() instanceof KineticPressBlockEntity press)) return null;
            List<ItemStack> items = new ArrayList<>();
            collect(items, press.getInputHandler());
            collect(items, press.getOutputHandler());
            // Empty results override the sided default too. Active task inputs
            // and rolled results are not inventory and must not appear here.
            return items.isEmpty() ? List.of() : List.of(new ViewGroup<>(items));
        }

        private static void collect(List<ItemStack> items, IItemHandler inventory) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                ItemStack matching = items.stream()
                        .filter(item -> ItemStack.isSameItemSameComponents(item, stack))
                        .findFirst().orElse(null);
                if (matching == null) items.add(stack.copy());
                else matching.grow(stack.getCount());
            }
        }

        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor,
                                                              List<ViewGroup<ItemStack>> groups) {
            return ClientViewGroup.map(groups, ItemView::new, null);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return -1000;
        }
    }
}
