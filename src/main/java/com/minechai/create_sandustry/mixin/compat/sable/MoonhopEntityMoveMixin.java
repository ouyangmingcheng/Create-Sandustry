package com.minechai.create_sandustry.mixin.compat.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.compat.sable.SableMoonhopBounce;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Entity.class, priority = 900)
public abstract class MoonhopEntityMoveMixin implements SableMoonhopBounce.ContactAccess {
    @Unique private final SableMoonhopBounce.ContactState createsandustry$moonhopContact = new SableMoonhopBounce.ContactState();

    @Override public SableMoonhopBounce.ContactState createsandustry$moonhopContact() { return createsandustry$moonhopContact; }

    @WrapMethod(method = "move")
    private void createsandustry$moonhopMove(MoverType type, Vec3 motion, Operation<Void> original) {
        var move = SableMoonhopBounce.begin((Entity) (Object) this, type, createsandustry$moonhopContact);
        boolean completed = false;
        try {
            original.call(type, motion);
            completed = true;
        } finally {
            SableMoonhopBounce.end(move, completed);
        }
    }

    @WrapMethod(method = "checkFallDamage")
    private void createsandustry$physicalLanding(double dy, boolean onGround, BlockState state, BlockPos pos,
            Operation<Void> original) {
        Entity entity = (Entity) (Object) this;
        BlockPos physical = SableMoonhopBounce.landingBlock(entity);
        original.call(dy, onGround, physical == null ? state : entity.level().getBlockState(physical),
                physical == null ? pos : physical);
    }
}
