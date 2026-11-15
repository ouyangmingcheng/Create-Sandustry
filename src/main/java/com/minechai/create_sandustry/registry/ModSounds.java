package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateSandustry.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> OPENING_CLUE =
            register("music.opening_clue");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPENING_SOLUTION =
            register("music.opening_solution");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENING_CLUE =
            register("music.awakening_clue");
    public static final DeferredHolder<SoundEvent, SoundEvent> AWAKENING_SOLUTION =
            register("music.awakening_solution");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEALING_CLUE =
            register("music.healing_clue");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEALING_SOLUTION =
            register("music.healing_solution");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIRDS_CLUE =
            register("music.birds_clue");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIRDS_SOLUTION =
            register("music.birds_solution");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIG_NORTHEAST_CLUE =
            register("music.bignortheastismyhomeland_clue");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIG_NORTHEAST_SOLUTION =
            register("music.bignortheastismyhomeland_solution");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private ModSounds() {
    }
}
