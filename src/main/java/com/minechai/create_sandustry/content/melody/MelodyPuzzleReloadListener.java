package com.minechai.create_sandustry.content.melody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 从 data/<namespace>/melody_puzzle/<id>.json 加载谜题定义。 */
public final class MelodyPuzzleReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "melody_puzzle";
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateSandustry/MelodyPuzzleConfigs");
    private static final Gson GSON = new Gson();

    private final RegistryAccess registries;

    public MelodyPuzzleReloadListener(RegistryAccess registries) {
        super(GSON, DIRECTORY);
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, MelodyPuzzleDefinition> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                MelodyPuzzleDefinition definition = parse(entry.getKey(), entry.getValue());
                loaded.put(definition.id(), definition);
            } catch (Exception exception) {
                LOGGER.error("Ignoring invalid melody puzzle config {}: {}", entry.getKey(), exception.getMessage());
            }
        }
        MelodyPuzzleManager.setDefinitions(loaded);
        LOGGER.info("Loaded {} melody puzzle definition(s)", loaded.size());
    }

    private MelodyPuzzleDefinition parse(ResourceLocation fileId, JsonElement element) {
        JsonObject json = GsonHelper.convertToJsonObject(element, "melody puzzle");
        ResourceLocation id = parseId(GsonHelper.getAsString(json, "id"));
        if (!fileId.equals(id)) {
            throw new IllegalArgumentException("id must match file id " + fileId);
        }

        ResourceLocation clueSound = parseId(GsonHelper.getAsString(json, "clue_sound"));
        ResourceLocation solutionSound = parseId(GsonHelper.getAsString(json, "solution_sound"));
        requireSound(clueSound);
        requireSound(solutionSound);

        Holder<JukeboxSong> clueSong = findSongForSound(clueSound);
        Holder<JukeboxSong> solutionSong = findSongForSound(solutionSound);

        List<Integer> answer = new ArrayList<>();
        for (JsonElement noteElement : GsonHelper.getAsJsonArray(json, "answer")) {
            int midi = GsonHelper.convertToInt(noteElement, "answer note");
            if (midi < 0 || midi > 127) {
                throw new IllegalArgumentException("MIDI note out of range: " + midi);
            }
            answer.add(midi);
        }
        if (answer.isEmpty() || answer.size() > 256) {
            throw new IllegalArgumentException("answer must contain 1..256 notes");
        }

        int timeout = GsonHelper.getAsInt(json, "input_timeout");
        if (timeout <= 0 || timeout > 72_000) {
            throw new IllegalArgumentException("input_timeout must be in 1..72000");
        }

        int range = GsonHelper.getAsInt(json, "range");
        if (range <= 0 || range > 128) {
            throw new IllegalArgumentException("range must be in 1..128");
        }

        int audioRange = GsonHelper.getAsInt(json, "audio_range", 16);
        if (audioRange <= 0 || audioRange > 64) {
            throw new IllegalArgumentException("audio_range must be in 1..64");
        }

        List<MelodyPuzzleDefinition.Reward> rewards = new ArrayList<>();
        if (json.has("reward")) {
            for (JsonElement rewardElement : GsonHelper.getAsJsonArray(json, "reward")) {
                JsonObject rewardJson = GsonHelper.convertToJsonObject(rewardElement, "reward");
                ResourceLocation itemId = parseId(GsonHelper.getAsString(rewardJson, "item"));
                Item item = BuiltInRegistries.ITEM.getOptional(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("unknown item " + itemId));
                int count = GsonHelper.getAsInt(rewardJson, "count", 1);
                if (count <= 0) {
                    throw new IllegalArgumentException("reward count must be positive: " + itemId);
                }
                ItemStack validationStack = new ItemStack(item, count);
                if (count > validationStack.getMaxStackSize()) {
                    throw new IllegalArgumentException("reward count exceeds max stack size: " + itemId);
                }
                rewards.add(new MelodyPuzzleDefinition.Reward(item, count));
            }
        }

        MelodyPuzzleDefinition.WeatherReward weather = null;
        if (json.has("weather")) {
            JsonObject weatherJson = GsonHelper.convertToJsonObject(json.get("weather"), "weather");
            String type = GsonHelper.getAsString(weatherJson, "type");
            MelodyPuzzleDefinition.WeatherKind kind = switch (type) {
                case "clear" -> MelodyPuzzleDefinition.WeatherKind.CLEAR;
                case "rain" -> MelodyPuzzleDefinition.WeatherKind.RAIN;
                case "thunder" -> MelodyPuzzleDefinition.WeatherKind.THUNDER;
                default -> throw new IllegalArgumentException("unknown weather type: " + type);
            };
            int duration = GsonHelper.getAsInt(weatherJson, "duration", 12_000);
            if (duration <= 0 || duration > 1_728_000) {
                throw new IllegalArgumentException("weather duration must be in 1..1728000 ticks");
            }
            weather = new MelodyPuzzleDefinition.WeatherReward(kind, duration);
        }

        if (rewards.isEmpty() && weather == null) {
            throw new IllegalArgumentException("puzzle must define at least one item or weather reward");
        }

        return new MelodyPuzzleDefinition(id, clueSound, solutionSound, clueSong, solutionSong,
                answer, timeout, range, audioRange, rewards, weather);
    }

    private void requireSound(ResourceLocation soundId) {
        if (BuiltInRegistries.SOUND_EVENT.getOptional(soundId).isEmpty()) {
            throw new IllegalArgumentException("unknown sound event " + soundId);
        }
    }

    private Holder<JukeboxSong> findSongForSound(ResourceLocation soundId) {
        List<Holder.Reference<JukeboxSong>> matches = registries.registryOrThrow(Registries.JUKEBOX_SONG)
                .holders()
                .filter(holder -> soundLocation(holder.value().soundEvent()).equals(soundId))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("expected exactly one jukebox song for " + soundId
                    + ", found " + matches.size());
        }
        return matches.getFirst();
    }

    private static ResourceLocation soundLocation(Holder<SoundEvent> holder) {
        return holder.unwrapKey()
                .map(key -> key.location())
                .orElseGet(() -> holder.value().getLocation());
    }

    private static ResourceLocation parseId(String value) {
        ResourceLocation id = value.indexOf(':') >= 0
                ? ResourceLocation.tryParse(value)
                : ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, value);
        if (id == null) {
            throw new IllegalArgumentException("invalid resource location: " + value);
        }
        return id;
    }
}
