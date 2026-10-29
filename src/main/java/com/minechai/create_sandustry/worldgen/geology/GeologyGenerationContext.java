package com.minechai.create_sandustry.worldgen.geology;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 单条规则的区块内生成上下文。
 *
 * <p>所有坐标都被限制在触发 Feature 的当前区块内，读取和写入均不会请求邻区块。</p>
 */
public final class GeologyGenerationContext {
    private final WorldGenLevel level;
    private final RandomSource random;
    private final int chunkMinX;
    private final int chunkMaxX;
    private final int chunkMinZ;
    private final int chunkMaxZ;
    private final int minY;
    private final int maxY;
    private int remainingChecks;

    public GeologyGenerationContext(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, int maxChecks) {
        this.level = level;
        this.random = random;
        this.chunkMinX = chunkX << 4;
        this.chunkMinZ = chunkZ << 4;
        this.chunkMaxX = this.chunkMinX + 15;
        this.chunkMaxZ = this.chunkMinZ + 15;
        this.minY = level.getMinBuildHeight();
        this.maxY = level.getMaxBuildHeight() - 1;
        this.remainingChecks = maxChecks;
    }

    public WorldGenLevel level() {
        return level;
    }

    public RandomSource random() {
        return random;
    }

    public int chunkMinX() {
        return chunkMinX;
    }

    public int chunkMaxX() {
        return chunkMaxX;
    }

    public int chunkMinZ() {
        return chunkMinZ;
    }

    public int chunkMaxZ() {
        return chunkMaxZ;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public boolean consumeCheck() {
        if (remainingChecks <= 0) {
            return false;
        }
        remainingChecks--;
        return true;
    }

    public boolean contains(int x, int y, int z) {
        return x >= chunkMinX && x <= chunkMaxX
                && z >= chunkMinZ && z <= chunkMaxZ
                && y >= minY && y <= maxY;
    }

    /**
     * 只读检查目标区块是否已在当前世界生成区域缓存中，不触发区块加载。
     */
    public boolean canRead(BlockPos pos) {
        return level.hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public int clampY(int y) {
        return Mth.clamp(y, minY, maxY);
    }

    public int surfaceY(int x, int z) {
        return clampY(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1);
    }

    /**
     * 替换前重新读取并校验当前方块，防止并行生成或其他 Feature 改变目标方块后误写。
     */
    public boolean replaceIfAllowed(BlockPos pos, Set<Block> allowedTargets, Block replacement) {
        if (!contains(pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        if (!allowedTargets.contains(current.getBlock()) || current.is(replacement)) {
            return false;
        }
        if (!level.ensureCanWrite(pos)) {
            return false;
        }

        level.setBlock(pos, replacement.defaultBlockState(), 2);
        return true;
    }

    /**
     * 放置带考古战利品表的刷子方块，并在 proto-chunk 的待加载方块实体 NBT 中写入战利品信息。
     */
    public boolean replaceIfAllowedWithLoot(
            BlockPos pos,
            Set<Block> allowedTargets,
            Block replacement,
            ResourceKey<LootTable> lootTable) {
        if (!replaceIfAllowed(pos, allowedTargets, replacement)) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.BRUSHABLE_BLOCK);
        if (entityId == null) {
            return true;
        }

        CompoundTag tag = new CompoundTag();
        tag.putString("id", entityId.toString());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("LootTable", lootTable.location().toString());
        tag.putLong("LootTableSeed", random.nextLong());
        level.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())).setBlockEntityNbt(tag);
        return true;
    }
}
