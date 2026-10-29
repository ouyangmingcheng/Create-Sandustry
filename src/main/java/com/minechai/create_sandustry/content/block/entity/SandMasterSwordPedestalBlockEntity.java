package com.minechai.create_sandustry.content.block.entity;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.content.block.SandMasterSwordPedestalBlock;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 沙噬剑座的数据层。
 *
 * <p>剑座状态与方块朝向分开保存：雷击把沙金剑转化为无主沙噬剑时只修改
 * {@link PedestalSwordState} 和展示物品，不替换方块，因此原插剑朝向不会被重置。</p>
 */
public class SandMasterSwordPedestalBlockEntity extends BlockEntity {
    private static final String SWORD_TAG = "Sword";
    private static final String SWORD_FACING_TAG = "SwordFacing";
    private static final String SWORD_STATE_TAG = "SwordState";

    private ItemStack storedSword = ItemStack.EMPTY;
    private Direction swordFacing = Direction.NORTH;
    private PedestalSwordState swordState = PedestalSwordState.EMPTY;

    public SandMasterSwordPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAND_MASTER_SWORD_PEDESTAL.get(), pos, state);
    }

    /** 当前展示的剑；渲染器只读取，不应修改返回的栈。 */
    public ItemStack getSword() {
        return this.storedSword;
    }

    public boolean hasSword() {
        return !this.storedSword.isEmpty();
    }

    public PedestalSwordState getSwordState() {
        return this.swordState;
    }

    public boolean isHoldingSandSword() {
        return this.swordState == PedestalSwordState.SAND_SWORD;
    }

    public boolean isUnowned() {
        return this.swordState == PedestalSwordState.UNOWNED;
    }

    /** 插剑时记录的水平朝向，渲染器用它决定剑面朝向。 */
    public Direction getSwordFacing() {
        return this.swordFacing;
    }

    public static boolean isAcceptedSword(ItemStack stack) {
        return stack.is(ModItems.SAND_SWORD.get()) || stack.is(ModItems.SAND_MASTER_SWORD.get());
    }

    /** 放入沙金剑或普通沙噬剑，并保留插剑时记录的朝向。 */
    public void setSword(ItemStack stack, Direction facing) {
        if (!isAcceptedSword(stack)) {
            return;
        }
        this.storedSword = stack.copyWithCount(1);
        this.swordState = stateFor(stack);
        if (facing.getAxis().isHorizontal()) {
            this.swordFacing = facing;
        }
        this.markUpdated();
    }

    /**
     * 雷击转化：沙金剑变为无主沙噬剑。
     *
     * <p>刻意不调用 {@link #setSword(ItemStack, Direction)}，避免重新推导或覆盖剑面朝向。</p>
     */
    public boolean transformToUnowned() {
        if (this.swordState != PedestalSwordState.SAND_SWORD
                || !this.storedSword.is(ModItems.SAND_SWORD.get())) {
            return false;
        }
        // 保留沙金剑的附魔、自定义名称、耐久及其他数据组件。
        this.storedSword = this.storedSword.transmuteCopy(ModItems.SAND_MASTER_SWORD.get(), 1);
        this.swordState = PedestalSwordState.UNOWNED;
        this.markUpdated();
        return true;
    }

    /** 直接取回普通剑；无主状态必须走拔剑流程。 */
    public ItemStack removeSword() {
        if (this.storedSword.isEmpty() || this.swordState == PedestalSwordState.UNOWNED) {
            return ItemStack.EMPTY;
        }
        return this.takeSword();
    }

    /** 拔剑成功：仅允许从无主状态取走剑。 */
    public ItemStack completePull() {
        if (this.swordState != PedestalSwordState.UNOWNED) {
            return ItemStack.EMPTY;
        }
        return this.takeSword();
    }

    /** 方块被移除时使用：清空数据但不向即将消失的方块发送更新包。 */
    public ItemStack clearSword() {
        if (this.storedSword.isEmpty()) {
            this.swordState = PedestalSwordState.EMPTY;
            this.setChanged();
            return ItemStack.EMPTY;
        }
        ItemStack result = this.storedSword.copy();
        this.storedSword = ItemStack.EMPTY;
        this.swordState = PedestalSwordState.EMPTY;
        this.setChanged();
        return result;
    }

    private ItemStack takeSword() {
        ItemStack result = this.storedSword.copy();
        this.storedSword = ItemStack.EMPTY;
        this.swordState = PedestalSwordState.EMPTY;
        this.markUpdated();
        return result;
    }

    private static PedestalSwordState stateFor(ItemStack stack) {
        if (stack.is(ModItems.SAND_SWORD.get())) {
            return PedestalSwordState.SAND_SWORD;
        }
        if (stack.is(ModItems.SAND_MASTER_SWORD.get())) {
            return PedestalSwordState.MASTER_SWORD;
        }
        return PedestalSwordState.EMPTY;
    }

    private static boolean isStateCompatible(PedestalSwordState state, ItemStack stack) {
        return switch (state) {
            case EMPTY -> stack.isEmpty();
            case SAND_SWORD -> stack.is(ModItems.SAND_SWORD.get());
            case MASTER_SWORD, UNOWNED -> stack.is(ModItems.SAND_MASTER_SWORD.get());
        };
    }

    private void syncSandSwordBlockState() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        BlockState state = this.getBlockState();
        if (!state.hasProperty(SandMasterSwordPedestalBlock.HAS_SAND_SWORD)) {
            return;
        }
        boolean present = this.swordState == PedestalSwordState.SAND_SWORD;
        if (state.getValue(SandMasterSwordPedestalBlock.HAS_SAND_SWORD) != present) {
            this.level.setBlock(this.worldPosition,
                    state.setValue(SandMasterSwordPedestalBlock.HAS_SAND_SWORD, present), Block.UPDATE_ALL);
        }
    }
    private void markUpdated() {
        this.setChanged();
        this.syncSandSwordBlockState();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always write the key: a state-change packet with an entirely empty tag is ignored by
        // BlockEntity#onDataPacket, leaving the previous client-side sword visible after removal.
        tag.put(SWORD_TAG,
                this.storedSword.isEmpty() ? new CompoundTag() : this.storedSword.save(registries));
        tag.putString(SWORD_FACING_TAG, this.swordFacing.getName());
        tag.putString(SWORD_STATE_TAG, this.swordState.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound(SWORD_TAG));
        this.storedSword = isAcceptedSword(loaded) ? loaded.copyWithCount(1) : ItemStack.EMPTY;

        Direction loadedFacing = Direction.byName(tag.getString(SWORD_FACING_TAG));
        if (loadedFacing != null && loadedFacing.getAxis().isHorizontal()) {
            this.swordFacing = loadedFacing;
        } else if (this.getBlockState().hasProperty(SandMasterSwordPedestalBlock.FACING)) {
            // Compatibility with pedestals created before sword facing was stored separately.
            this.swordFacing = this.getBlockState().getValue(SandMasterSwordPedestalBlock.FACING);
        }

        PedestalSwordState loadedState = null;
        String stateName = tag.getString(SWORD_STATE_TAG);
        if (!stateName.isEmpty()) {
            try {
                loadedState = PedestalSwordState.valueOf(stateName);
            } catch (IllegalArgumentException ignored) {
                loadedState = null;
            }
        }
        if (loadedState == null || !isStateCompatible(loadedState, this.storedSword)) {
            // Old saves have no state tag: ordinary sand master swords remain directly removable.
            this.swordState = stateFor(this.storedSword);
        } else {
            this.swordState = loadedState;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
