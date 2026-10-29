package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** 沙金剑的运行时事件。 */
public final class SandSwordEvents {
    private SandSwordEvents() {
    }

    /** 破损沙金剑不能作为武器使用。 */
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().is(ModItems.SAND_SWORD_BROKEN.get())) {
            event.setCanceled(true);
        }
    }
}
