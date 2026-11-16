package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.foundation.event.ArchaeologyCompletedEvent;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.content.archaeology.BrushableFallProtection;
import com.minechai.create_sandustry.content.archaeology.BrushProgressAccess;
import com.minechai.create_sandustry.content.archaeology.UniversalArchaeologyLootPool;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 可疑沙子 / 可疑沙砾 / 猫砂的考古相关补丁。
 *
 * <p>1. 全局考古战利品池：在配置开启时改为从全局池抽取可疑方块的战利品表，不改写原版刷掘流程
 * （解析→roll→设置 item→清空 lootTable→setChanged、{@code GENERATE_LOOT} 进度触发、
 * 刷掘进度与声音都保持不变）。手动放置的可疑方块没有 LootTable 时先注入一个合成标记键，
 * 随后同样由下方 {@code @Redirect} 接管为全局池。</p>
 *
 * <p>2. 考古完成计数：{@code brush} 返回 {@code true} 表示一次考古真正完成（原版玩家用刷子
 * 连续刷掘直到完成）。在这里发出一次 {@link ArchaeologyCompletedEvent}，
 * 为玩家累加“考古次数”自定义统计。只在服务端、且仅对真实玩家触发，
 * 因此不会重复计数，也不会把假玩家（机械手）混入——机械手路径由 {@code DeployerBrushCompat}
 * 单独触发。</p>
 *
 * <p>3. 防重力摔碎：作为 {@link BrushProgressAccess}，在下落方块落地、原版把携带的
 * TileEntityData 合并回新的方块实体之后，恢复刷掘次数（见 {@link BrushableFallProtection}）。</p>
 */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin implements BrushProgressAccess {
    private static final ResourceKey<LootTable> POOL_MARKER =
            ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology/pool_marker"));

    @Accessor("item")
    abstract ItemStack createsandustry$getItem();

    @Accessor("lootTable")
    abstract ResourceKey<LootTable> createsandustry$getLootTable();

    @Accessor("brushCount")
    public abstract int createsandustry$getBrushCount();

    @Accessor("brushCount")
    public abstract void createsandustry$setBrushCount(int count);

    @Accessor("brushCountResetsAtTick")
    public abstract long createsandustry$getBrushCountResetsAtTick();

    @Accessor("brushCountResetsAtTick")
    public abstract void createsandustry$setBrushCountResetsAtTick(long tick);

    @Unique
    private boolean createsandustry$isSuspiciousBlock() {
        Block block = ((BrushableBlockEntity) (Object) this).getBlockState().getBlock();
        return block == Blocks.SUSPICIOUS_SAND || block == Blocks.SUSPICIOUS_GRAVEL;
    }

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void createsandustry$ensurePoolTable(Player player, CallbackInfo ci) {
        if (!ModConfigs.UNIVERSAL_ARCHAEOLOGY_LOOT.get() || !this.createsandustry$isSuspiciousBlock()) {
            return;
        }
        BrushableBlockEntity self = (BrushableBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        if (this.createsandustry$getLootTable() == null && this.createsandustry$getItem().isEmpty()) {
            self.setLootTable(POOL_MARKER, 0L);
        }
    }

    @Redirect(
            method = "unpackLootTable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"))
    private LootTable createsandustry$usePool(ReloadableServerRegistries.Holder holder, ResourceKey<LootTable> key) {
        if (ModConfigs.UNIVERSAL_ARCHAEOLOGY_LOOT.get() && this.createsandustry$isSuspiciousBlock()) {
            ServerLevel serverLevel = (ServerLevel) ((BrushableBlockEntity) (Object) this).getLevel();
            LootTable table = UniversalArchaeologyLootPool.pickRandom(serverLevel);
            return table != null ? table : LootTable.EMPTY;
        }
        return holder.getLootTable(key);
    }

    /**
     * 一次考古真正完成时（{@code brush} 返回 true）发出考古完成事件。
     * 只对服务端真实玩家触发：客户端 {@code brush} 恒返回 false；假玩家（机械手）由
     * {@code DeployerBrushCompat} 单独处理，避免重复计数。
     */
    @Inject(method = "brush", at = @At("RETURN"))
    private void createsandustry$onArchaeologyCompleted(long startTick, Player player, Direction hitDirection,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()
                && player instanceof ServerPlayer serverPlayer
                && !(player instanceof FakePlayer)) {
            NeoForge.EVENT_BUS.post(new ArchaeologyCompletedEvent(serverPlayer));
        }
    }

    /**
     * 下落方块落地后，原版会把携带的 TileEntityData 合并进新的方块实体并调用
     * {@code loadWithComponents} → {@code loadAdditional}。在这里恢复随数据一起搬运过来的刷掘次数，
     * 使玩家可以接着原来的进度继续刷掘。
     */
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void createsandustry$restoreBrushCount(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains(BrushableFallProtection.BRUSH_COUNT_TAG)) {
            this.createsandustry$setBrushCount(tag.getInt(BrushableFallProtection.BRUSH_COUNT_TAG));
        }
        if (tag.contains(BrushableFallProtection.BRUSH_RESET_TICK_TAG)) {
            this.createsandustry$setBrushCountResetsAtTick(tag.getLong(BrushableFallProtection.BRUSH_RESET_TICK_TAG));
        }
    }
}
