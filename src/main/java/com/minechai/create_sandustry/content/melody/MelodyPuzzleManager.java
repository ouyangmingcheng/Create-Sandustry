package com.minechai.create_sandustry.content.melody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.steamwhistle.SteamWhistlePerformanceEvent;
import com.minechai.create_sandustry.network.MelodyPuzzleAudioPayload;
import com.minechai.create_sandustry.network.MelodyPuzzleStatePayload;
import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 服务端权威的唱片机蒸汽笛旋律谜题管理器。 */
public final class MelodyPuzzleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateSandustry/MelodyPuzzle");
    private static final Map<ResourceLocation, MelodyPuzzleDefinition> DEFINITIONS = new HashMap<>();
    private static final Map<SessionKey, PuzzleSession> SESSIONS = new LinkedHashMap<>();

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MelodyPuzzleReloadListener(event.getRegistryAccess()));
    }

    static void setDefinitions(Map<ResourceLocation, MelodyPuzzleDefinition> definitions) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(definitions);
    }

    public static void onPerformance(SteamWhistlePerformanceEvent event) {
        if (!event.isAudibleOnServer()) {
            return;
        }
        queueNoteInput(event.getLevel(), event.getWhistlePos(), event.getMidiNote(), true, event.getGameTime());
    }

    public static void onJukeboxItemChanged(ServerLevel level, JukeboxBlockEntity jukebox, ItemStack stack) {
        SessionKey key = key(level, jukebox.getBlockPos());
        PuzzleSession existing = SESSIONS.get(key);

        if (stack.isEmpty()) {
            if (existing != null) {
                endSession(existing, level, "jukebox item cleared");
            }
            return;
        }

        ResourceLocation puzzleId = puzzleId(stack);
        if (puzzleId == null) {
            if (existing != null) {
                endSession(existing, level, "non-puzzle item inserted");
            }
            return;
        }

        MelodyPuzzleDefinition definition = DEFINITIONS.get(puzzleId);
        if (definition == null) {
            LOGGER.warn("Jukebox at {} uses unknown melody puzzle '{}'", jukebox.getBlockPos(), puzzleId);
            if (existing != null) {
                endSession(existing, level, "puzzle definition removed");
            }
            return;
        }

        if (existing != null) {
            if (existing.closing) {
                return;
            }
            if (existing.definition.id().equals(definition.id())) {
                return;
            }
            endSession(existing, level, "puzzle item changed");
        }

        PuzzleSession session = new PuzzleSession(key, definition, level.getGameTime());
        SESSIONS.put(key, session);
        JukeboxSongPlayer songPlayer = jukebox.getSongPlayer();
        if (!songPlayer.isPlaying() || !sameSong(songPlayer.getSong(), definition.clueSong().value())) {
            songPlayer.play(level, definition.clueSong());
        }
        playAudio(level, session.key.pos(), definition.clueSong().value().soundEvent(), definition.audioRange());
        sync(session, level);
        LOGGER.info("Started melody puzzle '{}' at {}", definition.id(), jukebox.getBlockPos());
    }

    /** 区块加载或服务器重载后恢复；无法确定进度时安全地重新开始谜面。 */
    public static void recoverJukebox(ServerLevel level, JukeboxBlockEntity jukebox) {
        SessionKey key = key(level, jukebox.getBlockPos());
        LOGGER.debug("Recovery check for {}: existing={}, stack={}", key, SESSIONS.containsKey(key), jukebox.getTheItem());
        if (SESSIONS.containsKey(key)) {
            return;
        }
        ItemStack stack = jukebox.getTheItem();
        if (stack.isEmpty()) {
            LOGGER.warn("Cannot recover jukebox at {}: item is empty", jukebox.getBlockPos());
            return;
        }
        ResourceLocation puzzleId = puzzleId(stack);
        if (puzzleId == null) {
            LOGGER.warn("Cannot recover jukebox at {}: item has no puzzle id", jukebox.getBlockPos());
            return;
        }
        MelodyPuzzleDefinition definition = DEFINITIONS.get(puzzleId);
        if (definition == null) {
            LOGGER.warn("Cannot recover melody puzzle '{}' at {}: definition is not loaded",
                    puzzleId, jukebox.getBlockPos());
            return;
        }

        PuzzleSession session = new PuzzleSession(key, definition, level.getGameTime());
        SESSIONS.put(key, session);
        jukebox.getSongPlayer().play(level, definition.clueSong());
        playAudio(level, session.key.pos(), definition.clueSong().value().soundEvent(), definition.audioRange());
        sync(session, level);
        LOGGER.info("Recovered melody puzzle '{}' at {} by restarting/continuing the clue phase",
                definition.id(), jukebox.getBlockPos());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (PuzzleSession session : new ArrayList<>(SESSIONS.values())) {
            ServerLevel level = event.getServer().getLevel(session.key.dimension());
            if (level != null) {
                tickSession(session, level, level.getGameTime());
            }
        }
    }

    static void tickSession(PuzzleSession session, ServerLevel level, long gameTime) {
        if (session.closing || SESSIONS.get(session.key) != session) {
            return;
        }

        BlockEntity blockEntity = level.isLoaded(session.key.pos()) ? level.getBlockEntity(session.key.pos()) : null;
        if (!(blockEntity instanceof JukeboxBlockEntity jukebox)
                || !(level.getBlockState(session.key.pos()).getBlock() instanceof JukeboxBlock)) {
            endSession(session, level, "jukebox missing");
            return;
        }

        ResourceLocation currentPuzzleId = puzzleId(jukebox.getTheItem());
        if (!session.definition.id().equals(currentPuzzleId)) {
            endSession(session, level, "puzzle disc no longer present");
            return;
        }

        if (session.state == MelodyPuzzleState.WAITING_INPUT
                && gameTime - session.inputStartedAt >= session.definition.inputTimeoutTicks()) {
            fail(session, level, jukebox, "input timeout");
            return;
        }

        if (!session.pendingNotes.isEmpty() && session.pendingTick <= gameTime) {
            List<Integer> notes = List.copyOf(session.pendingNotes);
            session.pendingNotes.clear();
            if (notes.size() > 1) {
                fail(session, level, jukebox, "multiple notes in one tick");
                return;
            }
            if (!notes.isEmpty()) {
                handleNote(session, level, jukebox, notes.getFirst());
                if (session.closing || SESSIONS.get(session.key) != session) {
                    return;
                }
            }
        }

        JukeboxSongPlayer songPlayer = jukebox.getSongPlayer();
        if (session.state == MelodyPuzzleState.PLAYING_CLUE) {
            if (!songPlayer.isPlaying()
                    || songPlayer.getTicksSinceSongStarted() >= session.definition.clueLengthTicks()) {
                stopAudio(level, session.key.pos(), session.definition.clueSong().value().soundEvent(), session.definition.audioRange());
                stopSong(level, jukebox);
                session.state = MelodyPuzzleState.WAITING_INPUT;
                session.inputStartedAt = gameTime;
                sync(session, level);
                LOGGER.info("Clue finished for melody puzzle '{}' at {}; waiting for input",
                        session.definition.id(), jukebox.getBlockPos());
            }
            return;
        }

        if (session.state == MelodyPuzzleState.PLAYING_SOLUTION) {
            if (!songPlayer.isPlaying()
                    || songPlayer.getTicksSinceSongStarted() >= session.definition.solutionLengthTicks()) {
                complete(session, level, jukebox);
            }
        }
    }

    private static void handleNote(PuzzleSession session, ServerLevel level, JukeboxBlockEntity jukebox, int midi) {
        if (session.state != MelodyPuzzleState.WAITING_INPUT) {
            return;
        }
        int expected = session.definition.answer().get(session.progress);
        if (midi != expected) {
            fail(session, level, jukebox, "expected MIDI " + expected + " but received " + midi);
            return;
        }

        session.progress++;
        sync(session, level);
        if (session.progress >= session.definition.answer().size()) {
            session.state = MelodyPuzzleState.PLAYING_SOLUTION;
            session.stateStartedAt = level.getGameTime();
            stopAudio(level, session.key.pos(), session.definition.clueSong().value().soundEvent(), session.definition.audioRange());
            jukebox.getSongPlayer().play(level, session.definition.solutionSong());
            playAudio(level, session.key.pos(), session.definition.solutionSong().value().soundEvent(), session.definition.audioRange());
            sync(session, level);
            LOGGER.info("Melody puzzle '{}' at {} solved; playing solution",
                    session.definition.id(), jukebox.getBlockPos());
        }
    }

    private static void fail(PuzzleSession session, ServerLevel level, JukeboxBlockEntity jukebox, String reason) {
        if (session.closing) {
            return;
        }
        session.closing = true;
        session.state = MelodyPuzzleState.FAILED;
        stopAudio(level, session.key.pos(), session.definition.clueSong().value().soundEvent(), session.definition.audioRange());
        stopAudio(level, session.key.pos(), session.definition.solutionSong().value().soundEvent(), session.definition.audioRange());
        stopSong(level, jukebox);
        sync(session, level);
        LOGGER.info("Melody puzzle '{}' at {} failed: {}", session.definition.id(), jukebox.getBlockPos(), reason);

        if (session.definition.id().equals(puzzleId(jukebox.getTheItem()))) {
            jukebox.popOutTheItem();
        }
        SESSIONS.remove(session.key);
        syncIdle(level, session.key.pos());
    }

    private static void complete(PuzzleSession session, ServerLevel level, JukeboxBlockEntity jukebox) {
        if (session.closing) {
            return;
        }
        session.closing = true;
        session.state = MelodyPuzzleState.REWARDING;
        stopAudio(level, session.key.pos(), session.definition.solutionSong().value().soundEvent(), session.definition.audioRange());
        stopSong(level, jukebox);
        sync(session, level);

        boolean validDisc = session.definition.id().equals(puzzleId(jukebox.getTheItem()));
        if (validDisc) {
            jukebox.setTheItem(ItemStack.EMPTY);
            for (MelodyPuzzleDefinition.Reward reward : session.definition.rewards()) {
                dropReward(level, session.key.pos(), reward.createStack());
            }
            applyWeatherReward(level, session.definition.weather());
        } else {
            LOGGER.warn("Melody puzzle '{}' at {} completed without its clue disc; reward suppressed",
                    session.definition.id(), session.key.pos());
        }

        SESSIONS.remove(session.key);
        syncIdle(level, session.key.pos());
        LOGGER.info("Melody puzzle '{}' at {} rewarded and released",
                session.definition.id(), session.key.pos());
    }

    static void queueNoteInput(ServerLevel level, BlockPos whistlePos, int midi, boolean audible, long gameTime) {
        if (!audible) {
            return;
        }

        PuzzleSession nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PuzzleSession session : SESSIONS.values()) {
            if (!session.key.dimension().equals(level.dimension())
                    || session.state != MelodyPuzzleState.WAITING_INPUT
                    || session.closing) {
                continue;
            }
            double distance = Vec3.atCenterOf(session.key.pos()).distanceToSqr(Vec3.atCenterOf(whistlePos));
            double range = session.definition.range();
            if (distance > range * range) {
                continue;
            }
            if (nearest == null
                    || distance < nearestDistance
                    || (distance == nearestDistance && comparePos(session.key.pos(), nearest.key.pos()) < 0)) {
                nearest = session;
                nearestDistance = distance;
            }
        }

        if (nearest != null) {
            queueForSession(nearest, midi, gameTime);
        }
    }

    private static void queueForSession(PuzzleSession session, int midi, long gameTime) {
        if (session.pendingTick != gameTime) {
            session.pendingNotes.clear();
            session.pendingTick = gameTime;
        }
        session.pendingNotes.add(midi);
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        for (PuzzleSession session : new ArrayList<>(SESSIONS.values())) {
            if (session.key.dimension().equals(level.dimension())
                    && new ChunkPos(session.key.pos()).equals(chunkPos)) {
                endSession(session, level, "chunk unloaded");
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SESSIONS.clear();
    }

    private static void endSession(PuzzleSession session, ServerLevel level, String reason) {
        SESSIONS.remove(session.key);
        boolean alreadyClosing = session.closing;
        session.closing = true;
        if (!alreadyClosing) {
            session.state = MelodyPuzzleState.IDLE;
        }
        stopAllAudio(level, session);
        if (level.isLoaded(session.key.pos())
                && level.getBlockEntity(session.key.pos()) instanceof JukeboxBlockEntity jukebox) {
            stopSong(level, jukebox);
        }
        syncIdle(level, session.key.pos());
        LOGGER.info("Released melody puzzle '{}' at {}: {}", session.definition.id(), session.key.pos(), reason);
    }

    private static void stopSong(ServerLevel level, JukeboxBlockEntity jukebox) {
        JukeboxSongPlayer songPlayer = jukebox.getSongPlayer();
        if (songPlayer.isPlaying()) {
            songPlayer.stop(level, jukebox.getBlockState());
        }
    }

    private static boolean sameSong(@Nullable JukeboxSong first, JukeboxSong second) {
        return first != null && soundLocation(first.soundEvent()).equals(soundLocation(second.soundEvent()));
    }

    private static ResourceLocation soundLocation(Holder<SoundEvent> holder) {
        return holder.unwrapKey().map(key -> key.location()).orElseGet(() -> holder.value().getLocation());
    }

    private static ResourceLocation puzzleId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String value = stack.get(ModDataComponents.MELODY_PUZZLE_ID.get());
        if (value == null) {
            return null;
        }
        return value.indexOf(':') >= 0
                ? ResourceLocation.tryParse(value)
                : ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, value);
    }

    private static int comparePos(BlockPos first, BlockPos second) {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) {
            return x;
        }
        int y = Integer.compare(first.getY(), second.getY());
        return y != 0 ? y : Integer.compare(first.getZ(), second.getZ());
    }

    private static void applyWeatherReward(ServerLevel level, @Nullable MelodyPuzzleDefinition.WeatherReward weather) {
        if (weather == null) {
            return;
        }
        int duration = weather.durationTicks();
        switch (weather.kind()) {
            case CLEAR -> level.setWeatherParameters(duration, 0, false, false);
            case RAIN -> level.setWeatherParameters(0, duration, true, false);
            case THUNDER -> level.setWeatherParameters(0, duration, true, true);
        }
    }
    private static void dropReward(ServerLevel level, BlockPos pos, ItemStack stack) {
        Vec3 dropPos = Vec3.atLowerCornerWithOffset(pos, 0.5D, 1.01D, 0.5D)
                .offsetRandom(level.random, 0.7F);
        ItemEntity entity = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, stack.copy());
        entity.setDefaultPickUpDelay();
        if (!level.addFreshEntity(entity)) {
            LOGGER.error("Failed to spawn melody puzzle reward {} at {}", stack, pos);
        }
    }

    public static boolean isPuzzleDisc(ItemStack stack) {
        return puzzleId(stack) != null;
    }

    public static boolean hasSession(ServerLevel level, BlockPos pos) {
        return SESSIONS.containsKey(key(level, pos));
    }

    private static void playAudio(ServerLevel level, BlockPos pos, Holder<SoundEvent> sound, int range) {
        PacketDistributor.sendToPlayersNear(level, null,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, range,
                new MelodyPuzzleAudioPayload(pos, sound, MelodyPuzzleAudioPayload.PLAY, range));
    }

    private static void stopAudio(ServerLevel level, BlockPos pos, Holder<SoundEvent> sound, int range) {
        PacketDistributor.sendToPlayersNear(level, null,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, range,
                new MelodyPuzzleAudioPayload(pos, sound, MelodyPuzzleAudioPayload.STOP, range));
    }

    private static void stopAllAudio(ServerLevel level, PuzzleSession session) {
        stopAudio(level, session.key.pos(), session.definition.clueSong().value().soundEvent(),
                session.definition.audioRange());
        stopAudio(level, session.key.pos(), session.definition.solutionSong().value().soundEvent(),
                session.definition.audioRange());
    }
    private static void sync(PuzzleSession session, ServerLevel level) {
        MelodyPuzzleStatePayload payload = new MelodyPuzzleStatePayload(
                session.key.pos(), session.state.ordinal(), session.progress, session.definition.answer().size());
        PacketDistributor.sendToPlayersNear(level, null,
                session.key.pos().getX() + 0.5D, session.key.pos().getY() + 0.5D, session.key.pos().getZ() + 0.5D,
                Math.max(32.0D, session.definition.range() + 16.0D), payload);
    }

    private static void syncIdle(ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersNear(level, null,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                32.0D, new MelodyPuzzleStatePayload(pos, MelodyPuzzleState.IDLE.ordinal(), 0, 0));
    }

    private static SessionKey key(ServerLevel level, BlockPos pos) {
        return new SessionKey(level.dimension(), pos.immutable());
    }

    static @Nullable MelodyPuzzleDefinition definitionForTesting(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    static @Nullable PuzzleSession sessionForTesting(ServerLevel level, BlockPos pos) {
        return SESSIONS.get(key(level, pos));
    }

    static void forceWaitingForTesting(PuzzleSession session, ServerLevel level, long gameTime) {
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity) level.getBlockEntity(session.key.pos());
        stopSong(level, jukebox);
        session.state = MelodyPuzzleState.WAITING_INPUT;
        session.inputStartedAt = gameTime;
        session.stateStartedAt = gameTime;
        session.pendingNotes.clear();
        sync(session, level);
    }

    static void queueForSessionForTesting(PuzzleSession session, int midi, boolean audible, long gameTime) {
        if (audible) {
            queueForSession(session, midi, gameTime);
        }
    }

    static void removeForTesting(ServerLevel level, BlockPos pos) {
        PuzzleSession session = SESSIONS.get(key(level, pos));
        if (session != null) {
            endSession(session, level, "test cleanup");
        }
    }

    private MelodyPuzzleManager() {
    }

    record SessionKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    static final class PuzzleSession {
        final SessionKey key;
        final MelodyPuzzleDefinition definition;
        MelodyPuzzleState state = MelodyPuzzleState.PLAYING_CLUE;
        int progress;
        long stateStartedAt;
        long inputStartedAt;
        long pendingTick = Long.MIN_VALUE;
        final List<Integer> pendingNotes = new ArrayList<>();
        boolean closing;

        PuzzleSession(SessionKey key, MelodyPuzzleDefinition definition, long gameTime) {
            this.key = key;
            this.definition = definition;
            this.stateStartedAt = gameTime;
            this.inputStartedAt = gameTime;
        }
    }
}
