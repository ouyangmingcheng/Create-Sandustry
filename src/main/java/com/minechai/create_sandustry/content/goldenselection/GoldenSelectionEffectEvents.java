package com.minechai.create_sandustry.content.goldenselection;

import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.content.goldenselection.ItemStackGoldAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Gameplay side of the Golden Selection status effect: grants the effect when a
 * Golden Selection food is eaten, and makes sand drops drop bonus gold nuggets.
 */
public final class GoldenSelectionEffectEvents {
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (!item.has(DataComponents.FOOD)) {
            return;
        }
        if (!((ItemStackGoldAccess) (Object) item).hasGold()) {
            return;
        }
        applyGoldenSelection(player);
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            return;
        }
        if (!player.hasEffect(ModMobEffects.GOLDEN_SELECTION)) {
            return;
        }
        if (!isSand(event.getState())) {
            return;
        }

        int maxNuggets = ModConfigs.GOLDEN_SELECTION_SAND_MAX_NUGGETS.get();
        int count = player.getRandom().nextInt(maxNuggets + 1);
        if (count > 0) {
            Block.popResource(player.level(), event.getPos(), new ItemStack(Items.GOLD_NUGGET, count));
        }
    }

    private static boolean isSand(BlockState state) {
        if (state.is(BlockTags.SAND)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && ModConfigs.GOLDEN_SELECTION_EXTRA_SAND_BLOCKS.get().contains(id.toString());
    }

    private static void applyGoldenSelection(ServerPlayer player) {
        int duration = ModConfigs.GOLDEN_SELECTION_DURATION.get();
        if (player.hasEffect(ModMobEffects.GOLDEN_SELECTION)) {
            MobEffectInstance existing = player.getEffect(ModMobEffects.GOLDEN_SELECTION);
            if (existing != null && existing.getDuration() >= duration) {
                return;
            }
        }
        player.addEffect(new MobEffectInstance(ModMobEffects.GOLDEN_SELECTION, duration, 0, false, true, true));
    }

    private GoldenSelectionEffectEvents() {
    }
}
