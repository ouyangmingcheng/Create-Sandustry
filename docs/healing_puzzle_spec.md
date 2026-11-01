# Song of Healing 谜题规格

## 基本数据

- 谜题 ID：`createsandustry:healing`
- 作者/曲名：`Nintendo-Song of Healing`
- 谜面音频：`createsandustry:music.healing_clue`
- 谜底音频：`createsandustry:music.healing_solution`
- 谜面时长：2.8571429 秒
- 谜底时长：8.9628798 秒
- 输入超时：200 tick（10 秒）
- 蒸汽笛判定范围：16 格
- 音频范围：16 格

## MIDI 答案

来自 `healing.mid`，3 个音符、按顺序判定：

```text
B3 -> D4 -> B3
MIDI: 59 -> 62 -> 59
```

对应蒸汽笛：

| 音名 | MIDI | 音区 | pitch | pitchId |
|---|---:|---|---:|---:|
| B3 | 59 | MEDIUM | 7 | 107 |
| D4 | 62 | MEDIUM | 4 | 104 |
| B3 | 59 | MEDIUM | 7 | 107 |

## 物品

谜题唱片：

```text
createsandustry:music_disc_healing
```

- 显示名：音乐唱片 / Music Disc
- Tooltip：`Nintendo-Song of Healing`
- 纹理：`createsandustry:item/music_disc_healing`

奖励：

```text
createsandustry:disc_fragment_healing
```

- 显示名：唱片残片 / Music Disc Fragment
- Tooltip：`Nintendo-Song of Healing`
- 使用原版 `DiscFragmentItem`
- 继承共享模型：`createsandustry:item/disc_fragment`
- 纹理：`createsandustry:item/disc_fragment`

## 配置

```text
data/createsandustry/melody_puzzle/healing.json
```

```json
{
  "id": "healing",
  "clue_sound": "createsandustry:music.healing_clue",
  "solution_sound": "createsandustry:music.healing_solution",
  "answer": [59, 62, 59],
  "input_timeout": 200,
  "range": 16,
  "audio_range": 16,
  "reward": [
    {
      "item": "createsandustry:disc_fragment_healing",
      "count": 1
    }
  ]
}
```
