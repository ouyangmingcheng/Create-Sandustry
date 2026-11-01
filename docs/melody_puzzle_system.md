# 沙金工艺：Create 蒸汽笛音乐解密系统

## 1. 实现概览

- 复用原版 `JukeboxBlock` / `JukeboxBlockEntity`，没有新增重复唱片机。
- 特殊谜面唱片仍是原版 `JUKEBOX_PLAYABLE` 物品，可正常插入唱片机。
- 普通音乐唱片不创建谜题会话，行为完全沿用原版。
- 服务端通过 `JukeboxBlockEntity` Mixin 接管特殊唱片的谜题生命周期。
- 旋律输入复用已通过验证的 `SteamWhistlePerformanceEvent`。
- 谜题答案、音频、超时、范围和奖励由数据包 JSON 提供。
- 客户端只接收状态镜像，不参与任何判定。

## 2. 新增文件

### Java

```text
src/main/java/com/minechai/create_sandustry/content/melody/MelodyPuzzleDefinition.java
src/main/java/com/minechai/create_sandustry/content/melody/MelodyPuzzleManager.java
src/main/java/com/minechai/create_sandustry/content/melody/MelodyPuzzleReloadListener.java
src/main/java/com/minechai/create_sandustry/content/melody/MelodyPuzzleState.java
src/main/java/com/minechai/create_sandustry/content/melody/MelodyPuzzleGameTests.java
src/main/java/com/minechai/create_sandustry/content/client/event/MelodyPuzzleClientState.java
src/main/java/com/minechai/create_sandustry/mixin/JukeboxBlockEntityPuzzleMixin.java
src/main/java/com/minechai/create_sandustry/mixin/ServerLevelJukeboxAudioMixin.java
src/main/java/com/minechai/create_sandustry/content/client/event/MelodyPuzzleSoundInstance.java
src/main/java/com/minechai/create_sandustry/content/client/event/MelodyPuzzleAudioClient.java
src/main/java/com/minechai/create_sandustry/network/MelodyPuzzleAudioPayload.java
src/main/java/com/minechai/create_sandustry/network/MelodyPuzzleStatePayload.java
src/main/java/com/minechai/create_sandustry/registry/ModJukeboxSongs.java
src/main/java/com/minechai/create_sandustry/registry/ModSounds.java
```

### 资源与数据

```text
src/main/resources/assets/createsandustry/sounds.json
src/main/resources/assets/createsandustry/sounds/music/opening_clue.ogg
src/main/resources/assets/createsandustry/sounds/music/opening_solution.ogg
src/main/resources/assets/createsandustry/models/item/music_disc_opening.json
src/main/resources/data/createsandustry/jukebox_song/opening_clue.json
src/main/resources/data/createsandustry/jukebox_song/opening_solution.json
src/main/resources/data/createsandustry/melody_puzzle/opening.json
```

## 3. 修改文件

```text
src/main/java/com/minechai/create_sandustry/CreateSandustry.java
src/main/java/com/minechai/create_sandustry/registry/ModItems.java
src/main/java/com/minechai/create_sandustry/registry/ModCreativeTabs.java
src/main/java/com/minechai/create_sandustry/registry/ModDataComponents.java
src/main/java/com/minechai/create_sandustry/network/ModNetwork.java
src/main/resources/createsandustry.mixins.json
src/main/resources/assets/createsandustry/lang/en_us.json
src/main/resources/assets/createsandustry/lang/zh_cn.json
```

已经存在的蒸汽笛捕获代码没有重新实现：

```text
src/main/java/com/minechai/create_sandustry/content/steamwhistle/SteamWhistlePerformanceEvent.java
src/main/java/com/minechai/create_sandustry/mixin/SteamWhistleBlockMixin.java
```

## 4. 系统架构

### 原版 Jukebox 适配层

`JukeboxBlockEntityPuzzleMixin` 只桥接两个生命周期点：

- `setTheItem` 返回后：
  - 谜面唱片插入时创建会话；
  - 物品清空时释放会话；
  - 普通唱片没有谜题组件，不创建会话。
- `loadAdditional` 返回后：
  - 下一服务端 tick 调用恢复逻辑；
  - 已保存的谜面唱片不会因为静态会话丢失而永久卡死。

原版 `JukeboxSongPlayer` 只用于服务端歌曲 tick 和时长换算。特殊谜题唱片的原版 1010/1011 声音广播由 `ServerLevelJukeboxAudioMixin` 拦截，改由自定义网络包发送给 `audio_range` 内玩家。

客户端 `MelodyPuzzleSoundInstance` 每 tick 根据玩家距离更新音量，超过 16 格时音量为 0；状态清理或中断时发送 STOP 包。普通唱片不经过该逻辑。

### MelodyPuzzleManager

职责：

- 按 `(维度, 唱片机坐标)` 管理相互独立的会话；
- 接收蒸汽笛事件；
- 同维度、范围内查找候选唱片机；
- 最近距离优先，距离相同时按方块坐标稳定排序；
- 一个演奏事件只会分配给一台唱片机；
- 按游戏 tick 暂存音符，以检测同一 tick 多音；
- 校验音高序列；
- 超时、失败、成功和奖励；
- 区块卸载/服务器停止清理；
- 服务端状态同步。

没有每 tick 扫描世界。服务端 tick 只遍历当前活跃会话。

### 数据加载

`MelodyPuzzleReloadListener` 使用 `SimpleJsonResourceReloadListener` 从：

```text
data/<namespace>/melody_puzzle/<id>.json
```

加载配置，并在 `AddReloadListenerEvent` 中注册。

每个谜面/谜底声音通过 `sound_event` 在 JukeboxSong 动态注册表中反查唯一歌曲，因此服务端直接使用原版歌曲时长，不解析 OGG 或依赖客户端播放事件。

## 5. 谜题数据格式

当前配置：

```text
src/main/resources/data/createsandustry/melody_puzzle/opening.json
```

结构：

```json
{
  "id": "opening",
  "clue_sound": "createsandustry:music.opening_clue",
  "solution_sound": "createsandustry:music.opening_solution",
  "answer": [69, 69, 66],
  "input_timeout": 400,
  "range": 16,
  "audio_range": 16,
  "reward": [
    {
      "item": "createsandustry:sand_sword",
      "count": 1
    }
  ]
}
```

校验规则：

- `id` 必须与文件名一致；无命名空间时默认使用 `createsandustry`。
- 谜面/谜底声音必须存在。
- 每个声音必须能唯一匹配一个 JukeboxSong。
- `answer` 不能为空，长度不超过 256，MIDI 范围必须是 0..127。
- `input_timeout` 必须为 1..72000 tick。
- `range` 必须为 1..128 格。
- `audio_range` 必须为 1..64 格，默认 16；谜面和谜底都只发送给该半径内玩家。
- 奖励不能为空，物品 ID 必须存在，数量必须为正且不超过最大堆叠数。
- 非法配置记录错误并跳过，不会导致服务器启动失败。

## 6. opening 第一首曲子

谜题 ID：

```text
createsandustry:opening
```

谜面唱片：

```text
createsandustry:music_disc_opening
```

答案按 15 个 MIDI 音符顺序严格验证：

```text
69, 69, 69, 66, 64, 66, 69, 64, 61,
64, 66, 69, 68, 64, 66
```

对应：

```text
A4, A4, A4, F#4, E4, F#4, A4, E4, C#4,
E4, F#4, A4, G#4, E4, F#4
```

奖励：

```text
createsandustry:sand_sword x1
```

注意：连续三个 A4 是三次独立 POWERED 上升沿，不能去重。

## 7. 状态机

```text
IDLE
  -> 插入 opening 谜面唱片
PLAYING_CLUE
  -> 原版 JukeboxSong 播放到配置时长
WAITING_INPUT
  -> 每个不同游戏 tick 的单个有效音符推进
  -> 错误音符 / 同 tick 多音符 / 超时
FAILED
  -> 停止声音，弹出谜面唱片，释放会话
WAITING_INPUT
  -> 最后一个音符正确
PLAYING_SOLUTION
  -> JukeboxSongPlayer 播放谜底，持续完整配置时长
REWARDING
  -> 消耗谜面唱片，弹出沙金剑，释放会话
```

- 所有状态由服务端修改。
- 状态变化通过 `MelodyPuzzleStatePayload` 同步给附近玩家。
- 谜面阶段不会接收蒸汽笛音符。
- 持续通电不产生新的 `SteamWhistlePerformanceEvent`。
- 不满足实际发声条件的蒸汽笛事件被直接忽略，不推进也不判错。
- 同一 tick 同一唱片机收到多个有效音符时直接失败，不按回调顺序判定。

## 8. 物品流与可靠性

- 插入：原版 `JukeboxPlayable.tryInsertIntoJukebox` 消耗玩家 1 张唱片，再交给原版 `setTheItem`。
- 失败：`popOutTheItem()` 由原版清楚唱片并弹出一次谜面唱片。
- 手动取出：原版负责弹出谜面唱片；管理器只释放会话并停止音频。
- 破坏唱片机：原版 `onRemove` 弹出谜面唱片；管理器释放会话，不额外掉落奖励。
- 成功：谜底完整播放结束后清空唱片机内的谜面唱片，然后弹出奖励。
- 奖励通过 `ItemEntity` 从唱片机上方弹出，不依赖外部容器库存空间。
- 区块卸载：释放内存会话；重新加载时优先继续可恢复的谜面歌曲，否则安全地重新播放谜面。
- 服务器停止：清空内存会话；加载后由 Jukebox BlockEntity 的 `loadAdditional` 调度恢复。
- 同一唱片机只存在一个会话，同一蒸汽笛事件最多归一台唱片机。

## 9. GameTest

执行命令：

```powershell
gradlew.bat runGameTestServer -x createMinecraftArtifacts --no-daemon
```

最终结果：

```text
========= 25 GAME TESTS COMPLETE =========
All 25 required tests passed :)
```

新增覆盖：

- opening 配置及 15 音答案正确加载。
- 正确音符推进进度。
- 错误音符立即失败并弹出谜面唱片。
- 15 音完整正确后进入谜底播放，谜底结束前不发奖励。
- 谜底结束后只弹出沙金剑。
- 无热源/不可发声事件不推进、不判错。
- 精确 400 tick（20 秒）输入超时失败。
- 同一 tick 多音符失败。
- 不同维度事件不影响谜题。
- 超出监听范围事件不影响谜题。
- 两个唱片机独立推进，互不串进度。
- 共享监听范围内按最近唱片机归属事件。
- 破坏唱片机只掉谜面唱片，不掉奖励。
- 会话丢失后的恢复会重新建立并播放谜面，不会永久卡死。
- 原有蒸汽笛捕获测试继续通过：持续通电只捕获一次。

完整构建：

```powershell
gradlew.bat build -x createMinecraftArtifacts -x test -x runGameTestServer --no-daemon
```

构建通过，产物：

```text
build/libs/createsandustry-0.1.6.jar
```

## 10. 尚未解决 / 后续确认

1. opening 谜面当前没有生存模式配方，只能通过创造模式、命令、任务或其他系统获得；未擅自添加获取途径。
2. 成功时会消耗谜面唱片并弹出沙金剑，以避免同一唱片反复刷奖励。如果希望唱片可重复使用，需要增加每台唱片机或每位玩家的完成标记，否则会重复发放奖励。
3. 极端服务器崩溃恰好发生在“清空唱片”和“生成奖励物品实体”之间时，原版物品实体与方块实体不提供事务级原子性；正常区块卸载、服务器保存和停止流程已处理，但完全断电级事务未实现。
4. 客户端当前只建立状态镜像，尚无进度 HUD/粒子提示；判定和状态同步已经完成。
5. 谜面输入目前只校验音高顺序，不校验 MIDI 节奏/音符时值；如果后续要求按原曲节奏演奏，需要扩展为带 tick 窗口的音符事件。
