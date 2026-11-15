package com.minechai.create_sandustry.content.gravity;

import java.util.List;
import java.util.Locale;
import com.minechai.create_sandustry.CreateSandustry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class GravityTooltipEvents {
    private GravityTooltipEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        adapt(event.getContext().level(), event.getItemStack(), event.getToolTip());
    }

    public static String percentage(double probability) {
        return String.format(Locale.ROOT, "%.2f", probability * 100);
    }

    public static void adapt(Level level, ItemStack stack, List<Component> lines) {
        if (level == null || !GravityMechanismAssembly.isAwaitingActivation(level, stack)) return;
        // This validated two-step recipe has exactly one remaining step: native 'next'.
        // There are no native trailing steps to remove. Never trim other mods' lines.
        for (int i = 0; i < lines.size(); i++) {
            if (!(lines.get(i).getContents() instanceof TranslatableContents next)
                    || !next.getKey().equals("create.recipe.assembly.next")
                    || next.getArgs().length != 1 || !(next.getArgs()[0] instanceof Component description)
                    || !(description.getContents() instanceof TranslatableContents step)
                    || !step.getKey().equals("recipe.createsandustry.assembly.bounce_activation")) continue;
            if (lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents t
                    && t.getKey().equals("tooltip.createsandustry.gravity.success"))) return;
            lines.add(i + 1, Component.translatable("tooltip.createsandustry.gravity.success",
                    percentage(GravityActivationLogic.successChance(1))).withStyle(ChatFormatting.GREEN));
            lines.add(i + 2, Component.translatable("tooltip.createsandustry.gravity.damage",
                    percentage(GravityActivationLogic.damageChance(GravityMechanismData.interruptions(stack))))
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
    }
}
