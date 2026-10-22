package com.minechai.create_candustry.block.entity;

import com.minechai.create_candustry.block.SandMasterSwordPedestalBlock;
import com.minechai.create_candustry.item.ModItems;

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
 * 沙噬剑座的数据层：只保存完整形态的沙噬剑。
 *
 * <p>剑的物品栈保存在服务端方块实体中，并通过更新包同步给客户端渲染器。这里保留完整的
 * {@link ItemStack} 组件（附魔、自定义名称等），因此放剑、取剑和破坏回收都不会丢失物品数据。</p>
 */
public class SandMasterSwordPedestalBlockEntity extends BlockEntity {
    private static final String SWORD_TAG = "Sword";
    private static final String SWORD_FACING_TAG = "SwordFacing";

    private ItemStack storedSword = ItemStack.EMPTY;
    private Direction swordFacing = Direction.NORTH;

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

    /** 插剑时记录的水平朝向，渲染器用它决定剑面朝向。 */
    public Direction getSwordFacing() {
        return this.swordFacing;
    }

    public static boolean isAcceptedSword(ItemStack stack) {
        return stack.is(ModItems.SAND_MASTER_SWORD.get());
    }

    /** 放入完整沙噬剑；破损形态不会被接受。 */
    public void setSword(ItemStack stack, Direction facing) {
        if (!isAcceptedSword(stack)) {
            return;
        }
        this.storedSword = stack.copyWithCount(1);
        if (facing.getAxis().isHorizontal()) {
            this.swordFacing = facing;
        }
        this.markUpdated();
    }

    /** 取回剑并将剑座清空。 */
    public ItemStack removeSword() {
        if (this.storedSword.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.storedSword.copy();
        this.storedSword = ItemStack.EMPTY;
        this.markUpdated();
        return result;
    }

    /** 方块被移除时使用：清空数据但不向即将消失的方块发送更新包。 */
    public ItemStack clearSword() {
        if (this.storedSword.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.storedSword.copy();
        this.storedSword = ItemStack.EMPTY;
        this.setChanged();
        return result;
    }

    private void markUpdated() {
        this.setChanged();
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