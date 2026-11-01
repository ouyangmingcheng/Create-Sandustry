# 第一首曲子：唱片机谜题规格

## 已锁定规则

- 第一首曲子的答案按顺序判定，不再使用无序音高集合。
- 答案从原始 MIDI 整体降一个八度。
- 共 15 个音符，必须按顺序演奏。
- 连续相同的三个 A4 是三次独立演奏，不能对相邻同音去重。
- 当前只锁定答案和音频资源规格，尚未实现谜题判定逻辑。

## 原始 MIDI 顺序

原始 MIDI 文件：`midi_export.mid`

```text
A5, A5, A5, F#5, E5, F#5, A5, E5, C#5, E5, F#5, A5, G#5, E5, F#5
```

原始 MIDI 数值：

```text
81, 81, 81, 78, 76, 78, 81, 76, 73, 76, 78, 81, 80, 76, 78
```

## 第一首答案：整体降一个八度

目标 MIDI 顺序：

```text
69, 69, 69, 66, 64, 66, 69, 64, 61, 64, 66, 69, 68, 64, 66
```

音名顺序：

```text
A4, A4, A4, F#4, E4, F#4, A4, E4, C#4, E4, F#4, A4, G#4, E4, F#4
```

可直接用于后续实现的数组：

```java
public static final int[] FIRST_SONG_MIDI = {
    69, 69, 69, 66, 64, 66, 69, 64, 61, 64, 66, 69, 68, 64, 66
};
```

## 蒸汽笛可演奏映射

所有目标音都在 Create 蒸汽笛正常音域内：

| 音名 | MIDI | 蒸汽笛音区 | pitch | pitchId |
|---|---:|---|---:|---:|
| C#4 | 61 | MEDIUM | 5 | 105 |
| E4 | 64 | MEDIUM | 2 | 102 |
| F#4 | 66 | MEDIUM | 0 | 100 |
| G#4 | 68 | SMALL | 10 | 110 |
| A4 | 69 | SMALL | 9 | 109 |

因此 15 音答案对应的 `PitchId` 顺序为：

```text
109, 109, 109, 100, 102, 100, 109, 102, 105, 102, 100, 109, 110, 102, 100
```

对应 Java 数组：

```java
public static final int[] FIRST_SONG_PITCH_IDS = {
    109, 109, 109, 100, 102, 100, 109, 102, 105, 102, 100, 109, 110, 102, 100
};
```

注意：`PitchId = pitch + 100 * size.ordinal()`，其中 SMALL=0、MEDIUM=1、LARGE=2。

## MIDI 节奏数据

来自 `midi_export.mid` 的精确时间信息：

- 格式：MIDI Format 1
- 960 ticks / 四分音符
- 126 BPM
- 4/4
- 最大复音数：1

起始 tick：

```text
300, 780, 1740, 2220, 3660, 4140, 4620, 5100, 7020,
7500, 7980, 8940, 9900, 10860, 11820
```

持续 tick：

```text
460, 940, 460, 1420, 460, 460, 460, 1540, 460,
460, 940, 940, 940, 940, 3200
```

音符之间通常有 20 tick 的断奏间隔；第 8 音和第 9 音之间有 380 tick 的较长间隔。谜题判定是否要求节奏尚未确定，当前只锁定音高顺序。

## 音频规格

Minecraft 1.21.1 自定义声音使用 Ogg Vorbis：

- 扩展名：`.ogg`
- 编码：Ogg Vorbis
- 采样率：44100 Hz
- 声道：音乐建议 stereo
- 建议质量：Vorbis q5-q7（约 112-160 kbps VBR）
- 峰值：不高于约 -1 dBFS，避免削波
- 去除不必要的前导静音
- 两个音频保持相近响度
- 文件名只使用 ASCII 小写字母、数字和下划线

WAV/MP3 不能直接作为 Minecraft 自定义声音资源使用，可以作为制作母带。

## 文件放置位置

谜面音频：

```text
src/main/resources/assets/createsandustry/sounds/music/first_song_puzzle.ogg
```

谜底音频：

```text
src/main/resources/assets/createsandustry/sounds/music/first_song_answer.ogg
```

对应声音事件 ID：

```text
createsandustry:music_disc.first_song_puzzle
createsandustry:music_disc.first_song_answer
```

后续会注册：

```text
src/main/resources/assets/createsandustry/sounds.json
```

若二者都作为唱片机曲目，还会创建：

```text
src/main/resources/data/createsandustry/jukebox_song/first_song_puzzle.json
src/main/resources/data/createsandustry/jukebox_song/first_song_answer.json
```

`length_in_seconds` 必须匹配最终 OGG 的实际时长。
