# 重力构件：源码调查、实施方案与验证记录

当前概率和活动轨迹以 [断连损坏率与活塞辅助记录](gravity_interruptions_and_pistons.md) 为准：成功率仍为首次5%、第15次必成，损坏率仅随有效断连按5/10/20/40/80/100%增长。弹跳物理由 [自然衰减修复记录](gravity_bounce_physics.md) 移除最低速度补偿，恢复重力、阻力和碰撞能量损失。下文保留首次实现调查与历史验证记录，最低反弹速度、按累计弹跳增长损坏率和强制连续完成的旧设计不再适用。

调查基线：Minecraft 1.21.1、NeoForge 21.1.251、Create 6.0.10-280、Java 21，模组 ID 为 `createsandustry`。版本依据为 [gradle.properties](../gradle.properties) 与 [build.gradle](../build.gradle)。

本报告中的 `output/create-src/` 路径为本地 Create 源码。弹射置物台调查涉及的五个文件，已与 Gradle 缓存中精确版本 `create-1.21.1-6.0.10-280-sources.jar` 逐字比较（仅归一换行），内容一致。Minecraft/NeoForge 调用链来自 `build/moddev/artifacts/neoforge-21.1.251-sources.jar`，本次相关源码提取于 `build/research/gravity/`。这些构建目录不是分发内容。

## A. 已验证的源码事实

### 1. 粘液块与掉落物：对需求前提的纠正

**1.21.1 的普通 `ItemEntity` 确实会执行粘液块反弹，没有按掉落物类型完全禁止弹跳的分支。** 其反弹系数为 0.8，随后还受到掉落物自身阻力，因而自然衰减，不能将普通掉落物的衰减误认为没有调用粘液块反弹。为任务半成品设置最低反弹初速度，是持续工艺所需的专属增强。

| 问题 | 源码结论与位置 |
| --- | --- |
| 粘液块在何时修改垂直速度？ | [`Entity#move`](../build/research/gravity/net/minecraft/world/entity/Entity.java)，621 行开始：先碰撞裁剪及更新位置，663 行设置 `verticalCollisionBelow`，670 行设置地面状态，673 行处理坠落，683–684 行在请求 Y 位移被裁剪时调用支撑方块的 `updateEntityAfterFallOn`。[`SlimeBlock`](../build/research/gravity/net/minecraft/world/level/block/SlimeBlock.java) 的 `fallOn`（26 行）主要处理坠落伤害；修改速度的是 `updateEntityAfterFallOn`（39 行）→ `bounceUp`（47 行）。 |
| 非生物弹跳系数？ | `SlimeBlock#bounceUp`，50–52 行：`LivingEntity` 为 1.0，其他实体为 0.8，且仅在当前 `vy < 0` 时翻转。 |
| 为什么普通掉落物表现不同？ | [`ItemEntity#tick`](../build/research/gravity/net/minecraft/world/entity/item/ItemEntity.java)，168–181 行：调用同一个 `Entity#move` 后，垂直速度乘 0.98；只有地面状态下速度仍为负，才额外乘 -0.5。粘液块已使速度为正时，不会再执行该负速度分支。因此不存在“掉落物不调用反弹”的结论，差异来自 0.8 弹性、0.04 重力和 0.98 阻力。 |
| 重力、移动、阻力的顺序？ | `ItemEntity#getDefaultGravity`，128–129 行为 0.04；`tick`，143–155 行先处理液体分支或 `applyGravity`，167 行进入移动分支，168 行 `move`，175 行乘阻力，176–181 行处理仍向下的落地速度。`Entity#getGravity/applyGravity`，1175–1183 行确认可读取本刻实际重力。水、岩浆和模组液体使用自己的运动分支，不应沿用空气运动推导。 |
| 如何判断从上方撞到粘液块？ | 组合本刻 Pre 的 `onGround == false`、下降速度和原位置，以及 Post 的 `verticalCollisionBelow && onGround()`、实际 Y 下降、`getOnPos()` 对应 `minecraft:slime_block`、实体底面到达块顶面与 X/Z 包围盒实际重叠。`Entity`，663–684 行表明这些字段正是本次移动的碰撞结果。不能只查询附近有没有粘液块。 |
| 如何区分首次落地、反弹、贴地与侧碰？ | 只有“空中下降 → 本次向下碰撞”才能开始反弹。开始后主动清除地面标记，进入上升状态；下一次落地还必须经历真实空中下降。贴地不满足 Pre 空中条件，侧碰不满足 `verticalCollisionBelow` 和块顶面接触条件。头顶碰撞不属于向下碰撞。 |
| 如何只增强指定半成品？ | 服务端 `EntityTickEvent.Pre/Post` 只处理已通过物品 ID、原生序列组件、配方结构及等待步骤校验的单件半成品。Pre 保存当前运动，空气中 `impactVy = preVy - entity.getGravity()`；Post 验证真实落地后，仅覆盖该实体 Y 速度，保留经过原生碰撞/阻力处理的 X/Z。无需修改 `Entity#move` 或所有粘液块。 |

普通方块的 `Block#updateEntityAfterFallOn` 会清零 Y 速度；粘液块覆盖该方法。`ItemEntity` 本身的地面负速度分支并不能用作“命中粘液块”的证据。

### 2. EntityTickEvent 与数据 API

- [`ServerLevel#tickNonPassenger`](../build/research/gravity/net/minecraft/server/level/ServerLevel.java)，771–780 行：对 `Entity` 直接执行 `fireEntityTickPre` → `entity.tick()` → `fireEntityTickPost`，没有排除 `ItemEntity`。这验证了事件真正覆盖掉落物，而非仅依据事件类名推断。
- [`EntityTickEvent`](../build/research/gravity/net/neoforged/neoforge/event/tick/EntityTickEvent.java)，24–26、45–49 行：Pre/Post 在两逻辑侧触发；Pre 被取消时实体 Tick 和 Post 均不会执行。加工监听器必须过滤到逻辑服务端，不取消原生 Tick。
- 同一精确源码 jar 中 `net/minecraft/client/multiplayer/ClientLevel.java`，298–305 行也包装了 Pre/Post。客户端事件不参与随机判定。
- [`AttachmentType`](../build/research/gravity/net/neoforged/neoforge/attachment/AttachmentType.java)，95–110 行提供默认值 builder；168–198 行支持自定义 NBT 序列化或 Codec 持久化。它适用于实体加工轨迹状态。
- [`AttachmentHolder`](../build/research/gravity/net/neoforged/neoforge/attachment/AttachmentHolder.java)，122–169 行保存/恢复可序列化 attachment。未提供 serializer 的临时快照不会写入存档。
- [`AllDataComponents.SEQUENCED_ASSEMBLY`](../output/create-src/com/simibubi/create/AllDataComponents.java)，246–248 行使用持久化 Codec 与网络 StreamCodec。项目现有 [`ModDataComponents`](../src/main/java/com/minechai/create_sandustry/registry/ModDataComponents.java) 已采用同样的数据组件注册体系。

### 3. 原生序列组装接入

| 已调查类/方法 | 结论 |
| --- | --- |
| [`DeployerBlockEntity#getRecipe`](../output/create-src/com/simibubi/create/content/kinetics/deployer/DeployerBlockEntity.java)，560–583 行 | 机械手通过 `SequencedAssemblyRecipe.getRecipe` 查询当前应执行的原生 deploying 步骤，执行材料仍由 Create 原生机械手配方验证。 |
| [`SequencedRecipe.CODEC/STREAM_CODEC`](../output/create-src/com/simibubi/create/content/processing/sequenced/SequencedRecipe.java)，15–28 行 | 自定义步骤必须同时属于 `ProcessingRecipe` 并实现 `IAssemblyRecipe`，磁盘解码和网络解码都会检查。`StandardProcessingRecipe` 继承满足前一个条件。 |
| 同文件 `initFromSequencedAssembly`，45–50 行 | 解码初始化时，第一步骤输入替换为“基础原料或半成品”，其他步骤为半成品。自定义步骤需要有正常的第 0 个输入槽。 |
| [`SequencedAssemblyRecipeSerializer`](../output/create-src/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipeSerializer.java)，18–35、40–55 行 | 支持注册 serializer 的自定义步骤，通过 `SequencedRecipe` 列表解码；不需要修改 Create 解码器。 |
| [`SequencedAssemblyRecipe#getRecipe`](../output/create-src/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe.java)，73–88 行 | 只返回配方当前步骤的目标类型，并用 `enforceNextResult` 交由父序列生成正确中间状态。 |
| 同文件 `advance`，112–125 行；`SequencedAssembly`，267–278 行 | 原生组件记录真实配方 ID、整数 step 和浮点 progress。中间产物由 transitional item 生成，最终一步调用父配方结果池。 |
| 同文件 `addToTooltip`，212–248 行 | 用组件里的真实 ID 查询客户端已同步的序列组装配方，显示原生序列状态、进度和步骤的 `getDescriptionForAssembly`。不存在的配方 ID 无法生成该 Tooltip。 |
| [`SequencedAssemblyItem`](../output/create-src/com/simibubi/create/content/processing/sequenced/SequencedAssemblyItem.java)，10–18、22–33 行 | 原生半成品最大堆叠数为 1；进度条直接读取原生组件的 progress。无需另写外观相似的进度系统。 |
| [`IAssemblyRecipe`](../output/create-src/com/simibubi/create/content/processing/sequenced/IAssemblyRecipe.java)，17–32 行 | 提供步骤描述、所需设备、额外材料和 JEI 子分类 supplier。 |
| [`SequencedAssemblyCategory#getSubCategory`](../output/create-src/com/simibubi/create/compat/jei/category/SequencedAssemblyCategory.java)，97–103 行 | JEI 根据步骤 serializer ID 缓存，并从步骤的 `getJEISubCategory` 创建子分类；可以在原生序列组装页面显示自定义弹跳步骤。 |

以上源码支持“原生 deploying 第一步 + 独立 recipe type 的弹跳第二步”，未发现必须绕开原生序列组装或伪造 Tooltip 的架构障碍。

### 4. 弹射置物台调查（本阶段不修改）

主要源码：[`EjectorBlockEntity`](../output/create-src/com/simibubi/create/content/logistics/depot/EjectorBlockEntity.java)、[`EntityLauncher`](../output/create-src/com/simibubi/create/content/logistics/depot/EntityLauncher.java)、[`DepotBehaviour`](../output/create-src/com/simibubi/create/content/logistics/depot/DepotBehaviour.java)、[`TransportedItemStack`](../output/create-src/com/simibubi/create/content/kinetics/belt/transport/TransportedItemStack.java)、[`DirectBeltInputBehaviour`](../output/create-src/com/simibubi/create/content/kinetics/belt/behaviour/DirectBeltInputBehaviour.java)。

1. **谁计算轨迹：** `EntityLauncher#getGlobalPos`（53 行）、`getGlobalVelocity`（67 行）、`y/dy`（75/84 行）、`recalculateTrajectory`（89 行）。`EjectorBlockEntity#getLaunchedItemLocation`（479 行）委托位置计算，`getLaunchedItemMotion`（483 行）委托速度计算后乘 0.5。
2. **开始、最高点、结束：** `activateDeferred`（146 行）调用 `launchItems`（213 行），把置物台栈移入 `launchedItems`。`addToLaunchedItems`（279–284 行）创建从 0 开始的 `IntAttached<ItemStack>`。`tick`（311–323 行）逐条推进计时；撞障或超过 `min(earlyTargetTime,totalFlyingTicks)` 时结束。没有最高点标记；`State.LAUNCHING` 描述的是盖板状态。`scanTrajectoryForObstacles`（368–398 行）可提前终止飞行。
3. **何时生成实体：** `placeItemAtTarget`（448–469 行）先通过目标 `DirectBeltInputBehaviour` 插入物品，剩余栈才生成 `ItemEntity`。`dropFlyingItems`（494–504 行）在弹射台销毁时将队列转换为实体。`activateDeferred`（164 行）明确跳过普通 `ItemEntity` 的实体弹射分支。因此仅监听掉落物 Tick 无法覆盖内部队列的飞行最高点。
4. **组件保留：** `EjectorBlockEntity#write/read`（529、560–561 行）用 `ItemStack.saveOptional/parseOptional` 保存完整栈及计时。`TransportedItemStack`（56、74–76、102 行）用 `stack.copy` 和完整栈保存/解析。`DepotBehaviour`（229–243 行）同样持久化 held/incoming/output；`DirectBeltInputBehaviour`（72–77、88–93 行）默认复制完整栈并进行插入。持久化 Data Component 可以随上述路径保留。
5. **扩展 API：** 没有发现公开的逐条飞行轨迹事件。`launchedItems`（74 行）和 `launcher`（77 行）为包访问字段，没有公开队列 getter。[`BlockEntityBehaviourEvent`](../output/create-src/com/simibubi/create/api/event/BlockEntityBehaviourEvent.java)（36–54 行）可挂载有 Tick/read/write 的行为，但本身无法公开访问队列，因此不足以在中途替换每个物品。
6. **未来最高点加工：** 优先使用上游增加的轨迹访问 API；若没有，再添加仅针对 `EjectorBlockEntity` 队列迭代的 accessor/hook，接入独立激活逻辑。每条飞行需要保存一次性加工标记、同步修改后的队列和正确保存进度。本阶段不添加该 Mixin。

未来实现还应注意两个细节：`getGlobalPos`（59–62 行）对实际高度有额外校正，而 `getGlobalVelocity` 使用未校正 `dy`，所以速度符号翻转未必严格对应显示轨迹的最高点；应检查实际位置函数的导数或相邻时间区间上升/下降趋势。替换队列中的 `ItemStack` 时还要维护 `trackedItem`，因为 `tick`（314 行）的障碍扫描使用引用相等，直接换栈会使旧引用失效。

## B. 技术实现方案

### 1. 物品和配方

- 使用已有 `createsandustry:moonhop_board`。新增 `gravity_mechanism` 和原生 `SequencedAssemblyItem` 类型的 `incomplete_gravity_mechanism`；初始调查未发现这两个物品已被注册。
- `incomplete_gravity_mechanism.png` 和 `gravity_mechanism.png` 均已存在于 `assets/createsandustry/textures/item/`，模型直接引用对应文件，不替换纹理。
- 真实父配方 ID：`createsandustry:sequenced_assembly/gravity_mechanism`，loops = 1，两个步骤。第一步原生 `create:deploying` 安装 `create:precision_mechanism`；第二步独立 `createsandustry:bounce_activation`，由 `StandardProcessingRecipe` + `IAssemblyRecipe` 实现。
- 完成第一步后，由 Create 自己写入 `id/step=1/progress=0.5`，原生 Tooltip 读取自定义步骤的“弹跳激活”描述，进度条仍由 `SequencedAssemblyItem` 渲染。
- 激活前严格检查单件半成品、组件 ID、step、progress 和服务器配方管理器中的父配方结构。确认第一步仍为安装精密构件、第二步为弹跳类型、最终结果仍为单个重力构件。不兼容的数据包覆盖直接拒绝加工。
- 成功判定后才请求 Create 的第二步结果。没有普通机器消费此自定义类型，因此半成品再次经过机械手不能跳过激活。

### 2. 专属物理与最高点

服务端 Pre/Post 不取消原生 Tick。Pre 保存当前位置、速度和地面状态，Post 使用已经验证的空气运动顺序还原撞击前速度，并检查本次真实向下碰撞及粘液块上表面。

```text
FALLING --有效粘液块碰撞--> RISING
RISING --向上速度转为非正，且运动连续--> FALLING + 一次激活
任意状态 --液体/外部运动/非粘液块接触/其他碰撞--> 重置连续流程
```

为排除静止物品在原版重力作用下的微弹，首次或中断后的撞击要求 `impactVy <= -0.08 block/tick`。低于此阈值的非常轻微首次落地不会启动工艺；正常连续反弹没有额外的该速度门槛。

反弹使用 `clamp(max(abs(impactVy) * 0.8, minimumBounceVelocity), minimumBounceVelocity, maximumBounceVelocity)`，默认最小 0.45、最大 2.0 block/tick，并提供仅服务端使用的 COMMON 配置。保留当前水平速度，清除地面标记，设置必要的原生速度同步标记。

最高点用有效反弹上升期的速度正到非正跨越检测，不要求速度精确为零，不比较 Y 坐标相等，不用固定 Tick 定时。触发前先消耗待激活状态，防止同一最高点重入。水平碰撞、头顶碰撞以及未经本工艺发起的击飞不提供激活资格。

Pre 读取的入 Tick 速度并不天然等于撞击前速度，必须扣除实际 `getGravity()`，并排除液体、无物理/无重力、被其他机制重置的位置或速度等不满足推导前提的情况。连续性检查记录期望位置、运动和维度，异常时保守中断，不生成额外次数。

### 3. 两种计数及持久化

| 数据 | 存放位置 | 生命周期 |
| --- | --- | --- |
| `totalBounces` | 持久化且网络同步的 `createsandustry:gravity_total_bounces` ItemStack Data Component | 随半成品保存、拾取、箱子、漏斗、传送带和置物台传递，不因连续过程重置而归零。 |
| `consecutiveBounces`、phase、期望位置/速度/维度 | 可序列化实体 attachment | 同一个实体且轨迹有效时延续；存档恢复后根据已保存运动校验。库存只保存栈，新生成实体从默认连续次数 0 开始。 |
| 本刻 Pre 快照 | 不序列化的临时实体 attachment | 只为配对本次 Pre/Post；加载后不恢复旧 Pre，避免将卸载前快照当作新刻碰撞。 |

累计次数在确认真实反弹时增加；概率判定在对应最高点执行。进水、岩浆、其他液体、落在普通方块、外部重新发射、传送或横向/头顶碰撞会清连续轨迹，不清累计次数。跨维度即使复制了实体 attachment，维度校验也会使流程重置。实体已经移除、栈变更或配方状态不再有效时，Post 不再加工。

半成品最大堆叠数为 1，原生 `ItemEntity#isMergable`（247–249 行）要求 `count < maxStackSize`，因此正常单件半成品不会实体合并。正常拾取、库存和物流继续使用原生行为。

### 4. 概率、产物和服务器权威

概率公式独立为可测试纯函数，随机源可注入：

| 次数 | 连续成功概率 | 累计损坏概率 |
| --- | --- | --- |
| 1 | 20% | 5% |
| 2 | 36% | 10% |
| 3 | 52% | 20% |
| 4 | 68% | 40% |
| 5 | 84% | 80% |
| 6 及以上 | 100% | 100% |

最高点先判成功，成功则结束；只有失败才判损坏。第六次连续弹跳直接成功，不消耗损坏随机数。损坏等概率产生单个 `create:cogwheel`、`createsandustry:moonhop_board` 或 `create:golden_sheet`，否则保留半成品。

成功和损坏均在同一个 `ItemEntity` 上替换栈，保留当前位置与自然运动，清除加工 attachment，产物不携带原生序列组装或累计计数组件。逻辑服务端是唯一随机判定和替换产物的执行侧，使用原生实体栈和速度同步传播给多人客户端。

### 5. 模块职责和未来来源

`GravityMechanismAssembly` 验证/推进真实序列；`GravityBouncePhysics` 管理事件和真实碰撞；`GravityBounceTracker` 管理可保存的运动状态；`GravityActivationLogic` 隔离概率和栈替换；`GravityMechanismData` 管理累计次数；轻量 `GravityActivationSource` 表示来源。

`GravityActivationLogic.resolve(Level, ItemStack, int, GravityActivationSource, RandomSource)` 已提供面向栈和可注入随机源的入口，返回 `ActivationResult`；未来弹射置物台无需伪造 `ItemEntity` 就能复用加工结果。当前只实现 `SLIME_BLOCK`。事件调用链已支持本阶段需求，**本阶段不需要新增 Mixin**，也不需要扫描世界中的全部掉落物。

## C. 实施文件清单

新增源码：

- `src/main/java/com/minechai/create_sandustry/content/gravity/BounceActivationRecipe.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityMechanismAssembly.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityBouncePhysics.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityBounceTracker.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityActivationLogic.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityActivationSource.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityMechanismData.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityAssemblyGameTests.java`
- `src/main/java/com/minechai/create_sandustry/content/gravity/GravityMechanismGameTests.java`
- `src/main/java/com/minechai/create_sandustry/content/compat/jei/GravityBounceAssemblySubCategory.java`

接入现有文件（只增加本工艺所需内容，保留工作区先前改动）：

- `CreateSandustry.java`：注册三个运动/维度事件监听器。
- `config/ModConfigs.java`：最低/最大速度配置。
- `registry/ModItems.java`、`ModCreativeTabs.java`：物品及成品创造栏入口。
- `registry/ModRecipeTypes.java`：自定义步骤类型/序列化器。
- `registry/ModDataComponents.java`、`ModAttachments.java`：累计组件和可保存实体轨迹。
- `src/main/resources/assets/createsandustry/lang/en_us.json`、`zh_cn.json`：两件物品和步骤描述。

新增资源和文档：

- `src/main/resources/data/createsandustry/recipe/sequenced_assembly/gravity_mechanism.json`
- `src/main/resources/assets/createsandustry/models/item/incomplete_gravity_mechanism.json`
- `src/main/resources/assets/createsandustry/models/item/gravity_mechanism.json`
- 本报告 `docs/gravity_mechanism.md`。

复用已有半成品 16×16 PNG、成品 16×256 动画 PNG 及其 16×16 帧 mcmeta，未生成或替换图片。未改已有月跃石板加工逻辑；未增加 Mixin 或修改弹射置物台。

数据组件的持久化/网络同步也与 [NeoForge 1.21.1 官方 Data Components 文档](https://docs.neoforged.net/docs/1.21.1/items/datacomponents/) 一致；最终 API 判断以本地精确版本源码和运行结果为准。

## D. 验证结果

最终执行（2026-11-05，Java 21.0.12.1，项目本地 Gradle 缓存）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:GRADLE_USER_HOME = 'G:\MODS\MC\createsandustry-neoforge-1.21.1\.gradle'
& 'C:\Users\DQQ\.gradle\wrapper\dists\gradle-9.2.1-bin\2t0n5ozlw9xmuyvbp7dnzaxug\gradle-9.2.1\bin\gradle.bat' build runGameTestServer --offline --no-daemon --console=plain
```

结果：**BUILD SUCCESSFUL，73 / 73 required GameTests 全部通过**，其中本次新增 11 项。普通 `test` 任务为 NO-SOURCE；概率测试在已启动的服务端 GameTest 中运行，并非声称执行了 JUnit。

- 完整日志：[build/gravity-verification.log](../build/gravity-verification.log)。
- 分发产物：[build/libs/createsandustry-0.1.7.jar](../build/libs/createsandustry-0.1.7.jar)。
- 已检查 jar 包含新增配方、两件物品模型、对应纹理/动画描述、翻译和物理类。

### 自动化测试与实际服务器行为

`GravityAssemblyGameTests`（3 项）：

| 测试 | 已验证内容 |
| --- | --- |
| `nativeDeployerCreatesIntermediateAndCannotSkipBounce` | 原生机械手两槽配方匹配与结果推进；真实 step=1/progress=.5；原生单件半成品/进度条；再次机械手不能跳步骤；成功由 Create 原生最终推进输出。 |
| `invalidAssemblyStatesAreRejected` | 无组件、错误 ID、错误步骤、非法/NaN 进度、错误物品、超量栈全部拒绝。 |
| `customAssemblyStepSurvivesJsonAndNetworkCodecs` | 整个原生序列配方的 JSON 与网络编解码往返，正确保留自定义 ProcessingRecipe 步骤。 |

`GravityMechanismGameTests`（8 项）：

| 测试 | 已验证内容 |
| --- | --- |
| `gravityProbabilityBoundariesAndSuccessPriority` | 成功/损坏 1–6 次概率、负数/极大计数、上限、成功先于损坏、第六次不抽随机、精确边界。 |
| `gravityWearSurvivesInventoryStackAndCreateLogisticsSave` | 普通容器、物品 NBT、Create TransportedItemStack NBT、实际 DepotBlockEntity 保存恢复保留累计计数及序列组件；计数溢出保护。 |
| `gravitySuccessAndDamageReplaceTheExistingEntity` | 成功/损坏均只替换原实体栈，保持 UUID/位置/速度，单个正确产物，删除加工组件，重复调用不会再次输出。 |
| `gravitySixPhysicalBouncesTriggerExactlySixApices` | `ServerLevel.tickNonPassenger` 驱动真实 ItemEntity、重力、碰撞与 NeoForge 事件。固定种子只控制前五轮概率存活，不修改运动；六次真实接触、六次唯一最高点，第六次成功。每个存活最高点只消耗成功/损坏各一次随机数。 |
| `gravityFlightSaveLoadResumesAndPickupResetsContinuity` | 上升期和最高点后分别执行真实实体 NBT save/load，恢复阶段/连续计数，不重复激活；同栈重新生成实体清连续、保留累计。 |
| `gravityIdleSideAndOrdinaryItemsNeverStartProcessing` | 顶面静止 100 tick（初始地面标记两种值）、侧碰均不加工；普通物品和无合法序列的半成品保持完全相同原版运动且不创建附件。 |
| `gravityExternalMotionFluidsAndStoneInterruptContinuity` | 外力重发射、普通方块落地和进水分别从有效连续状态开始测试，均清连续而保留累计；已移除实体不加工。 |
| `gravityRealHopperPickupAndChestStorageRetainWear` | 世界中的真实 ItemEntity 被真实漏斗收取，传入箱子，再执行实际箱子 NBT 保存恢复，累计次数与原生序列组件保留。 |

### 验证边界与保留风险

- 半成品纹理、成品动画、模型路径与翻译已静态检查；原生进度组件、进度条和客户端配方网络格式已运行验证。尚未人工打开客户端查看 Tooltip/JEI 的实际绘制和运动同步视觉。
- 服务端 GameTest 已实际启动完整专用服务器模组环境。两个玩家客户端联机、完整服务端重启后再连接、真实玩家拾取、跨维度传送和区块管理器真实卸载/加载尚未人工实测。实体/物品 NBT 往返已覆盖其持久化数据路径；不能把它表述为已做人工重启或双客户端验收。
- 本阶段没有新增 Mixin。弹射置物台仅完成源码研究，未来需针对内部飞行队列增加访问入口和一次性最高点状态，再调用现有 `resolve`；目前不支持弹射台激活。
- 首次/中断后速度门槛 0.08 会忽略非常轻微的首次下降，目的是排除原版静止微弹。最低反弹速度默认 0.45，服务器可调整；水平/头顶碰撞采用保守中断策略。
- 首轮 72 项测试中，新增的 10 项全部通过，未修改的 `replacesNormalAndDeepslateSources` 失败；补强测试后最终 73 项全通过，没有改动该矿石测试或世界生成代码。首轮失败位置 X=-11347505（区块局部 X=15），相邻深板岩矿石会跨入下一块，而该测试的处理上下文只扫描前者所在区块，存在位置相关的不稳定隐患。首轮日志：[build/gravity-gametest-first-run.log](../build/gravity-gametest-first-run.log)。
- 工作区原有其他功能改动均保留；本次不修改月跃石板已有加工机制。
