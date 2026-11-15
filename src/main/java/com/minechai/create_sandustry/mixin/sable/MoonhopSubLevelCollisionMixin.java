package com.minechai.create_sandustry.mixin.sable;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.minechai.create_sandustry.content.compat.sable.SableMoonhopBounce;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SubLevelEntityCollision.class, remap = false)
public abstract class MoonhopSubLevelCollisionMixin {
    @ModifyVariable(method = "collide", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Vec3 createsandustry$serverTouchingContact(Vec3 motion, @Local(argsOnly = true) Entity entity) {
        return SableMoonhopBounce.serverContactProbe(entity, motion);
    }

    @Inject(method = "collide", at = @At("HEAD"))
    private static void createsandustry$incoming(Entity entity, Vec3 motion, Vec3 inherited, LevelReusedVectors sink,
            CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> ci) {
        SableMoonhopBounce.incoming(entity, motion, inherited);
    }

    @Definition(id = "serverPlayer", type = ServerPlayer.class)
    @Expression("? instanceof serverPlayer")
    @ModifyExpressionValue(method = "collide", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean createsandustry$serverMoonhopCollision(boolean serverPlayer,
            @Local(argsOnly = true) Entity entity, @Local(argsOnly = true, ordinal = 0) Vec3 motion) {
        return serverPlayer && !SableMoonhopBounce.needsServerCollision(entity, motion);
    }

    @Inject(method = "collide", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private static void createsandustry$actualTopContact(Entity entity, Vec3 motion, Vec3 inherited,
            LevelReusedVectors sink, CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> ci,
            @Local(name = "subLevel") SubLevel surface, @Local(name = "subLevelPose") Pose3d pose,
            @Local(name = "maxBlockPos") BlockPos.MutableBlockPos block,
            @Local(name = "collidedBlockState") BlockState state, @Local(name = "maxMTV") Vector3d mtv,
            @Local(name = "entityBoundsOBB") OrientedBoundingBox3d bounds) {
        SableMoonhopBounce.contact(entity, surface, pose, block, state, mtv, bounds);
    }
}
