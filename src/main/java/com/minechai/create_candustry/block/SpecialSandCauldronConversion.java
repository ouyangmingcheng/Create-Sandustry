package com.minechai.create_candustry.block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.item.ModItems;
import com.minechai.create_candustry.recipe.CauldronConversionRecipe;
import com.minechai.create_candustry.recipe.ModRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class SpecialSandCauldronConversion {
    private static final int CHECK_INTERVAL = 5;
    private static final int CAULDRON_CAPACITY_MB = 1000;
    private static final ResourceLocation WATER_FLUID = ResourceLocation.withDefaultNamespace("water");
    private static final String WATER_DATA_NAME = CreateSandustry.MODID + "_cauldron_water";

    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || item.level().isClientSide
                || item.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        ItemStack stack = item.getItem();
        if (stack.isEmpty() || !(item.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<RecipeHolder<CauldronConversionRecipe>> recipeHolder = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CAULDRON_CONVERSION.get(), new SingleRecipeInput(stack), level);
        if (recipeHolder.isEmpty()) {
            return;
        }

        BlockPos cauldronPos = findWaterCauldron(level, item);
        if (cauldronPos == null) {
            return;
        }

        processConversion(level, item, cauldronPos, recipeHolder.get().value());
    }

    private static void processConversion(ServerLevel level, ItemEntity item, BlockPos cauldronPos,
                                          CauldronConversionRecipe recipe) {
        ItemStack stack = item.getItem();
        if (!recipe.matches(new SingleRecipeInput(stack), level)) {
            return;
        }

        if (!isWaterFluid(recipe.fluid())) {
            CreateSandustry.LOGGER.warn("Skipping cauldron conversion recipe with unsupported fluid: {}",
                    recipe.fluid());
            return;
        }

        BlockState cauldronState = level.getBlockState(cauldronPos);
        if (!cauldronState.is(Blocks.WATER_CAULDRON)) {
            return;
        }

        int waterCostMb = recipe.waterMb();
        if (waterCostMb <= 0) {
            return;
        }

        WaterData waterData = waterData(level);
        int remainingWaterMb = resolveWaterMb(level, cauldronPos, cauldronState, waterData);
        if (remainingWaterMb < waterCostMb) {
            return;
        }

        ItemStack result = ItemStack.EMPTY;
        int conversions = 0;
        while (!stack.isEmpty() && remainingWaterMb >= waterCostMb) {
            cauldronState = level.getBlockState(cauldronPos);
            if (!cauldronState.is(Blocks.WATER_CAULDRON)) {
                break;
            }

            remainingWaterMb = resolveWaterMb(level, cauldronPos, cauldronState, waterData);
            if (remainingWaterMb < waterCostMb) {
                break;
            }

            ItemStack converted = recipe.result().copy();
            if (converted.isEmpty()) {
                break;
            }

            stack.shrink(1);
            remainingWaterMb -= waterCostMb;
            waterData.setWater(cauldronPos, remainingWaterMb);
            updateCauldronState(level, cauldronPos, remainingWaterMb);
            spawnConversionParticles(level, cauldronPos);
            SpecialSandEffects.triggerCauldronEffect(level, cauldronPos, recipe.effectId());
            result = merge(result, converted);
            conversions++;
        }

        if (conversions <= 0) {
            return;
        }

        Vec3 motion = item.getDeltaMovement().scale(0.35D);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        spawnResult(level, cauldronPos, result, motion);
    }

    private static boolean isWaterFluid(String fluidId) {
        ResourceLocation id = ResourceLocation.tryParse(fluidId);
        return id != null && id.equals(WATER_FLUID);
    }

    private static int resolveWaterMb(ServerLevel level, BlockPos pos, BlockState state, WaterData waterData) {
        int levelValue = state.getValue(LayeredCauldronBlock.LEVEL);
        int vanillaEquivalent = levelValue * CAULDRON_CAPACITY_MB / 3;
        int stored = waterData.getWater(pos, -1);

        if (stored < 0) {
            waterData.setWater(pos, vanillaEquivalent);
            return vanillaEquivalent;
        }

        if (levelValue != waterToLevel(stored)) {
            waterData.setWater(pos, vanillaEquivalent);
            return vanillaEquivalent;
        }

        return stored;
    }

    private static void updateCauldronState(ServerLevel level, BlockPos pos, int remainingWaterMb) {
        BlockState state;
        if (remainingWaterMb <= 0) {
            state = Blocks.CAULDRON.defaultBlockState();
        } else {
            state = Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, waterToLevel(remainingWaterMb));
        }
        level.setBlockAndUpdate(pos, state);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
    }

    private static int waterToLevel(int waterMb) {
        if (waterMb <= 0) {
            return 0;
        }
        return Math.min(3, (waterMb * 3 + CAULDRON_CAPACITY_MB - 1) / CAULDRON_CAPACITY_MB);
    }

    private static void spawnConversionParticles(ServerLevel level, BlockPos pos) {
        int count = 8 + level.random.nextInt(8);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D,
                pos.getY() + 0.8D,
                pos.getZ() + 0.5D,
                count,
                0.3D,
                0.35D,
                0.3D,
                0.02D);
    }

    private static BlockPos findWaterCauldron(ServerLevel level, ItemEntity item) {
        BlockPos center = item.blockPosition();
        BlockPos min = center.offset(-1, -1, -1);
        BlockPos max = center.offset(1, 1, 1);
        AABB itemBox = item.getBoundingBox();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER_CAULDRON)
                    && state.getValue(LayeredCauldronBlock.LEVEL) > 0
                    && itemBox.intersects(new AABB(pos))) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static ItemStack merge(ItemStack result, ItemStack converted) {
        if (result.isEmpty()) {
            return converted.copy();
        }
        if (ItemStack.isSameItemSameComponents(result, converted)) {
            result.grow(converted.getCount());
        }
        return result;
    }

    private static void spawnResult(ServerLevel level, BlockPos cauldronPos, ItemStack result, Vec3 motion) {
        if (result.isEmpty()) {
            return;
        }
        ItemEntity resultEntity = new ItemEntity(
                level,
                cauldronPos.getX() + 0.5D,
                cauldronPos.getY() + 0.75D,
                cauldronPos.getZ() + 0.5D,
                result);
        resultEntity.setDeltaMovement(motion.x, Math.max(0.1D, motion.y), motion.z);
        resultEntity.setPickUpDelay(10);
        level.addFreshEntity(resultEntity);
    }

    private static WaterData waterData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WaterData::new, WaterData::load),
                WATER_DATA_NAME);
    }

    public static final class WaterData extends SavedData {
        private final Map<BlockPos, Integer> water = new HashMap<>();

        public int getWater(BlockPos pos, int fallback) {
            return this.water.getOrDefault(pos, fallback);
        }

        public void setWater(BlockPos pos, int amount) {
            BlockPos immutablePos = pos.immutable();
            if (amount <= 0) {
                this.water.remove(immutablePos);
            } else {
                this.water.put(immutablePos, amount);
            }
            this.setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            for (Map.Entry<BlockPos, Integer> entry : this.water.entrySet()) {
                tag.putInt(Long.toString(entry.getKey().asLong()), entry.getValue());
            }
            return tag;
        }

        private static WaterData load(CompoundTag tag, HolderLookup.Provider registries) {
            WaterData data = new WaterData();
            for (String key : tag.getAllKeys()) {
                try {
                    data.water.put(BlockPos.of(Long.parseLong(key)), tag.getInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
            return data;
        }
    }

    private SpecialSandCauldronConversion() {
    }
}