package com.minechai.create_sandustry.content.block;

import com.minechai.create_sandustry.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.minechai.create_sandustry.config.ModConfigs;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class SpecialSandEffects {
    private static final Logger LOGGER = LogUtils.getLogger();
    public enum Trigger {
        PLAYER_BREAK,
        EXPLOSION,
        CHAIN_REACTION,
        FALLING_BLOCK_DISCARD,
        CAULDRON_CONVERSION,
        OTHER
    }

    @FunctionalInterface
    public interface Handler {
        void trigger(ServerLevel level, BlockPos pos, BlockState state, Trigger trigger,
                     @Nullable ChainContext context);
    }

    public static final class ChainContext {
        private final ServerLevel level;
        private final Set<BlockPos> triggeredPositions;
        private final int chainDepth;
        private final Trigger source;

        private ChainContext(ServerLevel level, Set<BlockPos> triggeredPositions, int chainDepth, Trigger source) {
            this.level = level;
            this.triggeredPositions = triggeredPositions;
            this.chainDepth = chainDepth;
            this.source = source;
        }

        public ServerLevel level() {
            return this.level;
        }

        public Set<BlockPos> triggeredPositions() {
            return this.triggeredPositions;
        }

        public int chainDepth() {
            return this.chainDepth;
        }

        public Trigger source() {
            return this.source;
        }
    }

    private record HandlerEntry(Supplier<Block> block, Handler handler) {
    }

    private static final List<HandlerEntry> HANDLERS = List.of(
            new HandlerEntry(ModBlocks.VOLATILE_SAND::get,
                    (level, pos, state, trigger, context) -> VolatileSandBlock.handleTrigger(level, pos, trigger)),
            new HandlerEntry(ModBlocks.THUNDER_SAND::get,
                    (level, pos, state, trigger, context) -> ThunderSandBlock.handleTrigger(level, pos, trigger)),
            new HandlerEntry(ModBlocks.CAT_LITTER::get,
                    (level, pos, state, trigger, context) -> CatLitterBlock.handleTrigger(level, pos)));

    private static final ThreadLocal<ChainContext> ACTIVE_CHAIN = new ThreadLocal<>();

    public static void triggerSpecialSandEffect(ServerLevel level, BlockPos pos, BlockState state,
                                                Trigger trigger, @Nullable ChainContext context) {
        Handler handler = findHandler(state.getBlock());
        if (handler != null) {
            handler.trigger(level, pos.immutable(), state, trigger, context);
        }
    }

    public static void triggerCauldronEffect(ServerLevel level, BlockPos pos, String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) {
            LOGGER.warn("Ignoring malformed cauldron conversion effect id: {}", effectId);
            return;
        }

        if (id.equals(ResourceLocation.parse(ThunderSandEffects.EFFECT_ID))) {
            ThunderSandEffects.triggerSingleStrike(level, pos);
            return;
        }

        LOGGER.warn("Ignoring unknown cauldron conversion effect id: {}", id);
    }
    public static boolean tryTriggerSpecialSandEffect(ServerLevel level, BlockPos pos, BlockState state,
                                                       Trigger trigger, float chance,
                                                       @Nullable ChainContext context) {
        if (chance <= 0.0F || (chance < 1.0F && level.random.nextFloat() >= chance)) {
            return false;
        }
        triggerSpecialSandEffect(level, pos, state, trigger, context);
        return true;
    }

    public static void triggerVolatileExplosion(ServerLevel level, BlockPos origin) {
        ChainContext parent = ACTIVE_CHAIN.get();
        Set<BlockPos> triggered = parent == null ? new HashSet<>() : parent.triggeredPositions();
        int depth = parent == null ? 0 : parent.chainDepth() + 1;
        if (depth > ModConfigs.VOLATILE_SAND_MAX_CHAIN_DEPTH.get()) {
            return;
        }

        BlockPos immutableOrigin = origin.immutable();
        triggered.add(immutableOrigin);
        ChainContext context = new ChainContext(level, triggered, depth, Trigger.CHAIN_REACTION);
        ACTIVE_CHAIN.set(context);
        try {
            VolatileSandBlock.performExplosion(level, immutableOrigin);
        } finally {
            if (parent == null) {
                ACTIVE_CHAIN.remove();
            } else {
                ACTIVE_CHAIN.set(parent);
            }
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ChainContext context = ACTIVE_CHAIN.get();
        if (context == null || context.level() != level) {
            return;
        }

        List<BlockPos> affected = new ArrayList<>(event.getAffectedBlocks());
        Comparator<BlockPos> comparator = Comparator.comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingInt((BlockPos pos) -> pos.getX())
                .thenComparingInt((BlockPos pos) -> pos.getZ());
        affected.sort(comparator);

        float chainChance = ModConfigs.VOLATILE_SAND_CHAIN_TRIGGER_CHANCE.get().floatValue();
        for (BlockPos pos : affected) {
            BlockPos immutablePos = pos.immutable();
            if (!context.triggeredPositions().add(immutablePos)) {
                continue;
            }

            BlockState state = level.getBlockState(immutablePos);
            tryTriggerSpecialSandEffect(level, immutablePos, state, Trigger.CHAIN_REACTION, chainChance, context);
        }
    }

    private static Handler findHandler(Block block) {
        for (HandlerEntry entry : HANDLERS) {
            if (entry.block().get() == block) {
                return entry.handler();
            }
        }
        return null;
    }

    private SpecialSandEffects() {
    }
}

