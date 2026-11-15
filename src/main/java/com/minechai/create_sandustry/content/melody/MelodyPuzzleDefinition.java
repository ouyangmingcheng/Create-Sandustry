package com.minechai.create_sandustry.content.melody;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.Nullable;

/** 不可变的谜题定义；会话在创建时持有一份快照。 */
public record MelodyPuzzleDefinition(
        ResourceLocation id,
        ResourceLocation clueSound,
        ResourceLocation solutionSound,
        Holder<JukeboxSong> clueSong,
        Holder<JukeboxSong> solutionSong,
        List<Integer> answer,
        int inputTimeoutTicks,
        int range,
        int audioRange,
        List<Reward> rewards,
        @Nullable WeatherReward weather) {

    public MelodyPuzzleDefinition {
        answer = List.copyOf(answer);
        rewards = List.copyOf(rewards);
    }

    public int clueLengthTicks() {
        return clueSong.value().lengthInTicks();
    }

    public int solutionLengthTicks() {
        return solutionSong.value().lengthInTicks();
    }

    public record Reward(Item item, int count) {
        public ItemStack createStack() {
            return new ItemStack(item, count);
        }
    }

    public enum WeatherKind {
        CLEAR,
        RAIN,
        THUNDER
    }

    public record WeatherReward(WeatherKind kind, int durationTicks) {
    }
}
