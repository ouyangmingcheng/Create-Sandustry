# 重力构件增量完善

JEI 当前实现以 [原生随机废料槽修正记录](gravity_native_jei.md) 为准：已删除额外专用页，恢复仅在 Create 原有页面显示 `[重力构件] [?]`。

本次基线仍为 Minecraft 1.21.1 / NeoForge 21.1.251 / Create 6.0.10-280 / JEI 19.39.0.371。

## 调查与改动

- `GravityActivationLogic` 是唯一概率计算入口：成功 `0.05 + 0.95 * (n-1)/14`，15及以上直接1；损坏为5/10/20/30%，4次及以上封顶30%。先成功再损坏。`GravityBounceTracker` 的连续次数内存和Codec上限同步改为15，旧0—6存档仍可读取。碰撞检测与速度参数未改。
- 中英文半成品显示名更新，ID、模型、PNG和配方保持原有路径。
- Create `foundation/events/ClientEvents.java:271` 的默认优先级监听器调用 `SequencedAssemblyRecipe.addToTooltip`。新增 `GravityTooltipEvents` 使用LOWEST，在真实物品/配方/步骤验证后识别原生 `create.recipe.assembly.next` 及其弹跳描述，插入绿色成功与金色损坏文本。有效配方本来仅剩一个步骤，因此没有后续箭头步骤需要删除；不按行数截断，不删除其他模组内容。库存UI不查询实体连续次数：下一次成功固定从连续第1次开始，损坏读取累计次数+1，整数上限安全处理。格式统一两位小数。
- `activate` 仅在服务端确定DAMAGE后，复制原栈纹理、广播ITEM_BREAK（NEUTRAL/0.8/1.0），发送15个ITEM碎片，带少量向上及随机水平速度；就地替换产物并清理实体轨迹。成功和普通反弹不触发反馈。栈级resolve继续可复用。
- `GravityBounceAssemblySubCategory` 围绕粘液块图标中心(18,69)放大1.10，半成品动画不缩放。
- JEI 修正：删除 `GravityAssemblyCategory` 和 `SandustryJeiPlugin` 的重复注册，只保留原生序列组装页面。客户端定向兼容原生 `?` 槽，展示全新半成品无中断加工的理论废料率约43.09%，不修改实际resultPool。详见上述修正记录。

## 修改文件

`content/gravity/GravityActivationLogic.java`、`GravityBounceTracker.java`、新增`GravityTooltipEvents.java`、`GravityMechanismGameTests.java`、`GravityAssemblyGameTests.java`；`content/compat/jei/GravityBounceAssemblySubCategory.java`、新增`GravityAssemblyCategory.java`、`SandustryJeiPlugin.java`；`assets/createsandustry/lang/en_us.json`和`zh_cn.json`；本记录及原调查报告入口说明。

## 验证

`build runGameTestServer --offline --no-daemon --console=plain` 成功，74项服务端GameTest全部通过。覆盖15次真实反弹/最高点、每次判定随机消耗次数、概率边界及上限、成功优先、产物及存档物流，新增Tooltip结构/颜色/5%和累计三次后30%/去重/保留其他文本检查。日志位于`build/gravity-refinement-verification.log`。

新反馈使用原生服务端声音和粒子广播API，客户端最终视听、JEI布局/悬停/来源查询与两客户端广播尚未人工验收，不能将服务端测试表述为已验证这些客户端效果。没有修改弹跳物理参数。
