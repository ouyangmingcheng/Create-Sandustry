package com.minechai.create_sandustry.content.sand;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VolatileSandBlock extends SpecialSandBlock {
    public static final MapCodec<VolatileSandBlock> CODEC = simpleCodec(VolatileSandBlock::new);
    private static final ResourceKey<DamageType> VOLATILE_SAND_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "volatile_sand"));
    private static final float EXPLOSION_DAMAGE_RADIUS = 3.0F;
    private static final double EXPLOSION_BLOCK_RADIUS = 2.0D;
    private static final int SAND_DUST_COLOR = 0xDBD3A0;

    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
            return explosion.center().distanceToSqr(Vec3.atCenterOf(pos))
                    <= EXPLOSION_BLOCK_RADIUS * EXPLOSION_BLOCK_RADIUS;
        }
    };

    public VolatileSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return SAND_DUST_COLOR;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        triggerPlayerBreak(level, pos, state, tool);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public static void handleTrigger(ServerLevel level, BlockPos pos, SpecialSandEffects.Trigger trigger) {
        if (level.random.nextFloat() < ModConfigs.VOLATILE_SAND_EXPLOSION_CHANCE.get().floatValue()) {
            SpecialSandEffects.triggerVolatileExplosion(level, pos);
        }
    }

    public static void performExplosion(ServerLevel level, BlockPos pos) {
        Vec3 explosionCenter = Vec3.atCenterOf(pos);
        DamageSource damageSource = new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(VOLATILE_SAND_DAMAGE),
                explosionCenter);
        level.explode(null, damageSource, EXPLOSION_DAMAGE_CALCULATOR,
                explosionCenter, EXPLOSION_DAMAGE_RADIUS, false, Level.ExplosionInteraction.BLOCK);
    }
}
