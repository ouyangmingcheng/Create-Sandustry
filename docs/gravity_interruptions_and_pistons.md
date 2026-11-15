# 重力构件：断连损坏率与活塞辅助

验证环境：Minecraft 1.21.1、NeoForge 21.1.251、Create 6.0.10-280、Java 21。

## 概率规则

成功率仍由当前连续有效弹跳次数决定：首次 5%，按原曲线增长，第 15 次必定成功。每次最高点仍先成功、后损坏；成功时不抽损坏或废料。

损坏率统一由 `GravityActivationLogic.damageChance(interruptionCount)` 计算：

| 已断连次数 | 0 | 1 | 2 | 3 | 4 | 5 及以上 |
| --- | --- | --- | --- | --- | --- | --- |
| 条件损坏率 | 5% | 10% | 20% | 40% | 80% | 100% |

同一轮连续弹跳不会增加损坏率。即使断连五次，首次弹跳仍先获得 5% 成功机会，成功失败后才执行 100% 损坏判定。整数超过五次时直接返回 1，避免指数溢出；没有 30% 平衡上限。

旧 `gravity_total_bounces` 组件保留作累计有效弹跳统计和存档兼容，不再用于任何损坏率计算。旧累计次数不会被推测或迁移成断连数，缺少新组件的旧物品按零次断连开始。

绿色/橙色 Tooltip 调用与服务端相同的概率函数。库存中下一轮首次成功率仍显示 5%；橙色损坏率读取新的断连组件，不读取累计弹跳数。成功产物和废料都会清除加工状态及两种历史组件，音效、粒子和三种等概率废料保持原实现。

## 一次性断连与持久化

新增 ItemStack 组件 `createsandustry:gravity_interruption_count`：非负整数 Codec 持久化、VAR_INT 网络同步，最大值安全饱和。它随原生 ItemStack 复制、NBT、库存、容器、Create TransportedItemStack 和多人同步保存。

当前连续次数仍保存在实体的 `GRAVITY_BOUNCE` 附件，沿用原来的序列化轨迹、半格起跳高度和阶段。只有该附件的连续次数大于零、物品仍为真正等待激活的半成品时，结束流程才会增加一次断连数。

`GravityChainLifecycle.interrupt` 是唯一常规提交入口：复制当前栈，增加断连数，使用 `ItemEntity#setItem` 同步，再清除当前连续流程。同一中断的后续 Tick 或事件看到的连续次数已为零，不会再次收费。尚未达到第一次半格有效弹跳的物品不会增加断连数。

自然反弹低于半格、非弹性落地、流体、真正破坏轨迹的未识别重新发射等调用此入口。正常上升、下降、重力最高点以及不同粘液块之间的连续落地不调用它。半格以下的剩余物理弹跳继续自然衰减，不会生成新的加工判定。

不监听实体移除或区块卸载来增加断连数。实体附件和物品组件直接保存/恢复，正常卸载和服务器存档不会收费。

## 在最终库存栈形成前提交

玩家、漏斗和 Create 收取钩子围住实际转移方法：

1. 仅对已有有效活动链的半成品，临时把增加一次断连数的副本设为实体当前栈。
2. 调用原来的完整收取逻辑，使库存、漏斗、传送带、置物台、漏斗口或溜槽复制的是已更新的栈。
3. 收取成功后清除源轨迹；收取失败、库存满或过滤拒绝时，恢复原栈和活动链。

同一实体的嵌套收取调用有事务标记，不会再次增加。溜槽向上吸取在原生接受检查之后、已经复制的栈被写入内部库存之前同步新组件。没有依赖实体销毁后的写入。

实体 `changeDimension` 同样在真正完成转移时提交，并清除目标实体复制到的活动轨迹，避免到达后的第一 Tick 重复断连；转移失败会回滚。取消或未完成的收取不会提前改变最终损坏率。

## 移动粘液块的真实原生行为

核查精确版本本地源码：

- `PistonMovingBlockEntity#moveCollidedEntities`：先以移动包围盒查询实体；对于移动粘液块，原生先沿活塞轴修改速度，向上时给非玩家实体 `Vy = 1`；随后才逐个扫描实际前缘碰撞形状。
- 实际前缘有正交叠距离 `d4 > 0` 时，才调用 `moveEntityByPiston`，执行 `Entity#move(MoverType.PISTON, ...)`。
- 因此“被原生设置了速度”和“实际发生了新的推碰”不是同一个事件。原生还限制每个游戏 Tick 的活塞位移累计为 0.51，这也不能被当作新的反弹速度。

新钩子只观察这些真实调用，不重写活塞碰撞、不自行扫描附近活塞，也不设置任何活塞速度：

- 合法原生粘液块改速更新预期轨迹，延续原链；它不会因为偏离上一 Tick 的自由飞行预测而被判为断连。
- 实际向上前缘推碰且确有向上位移时，记录一个新起跳原点。随后仍需自由上升达到原有半格门槛，才增加一次连续次数。
- 仅被加速但没有实际推碰时，保留原起跳和连续次数，不创建新反弹。
- 下一次最高点使用更新后的原生运动判断，活塞速度跳变本身不触发概率判定。
- 活塞被天花板真正阻断等实际终止仍可结束链；已经自然结束的链不会被补能恢复旧连续次数。
- 原生赋予的 `Vy = 1` 及 X/Z 不经普通粘液块的 `0.8` 公式或普通反弹安全上限覆盖。后续仍由 ItemEntity 施加原版重力和阻力。

每次活塞运动在该 PistonMovingBlockEntity 的持久化 `NeoForgeData` 内分配 UUID。实体轨迹持久化最近运动 UUID，按运动而不是按 Tick 去重，跨实体/方块实体读档也不重置同一次运动的起跳点。记录有 64 条边界，正常第 15 次必成的加工链不会耗尽该范围。

## JEI 与物理范围

序列组装 JSON、resultPool、机械手步骤、粘液块动画、原生随机废料槽布局与样式均未改变，没有新增 Category 或第二套产物抽奖。

JEI 现有显示概率的计算入口改用新规则：首次加工、不中断直到产生结果时，条件损坏率每次恒为 5%，理论总成功率为 83.0662379876%，总废料率为 16.9337620124%，原生槽悬停按整数显示约 17%。这取代了旧损坏规则下的 43%，不是单次损坏率，也不影响实际执行。

普通粘液块的 0.8 弹性、重力、0.98 阻力、0.08 微小撞击过滤及 0.5 格有效计数门槛保持原值。回归数据仍为 8 格静止释放后六次物理反弹、前三次加工计数，第 91 Tick 停止。

## 修改文件

| 范围 | 文件 |
| --- | --- |
| 新组件与统计 | `registry/ModDataComponents.java`、`content/gravity/GravityMechanismData.java` |
| 共享概率、Tooltip、JEI 概率 | `GravityActivationLogic.java`、`GravityTooltipEvents.java`、`GravityAssemblyJeiChance.java` |
| 断连与活动轨迹 | 新增 `GravityChainLifecycle.java`；修改 `GravityBounceTracker.java`、`GravityBouncePhysics.java` |
| 活塞观察 | 新增 `GravityPistonMotion.java`、`mixin/PistonGravityMotionMixin.java` |
| 复制前收取 | 新增 `ItemEntityGravityCollectionMixin`、`HopperGravityCollectionMixin`、`CreateGravityBeltCollectionMixin`（传送带及漏斗口）、`CreateGravityDepotCollectionMixin`、`CreateGravityChuteCollectionMixin`、`CreateGravityChuteSuctionMixin` |
| 注册 | `CreateSandustry.java`、`resources/createsandustry.mixins.json` |
| 测试 | 新增 `GravityInterruptionGameTests.java`、`GravityPistonGameTests.java`；更新 `GravityMechanismGameTests.java`、`GravityAssemblyGameTests.java` |

Java 路径均位于 `src/main/java/com/minechai/create_sandustry/`；未列目录的重力文件位于其 `content/gravity/` 内。

## 测试与构建

`build runGameTestServer --offline --no-daemon --console=plain` 使用本机 Gradle 9.2.1 执行，构建成功，全部 **95 项 GameTests** 通过，没有跳过构建或测试任务。日志：`build/gravity-interruption-verification.log`。

新增与更新验证包括：

- 连续十次判定保持同一损坏率；五轮真实自然衰减停止，损坏率依次 5/10/20/40/80/100%；大量静止 Tick 和重复中断调用不重复增长。
- 100% 条件损坏率仍先检查成功，第 15 次仍无须随机数而必成。
- 真实玩家拾取、满库存回滚、从未弹跳的拾取，以及反复拾取/丢弃不重复增加。
- 真实漏斗收取及拒绝、Create 置物台/传送带/溜槽落入及吸入，检查目标内部最终栈；传送带取出重放不会再次增长。
- 实体卸载原因、NBT 读写、继续到最高点、ItemStack 网络编解码、Create 物流序列化及完成产物清理。
- 活塞真实碰撞后的原生 Vy=1、后续 Vy=0.9408；原生加速但无实际推碰；同一运动跨存档去重；自然停止后活塞重新起跳从 1 开始；跨不同位置粘液块保持连续。
- 活塞辅助长时 GameTest 使用真实服务端 Tick、原生方块实体运动和生产概率调用，只固定随机数以免提前产出。实测日志：`GRAVITY_PISTON_CHAIN,consecutive=10,piston_launches=9,interruptions=0,damage=0.05`。

需要观察完整自然停止的状态测试使用每个测试局部的最高点回调，避免随机提前完成；正式玩法没有新增概率禁用配置或全局调试开关。
