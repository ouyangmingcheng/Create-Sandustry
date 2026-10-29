package com.minechai.create_sandustry.content.client.event;

import com.minechai.create_sandustry.CreateSandustry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import org.jetbrains.annotations.Nullable;

/**
 * 提前创建世界内溜槽嘴 PartialModel，使其在 Flywheel 的 RegisterAdditional 阶段自动烘焙。
 */
public final class SiftingBeltClientEvents {
    public static final ResourceLocation CHUTE_MOUTH_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/sifting_chute_mouth");
    public static final PartialModel CHUTE_MOUTH = PartialModel.of(CHUTE_MOUTH_ID);

    public static final ResourceLocation SMART_CHUTE_MOUTH_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/sifting_smart_chute_mouth");
    public static final PartialModel SMART_CHUTE_MOUTH = PartialModel.of(SMART_CHUTE_MOUTH_ID);

    public static final ResourceLocation SMART_CHUTE_POWERED_MOUTH_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/sifting_smart_chute_powered_mouth");
    public static final PartialModel SMART_CHUTE_POWERED_MOUTH = PartialModel.of(SMART_CHUTE_POWERED_MOUTH_ID);

    public static final ResourceLocation CONNECTED_BRASS_CHUTE_MOUTH_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CreateSandustry.MODID, "block/sifting_connected_brass_chute_mouth");
    @Nullable
    public static final PartialModel CONNECTED_BRASS_CHUTE_MOUTH =
            ModList.get().isLoaded("create_connected") ? PartialModel.of(CONNECTED_BRASS_CHUTE_MOUTH_ID) : null;

    private SiftingBeltClientEvents() {
    }

    public static void init() {
        // 触发类初始化，让 PartialModel.ALL 在模型注册前包含溜槽嘴模型。
    }
}
