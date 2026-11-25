package com.minechai.create_sandustry.foundation.render;

import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

public final class GoldenGlintRenderBuffers {
    public static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(GoldenGlintRenderType.GOLDEN_GLINT);
        event.registerRenderBuffer(GoldenGlintRenderType.GOLDEN_GLINT_TRANSLUCENT);
        event.registerRenderBuffer(GoldenGlintRenderType.GOLDEN_ENTITY_GLINT_DIRECT);
    }

    private GoldenGlintRenderBuffers() {
    }
}