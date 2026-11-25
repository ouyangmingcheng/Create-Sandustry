package com.minechai.create_sandustry.compat.createstuffadditions;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class CreateStuffAdditionsFlorinolGameTests {
    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID,
            timeoutTicks = 20)
    public static void brassArmorMixinTargetsLoad(GameTestHelper helper) {
        try {
            Class.forName("net.mcreator.createstuffadditions.procedures.BrassEncasedPropelerBodyTickEventProcedure");
            Class.forName("net.mcreator.createstuffadditions.procedures.BrassExoskeletonBodyTickEventProcedure");
            Class.forName("net.mcreator.createstuffadditions.procedures.NetheriteJetpackChestplateTickEventProcedure");
            Class.forName("net.mcreator.createstuffadditions.item.NetheriteJetpackItem$Chestplate");
            Item jetpack = BuiltInRegistries.ITEM.get(BrassArmorFlorinolCompat.BRASS_JETPACK_ID);
            Item exoskeleton = BuiltInRegistries.ITEM.get(BrassArmorFlorinolCompat.BRASS_EXOSKELETON_ID);
            helper.assertTrue(FlorinolToolEnergy.isBindingCapable(new ItemStack(jetpack)),
                    "Brass jetpack should be Florinol-bindable");
            helper.assertTrue(FlorinolToolEnergy.isBindingCapable(new ItemStack(exoskeleton)),
                    "Brass exoskeleton should be Florinol-bindable");
            Item netheriteJetpack = BuiltInRegistries.ITEM.get(BrassArmorFlorinolCompat.NETHERITE_JETPACK_ID);
            Item netheriteExoskeleton = BuiltInRegistries.ITEM.get(BrassArmorFlorinolCompat.NETHERITE_EXOSKELETON_ID);
            helper.assertTrue(FlorinolToolEnergy.isBindingCapable(new ItemStack(netheriteJetpack)),
                    "Netherite jetpack should be Florinol-bindable");
            helper.assertTrue(FlorinolToolEnergy.isBindingCapable(new ItemStack(netheriteExoskeleton)),
                    "Netherite exoskeleton should be Florinol-bindable");
            helper.succeed();
        } catch (Throwable throwable) {
            helper.fail("Create SA brass armor mixin targets failed to load: " + throwable);
        }
    }

    private CreateStuffAdditionsFlorinolGameTests() {
    }
}