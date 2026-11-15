# 重力构件：恢复原生 JEI 随机废料槽

## 本次改动

删除 `content/compat/jei/GravityAssemblyCategory.java`。从 `SandustryJeiPlugin` 删除 gravityCategory 字段、额外 Category 创建/注册、重复 registerRecipes 调用。不再注册 `createsandustry:gravity_assembly`，不再添加三个独立废料槽。Create 原本的序列组装分类与配方注册保持原样；机械手步骤、弹跳步骤及动画没有修改。

增加 `content/gravity/GravityAssemblyJeiChance.java` 计算仅供展示的完整连续加工概率；增加仅客户端的 `mixin/SequencedAssemblyGravityJeiMixin.java`，在 `createsandustry.mixins.json` 的 client 列表注册。删除额外 Category 及自定义悬停说明的闲置文本。为 `GravityAssemblyGameTests` 增加概率/真实结果池隔离回归测试。

## 源码依据与兼容方式

已逐行核对本地源码与精确 Create `6.0.10-280` sources.jar，一致：

- `output/create-src/com/simibubi/create/compat/jei/category/SequencedAssemblyCategory.java`：`setRecipe` 第50、59、65行根据 `getOutputChance` 设置主要产物布局、概率槽背景和延迟 Tooltip；`draw` 第118行决定是否绘制 `JEI_CHANCE_SLOT` 与问号；`getTooltipStrings` 第169、178行决定是否使用原生问号命中区与废料概率说明。
- `output/create-src/com/simibubi/create/content/processing/sequenced/SequencedAssemblyRecipe.java`：`advance` 第113行在最后一步调用 `rollResult`，第132行后的 `rollResult` 根据 resultPool 权重抽取真实产物；`getOutputChance` 第189行返回首个结果权重/总权重。因此直接往真实池添加废料会产生第二次随机产物选择，不能用于本工艺的纯展示。

兼容补丁只重定向上述原生 Category 内的6处概率读取（含主要产物延迟 Tooltip）。只有当前客户端配方管理器中真实 ID `createsandustry:sequenced_assembly/gravity_mechanism` 对应的同一配方实例，并且仍有正确半成品、两个步骤和单一成品池时，才返回展示概率。其他配方直接调用原生 getOutputChance。

这样原有 Category 自己进入 `singleOutput == false` 分支，输出 `[重力构件] [?]`。纹理、坐标偏移、原生问号、143≤X<161且90≤Y<108的悬停区、原生“随机废料”标题和概率样式均来自 Create，补丁不自行绘制槽位，也不增加页面。按最新显示要求，悬停只保留原生“随机废料”和43%概率，不再附加小数概率、理论定义或三选一说明；主要产物也不额外附加文字。

## 概率含义

令 R₁=1，表示全新半成品从第一次弹跳开始、不中断且未产出结果的概率。每次 n：

- 成功贡献：Rₙ × sₙ。
- 废料贡献：Rₙ × (1−sₙ) × dₙ。
- 下一次存活概率：Rₙ₊₁ = Rₙ × (1−sₙ) × (1−dₙ)。

sₙ 和 dₙ 直接调用现有 GravityActivationLogic 的概率函数；第15次成功率为1，剩余概率归零。求和得到：成品56.90701346698345%，随机废料43.09298653301655%。原生槽 Tooltip 按最新要求只使用标准整数显示43%，完整概率定义保留在本记录中。30%仍仅为每次成功失败后的条件损坏概率上限。已累计损耗或中途收取后重新抛出的实际全程概率不使用此初始工况统计。

实际配方文件和 resultPool 保持仅一个权重1的重力构件；`SequencedAssemblyRecipe#getOutputChance` 本身没有被修改，真实值仍为1。服务端成功/损坏只由现有弹跳最高点判定决定。成功后虽仍调用原生最终推进，但单结果池只能返回重力构件，没有增加废料抽奖；损坏时仍由现有逻辑从原三种废料等概率选择。

## 验证记录

- Gradle `build runGameTestServer -x createMinecraftArtifacts --offline --no-daemon --console=plain` 成功，75项required GameTests全部通过。新增测试校验总概率、只影响本配方、展示调用前后真实池/权重不变、成功推进仍只生成重力构件。日志：`build/gravity-native-jei-verification.log`。
- 因当前运行的游戏锁定了NeoForge依赖jar，复用了现有依赖并跳过该文件替换。已逐条字节比较NeoForge原jar和新生成临时jar的所有内部条目，内容完全一致。
- 核对本次前后源码散列：激活概率、产物替换、碰撞/反弹、轨迹数据、Tooltip和配方JSON均未改变。动画源码也没有修改。
- 客户端补丁通过隔离目录的自动启动探针验证目标原生类成功加载，六处必须匹配的重定向均被接受。探针仅在临时Gradle init脚本下编译，最终分发包不包含探针。最终探针运行正常退出，日志：`build/gravity-native-jei-client-smoke.log`。
- 本次未操作正在运行的玩家世界，也未重启其客户端；实际游戏内搜索结果、绘制与悬停视觉尚未人工验收。

最终标准源码构建日志：`build/gravity-native-jei-final-build.log`。已检查jar不含已删除Category和临时探针，保留原生弹跳动画类、客户端补丁及单一真实结果池。

分发产物：`build/libs/createsandustry-0.1.7.jar`。本补丁针对已核验的Create 6.0.10调用点，升级Create时需要重新核查这些JEI显示方法。
