# 月跃石块：实体无限弹跳与 Sable 物理结构兼容

## 实际环境与保留内容

- Minecraft 1.21.1、NeoForge 21.1.251、Create 6.0.10-280、Aeronautics 1.3.2、Sable 2.0.6（内嵌 Companion 1.6.0）。
- MOD_ID 为 `createsandustry`；既有方块 ID 为 `createsandustry:moonhop_block`，仍由 `registry/ModBlocks.java` 注册。
- 本次没有重写 MoonhopBlock，也没有改动其注册、纹理、模型、配方、战利品、音效、摩擦、硬度或双向不黏附行为。
- 普通世界仍使用 MoonhopBlock 的原版 1.21.1 黏液块回调和 `MINIMUM_BOUNCE_VELOCITY = 0.42D`。生物反弹系数 1.0、其他实体 0.8；潜行停止。物品保持原版重力、阻力、拾取和 6000 tick 寿命。

## 源码调查与根因

结论基于 Gradle 实际解析的 Sable 2.0.6 JAR 及内嵌 Companion 1.6.0，而非假设 GitHub main 等同于安装版本。调查材料位于 `build/moonhop-sable-reference`（构建目录，不发布）。源码参考：[Sable](https://github.com/ryanhcode/sable)、[Companion API](https://github.com/ryanhcode/sable-companion)。

1. `sublevel/entity_collision/SubLevelEntityCollision#collide` 的开头对 ServerPlayer 直接返回简化结果，不执行后面的 SAT 方块碰撞与 firstCollisions 收集。其他实体才进入完整求解。求解内部区分 `abs(normal.dot(entityUp)) > 0.6` 的垂直碰撞，并读取 `collidedBlockState.is(SableTags.BOUNCY)`。
2. `mixin/entity/entity_sublevel_collision/LivingEntityMixin#sable$beforeAnimation` 只对 `collisionInfo.horizontal()` 且结构不同于 `preTrackingSubLevel` 的记录调用 `sable$computeCollisionEffects`。该特殊分支不处理顶部落地，并且只属于 LivingEntity，不能补齐物品弹跳。
3. `mixin/entity/entity_sublevel_collision/EntityMixin#sable$collideRedirect` 将 Sable 求解与原版 move 接合；其 updateEntityAfterFallOn 包装仍依赖垂直碰撞。`entities_stick_sublevels/effects/EntityMixin#sable$preGetOnPos` 还可能把支撑位置转换为 plot 局部坐标。原 MoonhopBlock 的顶面判断把实体世界包围盒 minY 与支撑方块局部 Y 比较，坐标空间不一致；倾斜顶部甚至会被 Sable 归为水平碰撞。
4. Companion 提供相交结构查询、姿态变换和接触点速度，但 1.6.0 没有实体碰撞事件、实际块接触法线或求解器回调。因此仅公开 API 或仅加标签不足以修复；需要两个受条件控制的兼容 Mixin。

## 修复流程

`MoonhopSubLevelCollisionMixin` 在 Sable 已通过 SAT 求解、排除内部遮挡面之后读取实际 SubLevel、BlockState、BlockPos、插值姿态、MTV 和实体 OBB。只有 MoonhopBlock 的局部 +Y 顶面、实体位于顶面外侧且相对法线速度向内的接触才进入反弹流程。

- 用碰撞姿态将局部顶面 +Y 转到世界法线；用实体 OBB 朝向表面的支撑点确定接触位置，而不是用实体中心代替角速度的力臂。
- 用 Companion `getVelocity(level, surface, localPoint, dest)` 获取包含角速度的表面速度，乘 0.05 转换为格/tick。
- 捕获碰撞前的移动速度，加上 Sable 为生物单独维护的 inheritedVelocity 得到世界入射速度。
- `vn = (worldIncoming - surfaceVelocity).dot(normal)`；`out = max(-vn * factor, 0.42)`；`worldOutgoing = worldIncoming + normal * (out - vn)`。这样保留切向速度，结构速度不会被重复累加。
- `MoonhopEntityMoveMixin` 在完整 move 返回后施加一次冲量，避免原版落地回调覆盖它。一个 move 的多个 SAT 子步只选择一个接触。每实体接触锁通过实际局部分离解除，无固定长冷却；换结构可重新碰撞。结构引用为弱引用，不保留已拆除结构的方块数据。
- 起跳时只保留一次已经产生的结构位移，清空 Sable 的独立运动继承并解除 tracking。只过滤月跃石块对应的 Sable 水平标签弹跳记录，避免再叠加 0.65 冲量。其他块和黏液块记录保持原样。
- ServerPlayer 仅在附近扫掠范围实际包含月跃石块时进入完整 Sable 求解；对已被客户端裁剪到恰好贴面的位置进行 0.00001 格碰撞探测，原始入射速度不变。服务器设置原版速度同步标记；客户端运行相同碰撞预测。
- 实际月跃石块的落地状态被交给原版摔落回调，保留正常免伤和潜行摔落伤害。液体、无重力、乘骑、穿墙、飞行与旁观状态不会获得专用最低冲量；世界障碍与附近 SubLevel 障碍/液体均检查上升空间。

没有新增全局实体 Tick 扫描，也没有改写 Sable 源码或实体空中物理。

## 重力构件集成

兼容层调用 `GravityBouncePhysics.acceptPhysicalMoonhopBounce` 通知现有观察器，后者不会施加第二次反弹。物理顶面即使被 Sable 标为 horizontalCollision，也不会误判为断连。Tracker 记录本次飞行法线，以该方向的半格上升确认计数，并在法线速度越过顶点时继续原有加工流程。

计数上限、半格门槛、断连/拾取规则及成功和损坏概率不变。原世界的速度上限处理不改动；物理结构的绝对平移速度不会被错误地当作反弹额外能量再次加入或裁剪。新飞行法线有兼容旧存档的默认值。

## 数据包、可选依赖与日志

新增 `src/main/resources/data/sable/tags/block/bouncy.json`，使用 `replace: false` 合并 `createsandustry:moonhop_block`。

保留 `src/main/resources/data/createsandustry/physics_block_properties/moonhop_block.json`：

```json
{
  "selector": "createsandustry:moonhop_block",
  "priority": 1001,
  "properties": { "sable:restitution": 0.8 }
}
```

Sable 2.0.6 的标签预设恢复系数为 0.5、默认 priority 为 1000。`PhysicsBlockPropertiesDefinitionLoader` 按优先级升序应用，因此 1001 的专用值最后覆盖为 0.8；GameTest 同时断言标签加载和最终数值，原版黏液块仍为 0.5。结构碰撞恢复系数与 Minecraft 实体的 0.42 托底互相独立。

本次增加 Sable 与 Companion 的 `compileOnly` API，不打包它们，也不声明强制运行前置。`MoonhopSableMixinPlugin` 在不加载 Sable 类的情况下检查目标字节码，缺失时禁用两个 Mixin。配置为 `createsandustry.sable.mixins.json`，在 neoforge.mods.toml 注册。已有航空学兼容配置不受影响。

日志默认关闭。启动 JVM 添加 `-Dcreatesandustry.debugMoonhopSable=true` 后，`[Moonhop/Sable]` 会记录结构 UUID、局部块坐标、实体、tick、法线、相对入射速度、表面格/tick 速度、出射速度，以及侧面、持续接触、向外接触、潜行/液体/阻挡等拒绝原因。无需改航空学 UI；原有 Simulated 属性提示由 0.8 数据驱动，前一轮客户端探针得到过 `Bouncy (80%)`，本次没有重新人工检查提示。

## 验证与可复现命令

- 安装 Aeronautics + Sable：`gradlew.bat runGameTestServer build --offline --console=plain`；108 项必需测试通过并构建成功，最终记录见 `build/moonhop-sable-reference/final-build.log`。
- 排除这两项运行依赖，在独立目录启动 GameTest：`gradlew.bat -I build/moonhop-sable-reference/without-sable.gradle runGameTestServer --offline --console=plain`；108 项必需测试通过，最终记录见 `build/moonhop-sable-reference/without-sable-final.log`。Sable 专属四项测试在该环境按缺失前置跳过其内部场景；原世界测试实际执行。
- 无 Sable 整套测试曾暴露既有矿石测试的随机失败：两个样本跨区块，而测试只扫描第一个区块。仅修正 `SandifiedOreRuleGameTests` 的样本位置，使两者总在同一区块；矿石生成代码不变。首次记录保留为 `without-sable-first.log`。
- 48 组真实 SubLevel SAT 顶面碰撞：0/25/65 度倾斜，静止/水平/上升/下降速度，非水平面还设置角速度；覆盖普通物品、牛、模拟玩家和 ServerPlayer。断言相对法线反射、切向保留、结构运动继承清理及服务端同步标记。
- 静止结构上的 1200 tick 真实实体轨迹：物品反弹 63 次，牛和模拟玩家各 117 次。普通世界的既有持续弹跳、衰减到最低值、物品寿命、声音/属性与活塞测试继续执行。
- 四种逐 tick 改变真实 SubLevel 姿态并设置原生刚体速度的场景：水平平移、上升、下降、旋转。普通物品、牛、模拟玩家和未激活构件各验证至少五次连续反弹，无速度累积或构件误断连。旋转/倾斜导致实体实际离开平台后不要求继续反弹。
- 物理测试使用固定、预先加载区块的场地，避免 GameTest 随机远坐标和原生刚体场地加载状态干扰手动 tick 轨迹。早期随机场地曾有一次移动牛未接触平台，开启日志的复核全部通过；最终夹具不依赖随机模板位置。
- 未激活构件在静止物理结构上验证 40 次碰撞、40 次顶点、40 次总计数，断连 0；测试仅以局部回调抑制随机加工结局以观察长轨迹，生产随机结局逻辑不变。
- 覆盖潜行停止、正常/潜行摔落伤害、物理侧面/底面、继承速度只转移一次、主世界/物理障碍与液体、普通石头与原版黏液块排除，以及不同结构间切换和离开结构后的冲量停止。

GameTest 中的玩家、ServerPlayer 和结构运动夹具不能代替真人联机测试。本次没有人工驾驶航空学结构或验证高延迟客户端手感，也没有重新测试两个物理结构之间的实碰轨迹；这些项目不记为通过。

## 本次文件

- 新增 Java：`content/compat/sable/SableMoonhopBounce.java`、`MoonhopSableMixinPlugin.java`，`mixin/sable/MoonhopEntityMoveMixin.java`、`MoonhopSubLevelCollisionMixin.java`，`content/gravity/SableMoonhopGameTests.java`。
- 修改 Java：`content/gravity/GravityBouncePhysics.java`、`GravityBounceTracker.java`、`MoonhopBounceGameTests.java`。
- 修正既有测试夹具：`worldgen/geology/rules/SandifiedOreRuleGameTests.java`，避免随机测试原点导致跨区块失败。
- 新增资源：`createsandustry.sable.mixins.json`、`data/sable/tags/block/bouncy.json`。
- 修改构建/元数据：`build.gradle`、`src/main/templates/META-INF/neoforge.mods.toml`。
- 更新本说明；保留 0.8 的专用 JSON，无需修改 MoonhopBlock 或原有模型资源。
