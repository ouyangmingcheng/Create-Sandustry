# Song of Awakening 谜题规格

## 基本数据

- 谜题 ID：`createsandustry:awakening`
- 作者/曲名：`Nintendo-Song of Awakening`
- 谜面音频：`createsandustry:music.awakening_clue`
- 谜底音频：`createsandustry:music.awakening_solution`
- 谜面时长：1.500 秒
- 谜底时长：7.4303628 秒
- 输入超时：200 tick（10 秒）
- 蒸汽笛判定范围：16 格
- 音频范围：16 格

## MIDI 答案

来自 `awakening.mid`，2 个音符、按顺序判定：

```text
E4 -> F#4
MIDI: 64 -> 66
```

对应蒸汽笛：

| 音名 | MIDI | 音区 | pitch | pitchId |
|---|---:|---|---:|---:|
| E4 | 64 | MEDIUM | 2 | 102 |
| F#4 | 66 | MEDIUM | 0 | 100 |

## 物品

谜题唱片：

```text
createsandustry:music_disc_awakening
```

- 显示名：音乐唱片 / Music Disc
- Tooltip：`Nintendo-Song of Awakening`
- 纹理：`createsandustry:item/music_disc_awakening`

奖励：

```text
createsandustry:disc_fragment_awakening
```

- 显示名：唱片残片 / Music Disc Fragment
- Tooltip：`Nintendo-Song of Awakening`
- 使用原版 `DiscFragmentItem`

## 唱片残片纹理约定

共享基础模型：

```text
assets/createsandustry/models/item/disc_fragment.json
```

其唯一纹理：

```text
assets/createsandustry/textures/item/disc_fragment.png
```

后续新增残片只需创建对应模型并继承：

```json
{
  "parent": "createsandustry:item/disc_fragment"
}
```

这样所有唱片残片统一使用 `disc_fragment.png`。

## 配置

```text
data/createsandustry/melody_puzzle/awakening.json
```

```json
{
  "id": "awakening",
  "clue_sound": "createsandustry:music.awakening_clue",
  "solution_sound": "createsandustry:music.awakening_solution",
  "answer": [64, 66],
  "input_timeout": 200,
  "range": 16,
  "audio_range": 16,
  "reward": [
    {
      "item": "createsandustry:disc_fragment_awakening",
      "count": 1
    }
  ]
}
```
