# Create 蒸汽笛服务端演奏事件技术验证

## 环境与结论

- Minecraft 1.21.1 / NeoForge 21.1.251
- Create 6.0.10-280
- Create 没有提供蒸汽笛专用的服务端演奏事件。
- 蒸汽笛的权威演奏起点是服务端方块状态 `WhistleBlock.POWERED` 的 `false -> true` 上升沿。
- 实际声音只在客户端由 `WhistleBlockEntity#tickAudio` 创建，不能作为服务端判定依据。
- 使用 Mixin 注入 `WhistleBlock#neighborChanged` 的 `RETURN` 点，可以无侵入地捕获每次红石上升沿，并取得音高、坐标、音区、来源邻居坐标和当前来源方块状态。
- GameTest 已实际验证：持续通电不重复触发；断电后重新通电会再次触发；`MEDIUM pitch=0 -> MIDI 66`，`MEDIUM pitch=1 -> MIDI 65`；岩浆供热时服务端判定 `audible=true`。

## 相关源码

反编译源码根目录：`output/create-src`

| 内容 | 类 / 方法 | 位置 |
|---|---|---|
| 方块注册 | `AllBlocks.STEAM_WHISTLE`、`STEAM_WHISTLE_EXTENSION` | `com/simibubi/create/AllBlocks.java:1073` |
| 方块实体注册 | `AllBlockEntityTypes.STEAM_WHISTLE` | `com/simibubi/create/AllBlockEntityTypes.java:667` |
| 方块状态 | `WhistleBlock.FACING/WALL/POWERED/SIZE` | `com/simibubi/create/content/decoration/steamWhistle/WhistleBlock.java:51-58` |
| 放置时电源状态 | `WhistleBlock#getStateForPlacement` | `WhistleBlock.java:95-113` |
| 音高更新调度 | `WhistleBlock#queuePitchUpdate/tick` | `WhistleBlock.java:171-181` |
| 服务端红石边沿 | `WhistleBlock#neighborChanged` | `WhistleBlock.java:196-204` |
| 音区/音高存储 | `WhistleBlockEntity#pitch`、`getPitchId` | `WhistleBlockEntity.java:42-43, 175-179` |
| 音高扫描 | `WhistleBlockEntity#updatePitch` | `WhistleBlockEntity.java:56-78` |
| 服务端/客户端 tick | `WhistleBlockEntity#tick` | `WhistleBlockEntity.java:80-96` |
| 客户端发声 | `WhistleBlockEntity#tickAudio` | `WhistleBlockEntity.java:129-173` |
| 延伸器状态 | `WhistleExtenderBlock.SHAPE/SIZE` | `WhistleExtenderBlock.java:36-47` |
| 延伸器更新根音高 | `WhistleExtenderBlock#onPlace/onRemove` | `WhistleExtenderBlock.java:142-152` |
| 循环音源 | `WhistleSoundInstance` | `WhistleSoundInstance.java:15-48` |
| 音区对应声音事件 | `AllSoundEvents.WHISTLE_HIGH/MEDIUM/LOW` | `AllSoundEvents.java:287-300` |
| 管道风琴进度使用音高 ID | `BoilerData#checkPipeOrganAdvancement` | `BoilerData.java:347-379` |

## 触发调用链

1. 红石电源变化，服务端调用方块邻居更新。
2. `WhistleBlock#neighborChanged` 首先执行 `if (worldIn.isClientSide) return;`，因此这段权威状态切换只在逻辑服务端执行。
3. 比较旧状态 `POWERED` 与 `worldIn.hasNeighborSignal(pos)`。
4. 若不同，执行：
   `worldIn.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS)`。
5. `POWERED` 同步到客户端。
6. 客户端 `WhistleBlockEntity#tick` 检查 `POWERED`、所属锅炉是否活跃以及是否有热源。
7. 满足条件时客户端 `tickAudio(size, powered)`：
   - 第一次进入时创建循环 `WhistleSoundInstance`；
   - 播放对应音区声音；
   - 设置音高倍率 `2^(-pitch/12)`；
   - 添加蒸汽粒子。
8. 服务端 `WhistleBlockEntity#tick` 只处理进度/成就，不播放声音。

因此，服务端谜题应捕获第 3-4 步产生的状态边沿，而不是监听声音。

## 音高数据与范围

### 存储

- `WhistleBlockEntity.pitch` 是私有 `int`，通过 NBT 键 `Pitch` 持久化并在更新时同步。
- `pitch=0` 表示该音区基准音 F#。
- 每增加一个半音，`pitch` 加 1，实际频率按 `2^(-pitch/12)` 下降。
- 音区由方块状态 `SIZE` 表示，枚举顺序为 `SMALL=0, MEDIUM=1, LARGE=2`。
- `getPitchId()` 的公开编码为 `pitch + 100 * SIZE.ordinal()`。

### updatePitch 扫描规则

从蒸汽笛上方开始：

- 没有延伸器：`pitch=0`。
- 遇到 `SHAPE=SINGLE` 的延伸器：音高加 1 并停止。
- 遇到 `DOUBLE` 或 `DOUBLE_CONNECTED`：音高加 2，继续向上。
- 扫描循环上限条件为 `newPitch <= 24`。

正常交互 `WhistleBlock#incrementSize` 最多处理 6 个延伸器位置，每个位置最多贡献 2 个半音，因此正常玩法范围为 `0..12`。源码没有额外硬性 clamp；通过命令、NBT 或其他模组可以写入更高值，扫描逻辑本身在外部放置 13 个连续 DOUBLE 延伸器时可计算到 26，且没有显式 clamp。谜题实现建议只接受正常范围并在系统边界做合法性校验。

## MIDI 映射

声音事件按音区选择：

- `SMALL -> create:whistle_high`
- `MEDIUM -> create:whistle`
- `LARGE -> create:whistle_low`

结合 `create.generic.notes` 的 `pitch % 12` 映射（从 F# 向下半音）和对实际 OGG 音频的频谱检查，得到：

| WhistleSize | pitch=0 基准 | MIDI |
|---|---|---:|
| SMALL | F#5 | 78 |
| MEDIUM | F#4 | 66 |
| LARGE | F#3 | 54 |

映射公式：

```java
midi = base[size] - pitch;
base = switch (size) {
    case SMALL -> 78;   // F#5
    case MEDIUM -> 66;  // F#4
    case LARGE -> 54;   // F#3
};
```

例：

- `MEDIUM, pitch=0 -> MIDI 66`
- `MEDIUM, pitch=1 -> MIDI 65`
- `SMALL, pitch=12 -> MIDI 66`
- `LARGE, pitch=0 -> MIDI 54`

`pitch=12` 会在听感上回到下一音区的 F#；这是正常范围上界造成的重叠，不是新的音区规则。

## 原生事件可行性

### Create

反编译源码中没有蒸汽笛专用事件、`WhistleEvent`、管道风琴演奏事件或可由服务端订阅的 NoteBlock 等价事件。

### NeoForge

存在通用服务端事件：

- `net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent`
- 仅在服务端触发。
- 提供发生邻居通知的方块坐标、方块状态、通知方向和强制红石更新标志。
- 在 `ServerLevel#updateNeighborsAt` 中于 `neighborChanged` 回调之前发布。

它不能直接提供：

- 被通知的蒸汽笛目标；
- 蒸汽笛音高/音区；
- 已完成的 `POWERED` 新状态；
- 触发玩家/实体。

理论上可以在事件中扫描 `getNotifiedSides()` 的相邻方块，找到 `WhistleBlock` 并预测 `!POWERED && hasNeighborSignal`，但这是间接推断，存在取消事件、额外扫描和事件时序问题，不如 Mixin 精确。

NeoForge 的 `PlaySoundEvent` / `PlaySoundSourceEvent` 属于客户端事件，禁止用于服务端谜题判定。

## 新演奏与持续鸣响

- 新演奏：`POWERED` 从 `false` 变为 `true`。
- 持续鸣响：`POWERED` 保持 `true`，服务端不会再产生新的状态边沿。
- 停止：`POWERED` 从 `true` 变为 `false`，客户端 `tickAudio` 会淡出音源。
- 若谜题需要音符时值，可对称增加“停止事件”；只需要音高序列时，上升沿事件足够。

当前 Mixin 的事件定义是“权威红石上升沿”。两个边界情况需要明确：

- 蒸汽笛被直接放置在已经通电的位置时，`getStateForPlacement` 初始即为 `POWERED=true`，没有 `false -> true` 的 `neighborChanged`，不会产生本事件。
- 已通电区块重新加载也不会产生本事件，这通常正是谜题想要的结果，可避免重登/区块加载产生幽灵音符。

如果确实要把“放置即通电”也算一次演奏，应额外处理 `WhistleBlock#onPlace`，并在方块实体创建后的下一个服务端任务/tick 发布事件；不要简单地每 tick 将 `POWERED=true` 当作新演奏，否则区块重载会误触发。

## 触发者与来源

- `fromPos` 是本次邻居更新来源坐标，通常是最接近蒸汽笛的变化方块。
- 红石线场景中，它可能是相邻红石线，而不是最终拉杆/按钮。
- `blockIn` 参数可能代表更新前方块，不能可靠地当作当前电源类型。
- 当前实现额外读取 `level.getBlockState(fromPos)`，通过事件的 `getSourceState()` 提供当前来源方块状态。
- Create 的演奏逻辑没有玩家/实体参数，因此无法通用地确定“谁触发了演奏”。若谜题需要玩家归因，应使用唱片机交互产生的服务端玩家会话，或对附近玩家交互做带时间窗口的辅助关联，不能把红石来源直接等同于玩家。

## 推荐实现

推荐使用项目内已加入的：

- `SteamWhistlePerformanceEvent`
- `SteamWhistleBlockMixin`
- `SteamWhistlePerformanceProbe`（仅验证探针）

Mixin 设计：

```java
@Mixin(WhistleBlock.class)
public abstract class SteamWhistleBlockMixin {
    @Inject(method = "neighborChanged", at = @At("RETURN"))
    private void createsandustry$capturePerformanceStart(
            BlockState previousState, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean isMoving, CallbackInfo ci) {
        // 仅逻辑服务端；比较 previousState.POWERED 与 level.getBlockState(pos).POWERED
        // 只在 false -> true 时发布事件。
    }
}
```

该注入点位于 Create 完成 `POWERED` 方块状态切换之后，不取消、不替换、不修改原方法返回值或状态。事件读取：

- 蒸汽笛坐标与 `WhistleBlockEntity`
- `SIZE`
- `getPitchId()` 及拆分后的 `pitch`
- MIDI
- 旧/新方块状态
- `fromPos` 与来源坐标当前状态
- 锅炉热源条件
- 游戏 tick

## 验证实现与结果

新增验证文件：

- `src/main/java/com/minechai/create_sandustry/content/steamwhistle/SteamWhistlePerformanceEvent.java`
- `src/main/java/com/minechai/create_sandustry/content/steamwhistle/SteamWhistlePerformanceProbe.java`
- `src/main/java/com/minechai/create_sandustry/content/steamwhistle/SteamWhistlePerformanceGameTests.java`
- `src/main/java/com/minechai/create_sandustry/mixin/SteamWhistleBlockMixin.java`
- `src/main/resources/data/createsandustry/structure/steam_whistle_empty.nbt`

GameTest 场景：

1. 放置岩浆、流体罐和蒸汽笛。
2. 放置红石块产生第一次上升沿。
3. 保持通电数 tick，断言只捕获一次。
4. 移除红石块。
5. 放置一个 `SINGLE` 延伸器，使音高变为 1。
6. 再次放置红石块，断言捕获第二次。
7. 校验服务端、坐标、来源红石块、可发声状态和 MIDI。

实际运行结果：

```text
steam whistle: size=MEDIUM pitch=0 pitchId=100 midi=66 sourceBlock=Block of Redstone audible=true tick=34
steam whistle: size=MEDIUM pitch=1 pitchId=101 midi=65 sourceBlock=Block of Redstone audible=true tick=50
All 12 required tests passed :)
```

该结果证明：逻辑服务端能够准确捕获新演奏、音高和 MIDI，并能避免持续鸣响重复计数。
