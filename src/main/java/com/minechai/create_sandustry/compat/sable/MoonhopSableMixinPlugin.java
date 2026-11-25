package com.minechai.create_sandustry.compat.sable;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Do not link any Sable API classes while starting a game without Sable. */
public final class MoonhopSableMixinPlugin implements IMixinConfigPlugin {
    private boolean present;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(
                    "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision");
            present = true;
        } catch (Exception absent) {
            present = false;
        }
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return present; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
