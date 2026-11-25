package com.minechai.create_sandustry.content.experience;

import java.util.HashSet;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = "createsandustry")
public final class GoldenExperienceConversion {
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getExplosion().interactsWithBlocks()) return;
        if (!(event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper) || !creeper.isPowered()) return;
        // One roll per position per explosion; keep both outcomes out of block destruction.
        var visited = new HashSet<BlockPos>();
        event.getAffectedBlocks().removeIf(pos -> {
            if (visited.contains(pos)) return true;
            if (!level.getBlockState(pos).is(AllBlocks.EXPERIENCE_BLOCK.get())) return false;
            if (visited.add(pos.immutable()) && level.random.nextBoolean()) {
                level.setBlockAndUpdate(pos, ModBlocks.GOLDEN_EXPERIENCE_BLOCK.get().defaultBlockState());
                level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + .5, pos.getY() + .5,
                        pos.getZ() + .5, 20, .4, .4, .4, .1);
            }
            return true;
        });
    }
}
