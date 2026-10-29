package com.minechai.create_sandustry.content.conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.minechai.create_sandustry.content.block.FlorinolSoilBlock;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.entity.FlorinolBatteryBlockEntity;
import com.minechai.create_sandustry.content.compat.sable.SableCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

public final class FlorinolSoilConversionManager {
    private static final Map<ServerLevel, List<PendingConversion>> QUEUED = new HashMap<>();

    public static void addTicker(ServerLevel level, BlockPos pos, boolean requiresHeat, boolean dormant) {
        BlockPos immutablePos = pos.immutable();
        List<PendingConversion> queued = QUEUED.computeIfAbsent(level, ignored -> new ArrayList<>());
        if (isQueued(queued, immutablePos) || FlorinolSoilConversionData.get(level).hasTicker(immutablePos)) {
            return;
        }
        queued.add(new PendingConversion(immutablePos, nextAge(level), 0, requiresHeat, dormant));
    }

    public static void tick(ServerLevel level) {
        FlorinolSoilConversionData data = FlorinolSoilConversionData.get(level);
        List<PendingConversion> tickers = data.tickers();
        boolean changed = tickers.removeIf(ticker -> tickTicker(level, ticker));

        List<PendingConversion> queued = QUEUED.remove(level);
        if (queued != null && !queued.isEmpty()) {
            Set<BlockPos> activePositions = new HashSet<>();
            for (PendingConversion ticker : tickers) {
                activePositions.add(ticker.pos());
            }

            for (PendingConversion ticker : queued) {
                if (activePositions.add(ticker.pos())) {
                    tickers.add(ticker);
                    initialize(level, ticker);
                    changed = true;
                }
            }
        }

        if (changed || !tickers.isEmpty()) {
            data.setDirty();
        }
    }

    public static void clearQueues(MinecraftServer server) {
        QUEUED.keySet().removeIf(level -> level.getServer() == server);
    }

    private static boolean tickTicker(ServerLevel level, PendingConversion ticker) {
        if (ticker.age() > 0) {
            if (ticker.requiresHeat() && !FlorinolSoilBlock.hasAdjacentHeatSource(level, ticker.pos())) {
                return true;
            }
            ticker.setAge(ticker.age() - 1);
            return false;
        }

        BlockState state = level.getBlockState(ticker.pos());
        if (!state.is(ModBlocks.FLORINOL_SOIL.get())) {
            return true;
        }

        if (shouldConvert(level, ticker)) {
            convert(level, ticker.pos());
            return true;
        }

        spawnParticles(level, ticker.pos(), ParticleTypes.SMOKE, 15);
        ticker.setAge(nextAge(level));
        ticker.setAttempts(ticker.attempts() + 1);
        return false;
    }

    private static boolean shouldConvert(ServerLevel level, PendingConversion ticker) {
        float maxAttempts = ticker.dormant() ? 10.0F : 5.0F;
        return level.random.nextFloat() < ticker.attempts() / maxAttempts;
    }

    private static void convert(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, ModBlocks.FLORINOL.get().defaultBlockState());
        // 土壤重新变成液体后，让所在弗洛林电池立刻刷新 FE / 纹理状态。
        FlorinolBatteryBlockEntity.notifyInteriorChanged(level, pos);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
        spawnParticles(level, pos, ParticleTypes.FLAME, 30);
        spawnParticles(level, pos, ParticleTypes.SMOKE, 15);

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && level.getBlockState(neighbor).is(ModBlocks.FLORINOL_SOIL.get())) {
                addTicker(level, neighbor, false, false);
            }
        }
    }

    private static void initialize(ServerLevel level, PendingConversion ticker) {
        level.playSound(null, ticker.pos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.5F);
        if (!ticker.dormant()) {
            spawnParticles(level, ticker.pos(), ParticleTypes.FLAME, 20);
        }
        spawnParticles(level, ticker.pos(), ParticleTypes.SMOKE, 15);
    }

    private static void spawnParticles(ServerLevel level, BlockPos pos, ParticleOptions particle, int count) {
        // 物理化后的内部空间会被上方的弗洛林土壤挡住，上升的烟雾只会穿模显示异常。
        // 保留火焰粒子，关闭烟雾（燃烧灰烬）粒子。
        if (particle == ParticleTypes.SMOKE && isPhysicalized(level, pos)) {
            return;
        }
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                count, 0.3D, 0.3D, 0.3D, 0.0D);
    }

    private static boolean isPhysicalized(ServerLevel level, BlockPos pos) {
        return ModList.get().isLoaded("sable") && SableCompat.isInSubLevel(level, pos);
    }

    private static boolean isQueued(List<PendingConversion> queued, BlockPos pos) {
        for (PendingConversion ticker : queued) {
            if (ticker.pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static int nextAge(ServerLevel level) {
        return level.random.nextInt(10, 40);
    }

    static final class PendingConversion {
        private final BlockPos pos;
        private int age;
        private int attempts;
        private final boolean requiresHeat;
        private final boolean dormant;

        private PendingConversion(BlockPos pos, int age, int attempts, boolean requiresHeat, boolean dormant) {
            this.pos = pos;
            this.age = age;
            this.attempts = attempts;
            this.requiresHeat = requiresHeat;
            this.dormant = dormant;
        }

        BlockPos pos() {
            return this.pos;
        }

        int age() {
            return this.age;
        }

        void setAge(int age) {
            this.age = age;
        }

        int attempts() {
            return this.attempts;
        }

        void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        boolean requiresHeat() {
            return this.requiresHeat;
        }

        boolean dormant() {
            return this.dormant;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", this.pos.asLong());
            tag.putInt("Age", this.age);
            tag.putInt("Attempts", this.attempts);
            tag.putBoolean("RequiresHeat", this.requiresHeat);
            tag.putBoolean("Dormant", this.dormant);
            return tag;
        }

        static PendingConversion load(CompoundTag tag) {
            if (!tag.contains("Pos") || !tag.contains("Age") || !tag.contains("Attempts")) {
                return null;
            }
            return new PendingConversion(
                    BlockPos.of(tag.getLong("Pos")),
                    Math.max(0, tag.getInt("Age")),
                    Math.max(0, tag.getInt("Attempts")),
                    tag.getBoolean("RequiresHeat"),
                    tag.getBoolean("Dormant"));
        }
    }

    private FlorinolSoilConversionManager() {
    }
}