package com.minechai.create_sandustry.content.kineticpress;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModSounds;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressSoundGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressSoundGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void sixteenParallelTasksPlayOneQuietStartAndOneImpact(GameTestHelper h) {
        withMachine(h, List.of(recipe(Items.CARROT, 20)), (press, capture) -> {
            press.receiveEnergy(3000, false);
            for (int slot = 0; slot < 16; slot++) {
                press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false);
            }
            h.assertTrue(capture.sounds.isEmpty(), "Collecting input must not play processing sounds");
            ticks(press, 1);
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 16
                            && capture.count(ModSounds.KINETIC_PRESS_START.get()) == 1,
                    "Sixteen simultaneous starts must produce one cue");
            ticks(press, 19);
            h.assertTrue(capture.sounds.size() == 1, "No impact may precede actual task completion");
            ticks(press, 1);
            h.assertTrue(capture.sounds.size() == 2 && capture.count(ModSounds.KINETIC_PRESS_IMPACT.get()) == 1
                            && press.getProcessor().getActiveTaskCount() == 0 && seeds(press) == 16,
                    "Sixteen completions must produce one impact and all sixteen products");
            h.assertTrue(capture.sounds.stream().allMatch(sound -> sound.source() == SoundSource.BLOCKS
                            && sound.volume() > 0 && sound.volume() <= 0.4F && sound.pitch() == 1.0F),
                    "Processing cues must use quiet block audio; resource definitions lower their pitch");
            ticks(press, 40);
            h.assertTrue(capture.sounds.size() == 2, "Idle output retries must not replay processing sounds");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void waitingPauseStructureFailureAndReloadDoNotReplayStart(GameTestHelper h) {
        withMachine(h, List.of(recipe(Items.CARROT, 20)), (press, capture) -> {
            press.getInputHandler().insertItem(0, new ItemStack(Items.CARROT), false);
            ticks(press, 30);
            h.assertTrue(capture.sounds.isEmpty(), "Waiting for energy must remain silent");
            for (int slot = 0; slot < 9; slot++) {
                press.getOutputHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
            }
            press.receiveEnergy(100, false);
            ticks(press, 30);
            h.assertTrue(capture.sounds.isEmpty(), "Waiting for output capacity must remain silent");
            press.getOutputHandler().setStackInSlot(8, ItemStack.EMPTY);
            ticks(press, 1);
            ticks(press, 5);
            h.setBlock(BASE.east(), Blocks.REDSTONE_BLOCK);
            ticks(press, 30);
            h.setBlock(BASE.east(), Blocks.AIR);
            h.setBlock(BASE.above(3), Blocks.AIR);
            press.requestStructureCheck();
            ticks(press, 30);
            h.assertTrue(capture.sounds.size() == 1 && press.getProcessor().getActiveTaskCount() == 1,
                    "Paused and invalid structures must neither land nor replay the start cue");

            h.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
            press.requestStructureCheck();
            CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored = new KineticPressBlockEntity(press.getBlockPos(), press.getBlockState());
            restored.setLevel(h.getLevel());
            restored.loadWithComponents(saved, h.getLevel().registryAccess());
            h.getLevel().setBlockEntity(restored);
            restored.requestStructureCheck();
            ticks(restored, 14);
            h.assertTrue(capture.sounds.size() == 1,
                    "Reloaded paid work must resume without startup or premature completion sounds");
            ticks(restored, 1);
            h.assertTrue(capture.sounds.size() == 2 && capture.count(ModSounds.KINETIC_PRESS_IMPACT.get()) == 1
                            && seeds(restored) == 1 && restored.getEnergyStored() == 0,
                    "Resumed work must land exactly once without charging again");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void nearbyCompletionWavesCoalesceWithoutDelayingProducts(GameTestHelper h) {
        withMachine(h, List.of(recipe(Items.CARROT, 20), recipe(Items.POTATO, 21)), (press, capture) -> {
            press.receiveEnergy(3000, false);
            for (int slot = 0; slot < 16; slot++) {
                press.getInputHandler().insertItem(slot,
                        new ItemStack(slot % 2 == 0 ? Items.CARROT : Items.POTATO), false);
            }
            ticks(press, 21);
            h.assertTrue(seeds(press) == 8 && capture.count(ModSounds.KINETIC_PRESS_IMPACT.get()) == 1,
                    "The first completion wave must play an impact immediately");
            ticks(press, 1);
            h.assertTrue(seeds(press) == 16 && press.getProcessor().getActiveTaskCount() == 0
                            && capture.count(ModSounds.KINETIC_PRESS_IMPACT.get()) == 1,
                    "Nearby completion waves must coalesce audio without delaying or losing output");
        });
        h.succeed();
    }

    private static void withMachine(GameTestHelper h, List<? extends RecipeHolder<?>> recipes,
                                    BiConsumer<KineticPressBlockEntity, SoundCapture> scenario) {
        var manager = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(manager.getRecipes());
        SoundCapture capture = new SoundCapture(h.getLevel(), h.absolutePos(BASE));
        NeoForge.EVENT_BUS.register(capture);
        try {
            manager.replaceRecipes(new ArrayList<RecipeHolder<?>>(recipes));
            h.setBlock(BASE.below(), Blocks.STONE);
            h.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
            KineticPressBlockEntity press = h.getBlockEntity(BASE);
            CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
            saved.putInt("workingHeight", 3);
            press.loadWithComponents(saved, h.getLevel().registryAccess());
            h.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
            press.requestStructureCheck();
            scenario.accept(press, capture);
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
            manager.replaceRecipes(original);
        }
    }

    private static RecipeHolder<KineticPressRecipe> recipe(Item input, int ticks) {
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID,
                "sound_test_" + ticks), new KineticPressRecipe(Ingredient.of(input),
                List.of(new KineticPressOutput(ModItems.AMETHYST_FLOWER_SEEDS.get(), 1, 1)), ticks, 3, 100));
    }

    private static void ticks(KineticPressBlockEntity press, int count) {
        for (int tick = 0; tick < count; tick++) press.getProcessor().tick();
    }

    private static int seeds(KineticPressBlockEntity press) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            ItemStack stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(ModItems.AMETHYST_FLOWER_SEEDS.get())) count += stack.getCount();
        }
        return count;
    }

    private record Emission(SoundEvent sound, SoundSource source, float volume, float pitch) {}

    private static final class SoundCapture {
        private final Level level;
        private final BlockPos pos;
        private final List<Emission> sounds = new ArrayList<>();

        private SoundCapture(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @SubscribeEvent
        public void onSound(PlayLevelSoundEvent.AtPosition event) {
            if (event.getLevel() != level || !BlockPos.containing(event.getPosition()).equals(pos)
                    || event.getSound() == null) return;
            SoundEvent sound = event.getSound().value();
            if (sound == ModSounds.KINETIC_PRESS_START.get() || sound == ModSounds.KINETIC_PRESS_IMPACT.get()) {
                sounds.add(new Emission(sound, event.getSource(), event.getOriginalVolume(), event.getOriginalPitch()));
            }
        }

        private long count(SoundEvent sound) {
            return sounds.stream().filter(emission -> emission.sound() == sound).count();
        }
    }
}
