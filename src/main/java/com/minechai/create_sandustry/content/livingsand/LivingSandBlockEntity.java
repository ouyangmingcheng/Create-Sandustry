package com.minechai.create_sandustry.content.livingsand;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.livingsand.LivingSand;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 活沙方块的方块实体：负责“玩家靠近后逐渐复活为活沙实体”的服务端计时与粒子。
 */
public class LivingSandBlockEntity extends BlockEntity {
    /** 检测半径（以方块中心为基准的三维距离，单位：格）。 */
    private static final double REVIVE_RANGE = 6.0D;
    /** 复活过程总时长（游戏刻）：2 秒 = 40 tick。 */
    private static final int REVIVE_TICKS = 40;

    private int reviveProgress;

    public LivingSandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIVING_SAND.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LivingSandBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!state.is(ModBlocks.LIVING_SAND.get())) {
            return;
        }

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        Player player = serverLevel.getNearestPlayer(centerX, centerY, centerZ, REVIVE_RANGE, false);

        if (player == null || !player.isAlive()) {
            // 玩家离开范围：暂停计时、停止粒子，进度保留。
            return;
        }

        blockEntity.reviveProgress++;
        blockEntity.setChanged();
        blockEntity.spawnReviveParticles(serverLevel, pos);

        if (blockEntity.reviveProgress >= REVIVE_TICKS) {
            blockEntity.tryRevive(serverLevel, pos, player);
        }
    }

    private void spawnReviveParticles(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 2; i++) {
            double px = pos.getX() + 0.15D + level.random.nextDouble() * 0.7D;
            double py = pos.getY() + 0.2D + level.random.nextDouble() * 0.8D;
            double pz = pos.getZ() + 0.15D + level.random.nextDouble() * 0.7D;
            level.sendParticles(ParticleTypes.WHITE_ASH, px, py, pz, 1, 0.0D, 0.05D, 0.0D, 0.03D);
        }
    }

    private void tryRevive(ServerLevel level, BlockPos pos, Player player) {
        if (!level.getBlockState(pos).is(ModBlocks.LIVING_SAND.get())) {
            this.reviveProgress = 0;
            return;
        }

        LivingSand entity = ModEntityTypes.LIVING_SAND.get().spawn(level, pos, MobSpawnType.TRIGGERED);
        if (entity == null) {
            // 生成失败：保留方块，清空进度等待下次重新计时，避免活沙永久丢失。
            this.reviveProgress = 0;
            return;
        }

        entity.awaken(player);
        level.removeBlock(pos, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ReviveProgress", this.reviveProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.reviveProgress = tag.getInt("ReviveProgress");
    }
}
