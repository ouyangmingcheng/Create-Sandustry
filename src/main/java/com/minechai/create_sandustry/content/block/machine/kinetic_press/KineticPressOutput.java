package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One item result of a kinetic pressing recipe with a server-rolled probability. */
public record KineticPressOutput(Item item, int count, float chance) {
    private record OutputData(Item item, int count, float chance) {}

    private static final Codec<OutputData> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(OutputData::item),
            Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(OutputData::count),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(OutputData::chance)
    ).apply(instance, OutputData::new));

    public static final Codec<KineticPressOutput> CODEC = DATA_CODEC.flatXmap(data -> {
        try {
            return DataResult.success(new KineticPressOutput(data.item(), data.count(), data.chance()));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }, output -> DataResult.success(new OutputData(output.item(), output.count(), output.chance())));

    public KineticPressOutput {
        if (item == null || item == Items.AIR) throw new IllegalArgumentException("Kinetic Press output item must not be air");
        if (count < 1 || count > 99) throw new IllegalArgumentException("Kinetic Press output count must be between 1 and 99");
        if (!Float.isFinite(chance) || chance < 0.0F || chance > 1.0F) {
            throw new IllegalArgumentException("Kinetic Press output chance must be a finite number between 0 and 1");
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, KineticPressOutput> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(Registries.ITEM), KineticPressOutput::item,
                    ByteBufCodecs.VAR_INT, KineticPressOutput::count,
                    ByteBufCodecs.FLOAT, KineticPressOutput::chance,
                    KineticPressOutput::new);

    public ItemStack getStack() {
        return new ItemStack(item, count);
    }

    /** Returns the maximum stack used for output-capacity preflight checks. */
    public ItemStack getMaximumStack() {
        return chance <= 0.0F ? ItemStack.EMPTY : getStack();
    }

    /** This is only called by the server-side processor. */
    public ItemStack roll(RandomSource random) {
        if (chance <= 0.0F) {
            return ItemStack.EMPTY;
        }
        if (chance >= 1.0F) {
            return getStack();
        }

        int rolled = 0;
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() < chance) {
                rolled++;
            }
        }
        return rolled == 0 ? ItemStack.EMPTY : new ItemStack(item, rolled);
    }
}
