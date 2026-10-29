package com.minechai.create_sandustry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minechai.create_sandustry.content.item.SandMasterSwordEvents;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

/**
 * 物品栏界面里的丢弃（Q 单击丢 1 个、Ctrl+Q 丢整组）统一走
 * {@code AbstractContainerMenu#clicked(..., ClickType.THROW, ...)}。
 *
 * <p>在方法最开始拦截，物品不会被 {@code Slot#safeTake} 移出槽位；客户端与服务端都会经过这里，
 * 因此客户端也不会出现先移除再复原的闪烁。</p>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void createsandustry$blockProtectedSwordThrow(int slotId, int button, ClickType clickType, Player player,
            CallbackInfo ci) {
        if (clickType != ClickType.THROW || slotId < 0) {
            return;
        }
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (slotId >= self.slots.size()) {
            return;
        }
        if (SandMasterSwordEvents.isProtectedSword(self.getSlot(slotId).getItem())) {
            ci.cancel();
        }
    }
}