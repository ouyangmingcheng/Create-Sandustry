package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

public final class ModJukeboxSongs {
    public static final ResourceKey<JukeboxSong> OPENING_CLUE = create("opening_clue");
    public static final ResourceKey<JukeboxSong> OPENING_SOLUTION = create("opening_solution");
    public static final ResourceKey<JukeboxSong> AWAKENING_CLUE = create("awakening_clue");
    public static final ResourceKey<JukeboxSong> AWAKENING_SOLUTION = create("awakening_solution");
    public static final ResourceKey<JukeboxSong> HEALING_CLUE = create("healing_clue");
    public static final ResourceKey<JukeboxSong> HEALING_SOLUTION = create("healing_solution");
    public static final ResourceKey<JukeboxSong> BIRDS_CLUE = create("birds_clue");
    public static final ResourceKey<JukeboxSong> BIRDS_SOLUTION = create("birds_solution");
    public static final ResourceKey<JukeboxSong> BIG_NORTHEAST_CLUE = create("bignortheastismyhomeland_clue");
    public static final ResourceKey<JukeboxSong> BIG_NORTHEAST_SOLUTION = create("bignortheastismyhomeland_solution");

    private static ResourceKey<JukeboxSong> create(String path) {
        return ResourceKey.create(Registries.JUKEBOX_SONG,
                ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, path));
    }

    private ModJukeboxSongs() {
    }
}
