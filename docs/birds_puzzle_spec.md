# Song of Birds 谜题规格

## 基本数据

- 谜题 ID：`createsandustry:birds`
- 作者/曲名：`Nintendo-Song of Birds`
- 谜面音频：`createsandustry:music.birds_clue`
- 谜底音频：`createsandustry:music.birds_solution`
- 谜面时长：2.8571429 秒
- 谜底时长：8.89322 秒
- 输入超时：200 tick（10 秒）
- 蒸汽笛判定范围：16 格
- 音频范围：16 格

## MIDI 答案

来自 `birds.mid`，3 个音符、按顺序判定：

```text
A4 -> G#4 -> A4
MIDI: 69 -> 68 -> 69
```

对应蒸汽笛：

| 音名 | MIDI | 音区 | pitch | pitchId |
|---|---:|---|---:|---:|
| A4 | 69 | SMALL | 9 | 109 |
| G#4 | 68 | SMALL | 10 | 110 |
| A4 | 69 | SMALL | 9 | 109 |

## 物品

谜题唱片：

```text
createsandustry:music_disc_birds
```

- 显示名：音乐唱片 / Music Disc
- Tooltip：`Nintendo-Song of Birds`
- 纹理：`createsandustry:item/music_disc_birds`

奖励：

```text
createsandustry:disc_fragment_birds
```

- 显示名：唱片残片 / Music Disc Fragment
- Tooltip：`Nintendo-Song of Birds`
- 使用原版 `DiscFragmentItem`
- 继承共享模型：`createsandustry:item/disc_fragment`
- 纹理：`createsandustry:item/disc_fragment`

## 配置

```text
data/createsandustry/melody_puzzle/birds.json
```

```json
{
  "id": "birds",
  "clue_sound": "createsandustry:music.birds_clue",
  "solution_sound": "createsandustry:music.birds_solution",
  "answer": [69, 68, 69],
  "input_timeout": 200,
  "range": 16,
  "audio_range": 16,
  "reward": [
    {
      "item": "createsandustry:disc_fragment_birds",
      "count": 1
    }
  ]
}
```
