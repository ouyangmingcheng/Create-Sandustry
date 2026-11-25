package com.minechai.create_sandustry.content.sandheart.client;

import java.util.IdentityHashMap;
import java.util.Map;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordEvents;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Create's Shift description format, with flavour text and usage behaviour. */
@EventBusSubscriber(modid = "createsandustry", value = Dist.CLIENT)
public final class HeartTooltipEvents {
    private static final Map<Item, ItemDescription> DESCRIPTIONS = new IdentityHashMap<>();
    private static String cachedLanguage = "";
    private static String cachedDropKey = "";

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == ModItems.SAND_MASTER_SWORD_BROKEN.get()) {
            // Per-stack countdown must not share the static item-description cache.
            ItemDescription description = new ItemDescription.Builder(Palette.STANDARD_CREATE)
                    .addSummary(I18n.get(item.getDescriptionId() + ".tooltip.summary",
                            remainingRepairSeconds(event.getItemStack(), event.getEntity())))
                    .build();
            event.getToolTip().addAll(1, description.getCurrentLines());
            return;
        }
        if (item != ModItems.SAND_HEART.get() && item != ModItems.GOLDEN_HEART.get()
                && item != ModItems.SAND_SWORD.get() && item != ModItems.SAND_MASTER_SWORD.get()
                && item != ModItems.SAND_SWORD_BROKEN.get() && item != ModItems.MYSTERIOUS_LUMP.get()) return;

        Minecraft minecraft = Minecraft.getInstance();
        String language = minecraft.getLanguageManager().getSelected();
        String dropKey = minecraft.options.keyDrop.getTranslatedKeyMessage().getString();
        // Rebuild formatted lines when the language or displayed drop binding changes.
        if (!language.equals(cachedLanguage) || !dropKey.equals(cachedDropKey)) {
            DESCRIPTIONS.clear();
            cachedLanguage = language;
            cachedDropKey = dropKey;
        }
        ItemDescription description = DESCRIPTIONS.computeIfAbsent(item, heart -> {
            String key = heart.getDescriptionId() + ".tooltip.";
            ItemDescription.Builder builder = new ItemDescription.Builder(Palette.STANDARD_CREATE)
                    .addSummary(I18n.get(key + "summary"));
            if (heart != ModItems.SAND_SWORD.get() && heart != ModItems.SAND_SWORD_BROKEN.get()
                    && heart != ModItems.MYSTERIOUS_LUMP.get()) {
                String behaviour = heart == ModItems.SAND_MASTER_SWORD.get()
                        ? I18n.get(key + "behaviour1", dropKey)
                        : I18n.get(key + "behaviour1");
                builder.addBehaviour(I18n.get(key + "condition1"), behaviour);
            }
            return builder.build();
        });
        event.getToolTip().addAll(1, description.getCurrentLines());
    }

    private static int remainingRepairSeconds(ItemStack sword, Player player) {
        int periods = repairPeriods(sword);
        if (player != null) {
            int queuedPeriods = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack candidate = player.getInventory().getItem(slot);
                if (candidate == sword) {
                    int elapsed = player.getData(ModAttachments.SAND_MASTER_SWORD_REPAIR_DISPLAY_TICKS);
                    return ((queuedPeriods + periods) * SandMasterSwordEvents.REPAIR_INTERVAL_TICKS
                            - elapsed + 19) / 20;
                }
                if (candidate.is(ModItems.SAND_MASTER_SWORD_BROKEN.get())) {
                    queuedPeriods += repairPeriods(candidate);
                }
            }
        }
        // Outside the player's inventory, show required repair time without queue progress.
        return periods * SandMasterSwordEvents.REPAIR_INTERVAL_TICKS / 20;
    }

    private static int repairPeriods(ItemStack sword) {
        return Math.max(1, (sword.getDamageValue() + SandMasterSwordEvents.DURABILITY_PER_REPAIR - 1)
                / SandMasterSwordEvents.DURABILITY_PER_REPAIR);
    }
}
