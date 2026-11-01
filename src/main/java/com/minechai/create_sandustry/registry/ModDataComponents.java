package com.minechai.create_sandustry.registry;

import java.util.UUID;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.api.florinol.FlorinolToolBinding;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergySnapshot;
import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateSandustry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GOLDEN_SELECTION =
            DATA_COMPONENT_TYPES.register("golden_selection", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> THERMAL_HEAT =
            DATA_COMPONENT_TYPES.register("thermal_heat", () -> DataComponentType.<Double>builder()
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    /** 通过剑座试炼后绑定的剑主 UUID；旧沙噬剑或直接获得的剑可以没有此组件。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SAND_MASTER_SWORD_OWNER =
            DATA_COMPONENT_TYPES.register("sand_master_sword_owner", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    /** 工具绑定的弗洛林电池总控；维度、坐标与电池 UUID 会随物品保存。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlorinolToolBinding>> FLORINOL_TOOL_BINDING =
            DATA_COMPONENT_TYPES.register("florinol_tool_binding", () -> DataComponentType.<FlorinolToolBinding>builder()
                    .persistent(FlorinolToolBinding.CODEC)
                    .networkSynchronized(FlorinolToolBinding.STREAM_CODEC)
                    .build());
    /** 工具最近一次同步到的绑定电池 FE，供 Tooltip 与 FE 条显示。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlorinolToolEnergySnapshot>> FLORINOL_TOOL_ENERGY =
            DATA_COMPONENT_TYPES.register("florinol_tool_energy", () -> DataComponentType.<FlorinolToolEnergySnapshot>builder()
                    .persistent(FlorinolToolEnergySnapshot.CODEC)
                    .networkSynchronized(FlorinolToolEnergySnapshot.STREAM_CODEC)
                    .build());

    /** 谜面唱片对应的服务端谜题 ID；普通唱片没有此组件。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MELODY_PUZZLE_ID =
            DATA_COMPONENT_TYPES.register("melody_puzzle_id", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }

    private ModDataComponents() {
    }
}
