# 重力压制器增量改进记录

首轮结果（2026-11-07）：阶段0～8的代码实现、构建和可自动执行的回归已完成，165项required GameTest通过。2026-11-11按新要求调整能耗、显示、教程和加工时间，并新增Jade适配，当前完整套件167项通过（压制器77项）。没有Git提交。阶段0～8保留当时的历史数值与验证记录，当前规则以本节及下面的更新记录为准。

## 当前功能与保留数值

| 项目 | 当前行为 |
|---|---|
| 注册与环境 | `createsandustry:kinetic_press`；Minecraft 1.21.1 / NeoForge 21.1.251 / Create 6.0.10-280 / Java 21 |
| 工作高度 | 3～23格，默认8格；保留Create原生侧面数值设置、手持溜槽快速搭建、调高时移动绑定溜槽 |
| 能源 | 3000 FE容量，仅水平四面输入（顶面、底面和无方向查询均不暴露FE能力）；实际费用=基础FE+缺少高度×服务端配置额外FE（默认200/格），一次启动只扣一次 |
| 批次 | 16槽各1件；满16件立即锁批，否则最后一次输入后等待10tick；最多16个独立并行任务 |
| 概率 | 开工前准备并保存确定结果；`count=4,chance=0.25`仍是4次独立判定；最大可能产量用于容量预留 |
| 输出 | 9槽缓存，向下容器/Create物流/空气掉落；拒收时保留余量，失败10tick后重试；轮询允许其他可收产物先输出 |
| 暂停恢复 | 红石暂停输入、任务推进、自动输出及外部主动抽取，仍可充FE和手动退料；结构异常暂停任务，复原后续做 |
| 显示 | 保留指数系数7的下落动画；整柱渲染范围判断，4tick进度校正、关键变化紧急同步；保留普通溜槽物品渲染 |
| 原压制配方 | 1焦化残渣→1休眠种子（100%）+1金粒（25%），20tick，要求高度8；基础200 FE（按新要求减为原来的1/10） |
| 显示与教程 | 护目镜精简且四空格缩进；Jade显示服务端电量/高度/状态/批次；JEI不显示常驻说明，鼠标悬停模型时显示高度/能耗/时间及“缺少高度增加能耗”；Ponder为3～4格的紧凑演示，无具体FE数值说明，无最后退料剧情 |

## 2026-11-11：Jade适配、说明精简与20tick配方更新

- 新增可选的 `SandustryJadePlugin` 与 `KineticPressJadeProvider`，由Jade的 `@WailaPlugin` 扫描注册，不从主模组入口引用Jade类。复用项目已有Jade 15.10.6编译/本地运行依赖，无新增强制依赖。
- Jade用独立的服务端状态数据提供当前电量/容量、设定高度、运行状态，存在批次时显示完成数；各观察方向均可显示，不依赖被观察面的FE能力。不发送库存及活动任务产物快照。通过显示优先级替换通用能源栏，避免重复显示电量；中英配置名称同步添加。
- JEI模型悬停保留配方高度、基础FE和加工时间三项数据，机制介绍只保留一句“缺少高度增加能耗。”，移除详细加价和公式显示。常驻界面文字仍为空。
- 删除Ponder末尾潜行右键取回缓冲原料的剧情及对应第9段中英文本，当前共8段，结束于红石恢复加工；实际退料交互保持可用。
- 焦化残渣真实配方 `processing_time` 由40改为20tick；同步Ponder下落演示时长，以及真实配方和同步测试的时长断言。高度8、基础200 FE与产物概率保留。
- 新增1项实际Jade GameTest，覆盖六个观察方向、服务端快照显示、缺电等待、红石暂停与继续充电。首轮166项通过、新测试失败：测试在填入服务端数据前创建Accessor，而Jade构造时复制NBT；改为按实际接收流程构建客户端Accessor后通过，未放宽业务断言。
- 最终命令 `gradlew.bat build runGameTestServer --offline --console=plain`：BUILD SUCCESSFUL，30秒，167项required全部通过（压制器77项、其他90项）。同步测试的20tick任务、40tick观察期共7次刷新请求。完整日志：`build/kinetic-press-jade-20261111-recheck.log`；首轮日志保留在 `build/kinetic-press-jade-20261111.log`。
- 打包检查通过：JAR内配方20tick/200 FE/高度8、两种语言和Ponder NBT均与源码一致；8段教程英文文本顺序匹配，无第9段；三个Jade类已打包，未内嵌Jade本体类。产物 `build/libs/createsandustry-0.1.9.jar`，4634061字节，SHA-256：`ca3a603f684a84fd06773d5fd5b9617e9b9f0273eecf219f60dc34d05c927956`。
- 本轮未进行客户端画面验收；Jade/JEI/思索的最终视觉效果不计为已验证。

## 2026-11-11：能耗、界面与紧凑教程更新

- FE缓存容量30000→3000，残渣配方基础耗电2000→200，默认每缺一格加价2000→200；高度3实际1200 FE，高度8及以上200 FE。16件批次、9槽物品缓存、加工时间和概率保持原样。
- `ModCapabilities`按方向限制能源接口：北/南/东/西输入，UP/DOWN/null均返回null；内部服务端任务仍用原能源存储对象。
- 护目镜参考弗洛林电池的白标题、灰说明、青色电量以及统一四空格缩进，正常只显示电量、高度、状态；存在批次时加完成数，结构异常时加具体原因/坐标，移除重复结构/实际高度/各类计数/ETA/能源明细。
- JEI页面177×100→177×80；常驻文字全部移除。只在投影模型区域显示完整信息悬停提示；输入、输出和空白区域不触发机器说明，产物自身的概率悬停提示保留。
- Ponder由13段缩至9段；用3格塔演示搭建和下落，用3→4→3演示移动。模板由5×12×5缩为5×8×5，镜头缩放0.75、纵向偏移-0.5，最高溜槽y6，避免原y10高塔越出画面。删除全部具体FE消耗和容量数字，改为水平供电及定性耗电说明。
- 对应中英文本同步更新；新增可重复生成模板的 `tools/generate_kinetic_press_ponder_structure.py`，复用项目已有NBT写入助手。
- 测试夹具的能源输入、成本及精确期望统一按比例调整，仍保留16件完整并行、混合调度、缺电/堵输出/恢复等断言。新增水平能源能力、模拟充电、3000容量、旧档容量上限回归；旧单任务迁移仍特意使用旧12000 FE快照，确认已付任务不会重新定价或扣电。Ponder资源测试改为精确验证新的紧凑模板尺寸。
- `gradlew.bat build runGameTestServer --offline --console=plain`：BUILD SUCCESSFUL，36秒；166项required GameTest全部通过，其中压制器76项。日志：`build/kinetic-press-rebalance-20261111.log`。
- 旧存档剩余电量按现有读取逻辑限制到新3000容量；已开工任务的原料、费用和确定产物快照保持原样。外部已有服务器配置不会被代码静默覆盖，沿用旧配置时应将 `kinetic_press.extra_energy_per_missing_height` 改为200，或按自定义比例调整；本工作区run与run-gametest的旧默认2000已改为200，其他配置保留。
- 最终JAR打包检查通过：基础耗电200、原40tick/高度8、紧凑NBT和中英文9段教程均与源码一致，教程不含具体FE数字说明；脚本文字顺序与英文语言键一致。当前 `build/libs/createsandustry-0.1.9.jar` 为4625159字节，SHA-256：`bc85b9da15daca37e31f177b47a283fa689f783d8ae94e123003d3212fb34568`。
- 已用 `runClient --offline` 启动开发客户端；Computer Use获取Java/Minecraft窗口时被自动审批拒绝，返回 `Computer Use was not approved to use java`。未尝试绕过该限制，因此JEI命中区域、缩短后的Ponder取景及护目镜排版仍需实机确认，不计为已通过。


## 阶段 0：源码与测试基线（2026-11-07）

环境：Minecraft 1.21.1，NeoForge 21.1.251，Create 6.0.10-280，Java 21。
以任务开始时的工作区为基础，保留全部已有修改，不提交 Git。

### 数据流及所有权

```mermaid
flowchart LR
    U[上游溜槽或顶部 Capability] --> I[InputHandler：16 槽，每槽 1 件]
    I --> B[满 16 件或最后插入后 10 tick 锁定批次]
    B --> C[查询配方、能耗及最大输出容量]
    C --> E[一次性扣 FE]
    E --> T[ActiveTask 接管原料并预先确定 rolledResults]
    T --> P[独立进度推进]
    P --> Q[pendingOutputs 接管完成产物]
    Q --> O[OutputHandler：9 槽]
    O --> D[下方容器、Create 物流接口或掉落实体]
```

- 投入前物品归上游；真实插入每次转移一件，模拟插入不得转移所有权。
- 未开工物品归 `KineticPressInputHandler.slots`；等待电力和输出容量没有独立任务对象。
- 开工时输入槽清空，`activeTasks[].inputItem` 持有原料恢复来源；`rolledResults` 是预先确定的潜在产物，尚不可同时返还。
- 任务完成移出活动列表，结果转移到 `pendingOutputs`，继而进入输出缓存；不得再返还任务原料。
- 输出缓存扣除已接受数量，未接受部分返回缓存。空气输出临时归 `inFlightDrop`，成功生成实体后清空。
- 概率按 `count` 次独立抽样，正常保存和重载使用已存结果，不重新抽样。

### 原有存档字段

- 基座：`workingHeight`、`energy`、`structureValid`、`structureFailure`、`BoundInput`、`BoundInputType`。
- 处理器：`InputItems`、`OutputItems`、`InputIdleTicks`、`BatchLocked`、`NextTaskId`、`ActiveTasks`、`PendingOutputs`。
- 每项活动任务：`Id`、`Item`、`Recipe`、`Progress`、`Total`、`WorkingHeight`、`EnergyCost`、`RequiredHeight`、`BaseEnergy`、`RolledResults`、`MaximumOutputs`。
- 自动输出：`InFlightDropItem`、`InFlightDropId`。
- 客户端另有 `Animations`（任务编号、物品、进度、时长、高度）；初始版本不同步绑定位置、输入数量和批次计数。
- 旧单任务存档：不存在 `ActiveTasks` 且 `Processing=true` 时，由 `CurrentItem`、`ProcessingProgress`、`ProcessingTotalTime`、`CurrentRecipe`、各种 `*Snapshot` 及 `RolledResults` 迁移成一个已付款任务，不再次扣电或抽样。

### 调用链与风险

已读取核心机器类、配方 Codec、能力注册、两个溜槽 Mixin、JEI、渲染器及测试；核对本地 Create 依赖源码中的 IBE、SmartBlockEntity、ChuteBlockEntity。

1. `Block.onRemove → IBE.onRemove → SmartBlockEntity.destroy`。同方块状态变更被 IBE 排除；普通卸载不应走内容释放。原实现没有回收自有库存和活动任务。
2. 输入不查配方且不许提取，无效物品可永久占位。
3. `batchLocked` 阻止等待时调高；结构失效只阻止新任务，未阻止已开工进度。
4. 调度和输出均有队首阻塞；活动任务逐 tick 完整同步，FE 与库存变动再次同步。
5. 客户端不接收 `BoundInput`，依赖它的溜槽渲染查找无法匹配；仅按相同物品隐藏还会误伤下一批。
6. 额外发现：移动带物品的溜槽时，删除旧方块触发 `ChuteBlockEntity.destroy` 掉落库存，然后新溜槽读取原 NBT 恢复同一库存，有复制风险；修复必须清晰转移旧、新溜槽库存并覆盖回滚。
7. 空气输出崩溃恢复依赖附近 UUID 实体扫描，不能视为跨区块保存的严格事务；需要单独验证恢复路径。
8. 未发现基座合成配方；现有唯一压制配方的数值为 40 tick、高度 8、基础 2000 FE，保持不变。

### 测试与构建

- 原有重力压制器测试：`KineticPressGameTests` 16 项，`KineticPressAnimationGameTests` 2 项，共 18 项。
- `gradlew.bat compileJava processResources --offline`：通过。
- 实际基线运行：`gradlew.bat runGameTestServer --offline --console=plain`，日志 `build/kinetic-press-stage0.log`：108 项 required GameTest 全部通过（包含压制器 18 项），BUILD SUCCESSFUL，54 秒。
- 后续每阶段先编译、实际运行对应 GameTest，通过后才继续下一阶段；不删除测试或放宽断言。

## 阶段 1：拆除回收（通过）

- 修改 `KineticPressBlockEntity`：真实 `destroy()` 集中调用幂等的 `releaseContentsForRemoval()`，客户端不执行掉落。
- 修改 `KineticPressProcessor`：`drainForRemoval()` 清空真实输入、活动任务、待输出及输出缓存，活动任务只回收原料，绝不结算其预抽产物；不退 FE。
- 修改 `KineticPressOutputManager`：拆除前处理历史待提交掉落；恢复缓存装不下时保留余量，避免静默丢失。
- 同方块外观更新由现有 `IBE.onRemove` 判断排除；卸载和正常退出仍走保存，不走内容释放。
- 新增 `KineticPressSafetyGameTests` 8 项；`runGameTestServer --offline --console=plain` 编译并实际回归，116 项 required 全部通过，日志 `build/kinetic-press-stage1.log`，50 秒。
- 限制：正常保存/重载可以验证；操作系统在方块数据与实体分开保存之间强制崩溃的跨文件原子性，不能靠本地附近实体 UUID 搜索证明，最终报告须明确列出，不能宣称严格崩溃事务已验证。

## 阶段 2：输入过滤和退料（通过）

- 修改 InputHandler、Processor：顶面和溜槽共用当前 RecipeManager 过滤，模拟插入无状态变化；有效原料不因缺电或输出满被判为无效。
- 修改 Block、BlockEntity：潜行、主手空手右键退回尚未启动的原料，优先进背包，溢出生成掉落物；已有任务和产物保留。有活动任务时维持批次锁，不能插入新批破坏 16 件批次上限。
- 本地 Create ValueSettingsInputHandler 排除潜行玩家，确认不抢占高度设置；补充中英文动作栏提示和原版拾取音效。
- 新增 InputGameTests 5 项。实际运行 121 项 required 全通过，日志 `build/kinetic-press-stage2.log`，41 秒。
- 尚未引入配方缓存，重载直接使用当前 RecipeManager；真实数据包重载专项验证留到阶段 6。

## 阶段 3：状态与恢复（通过）

- Processor 推导状态：暂停 > 结构无效 > 活动加工 > 收集 > 输出等待 > 超容量/能源等待 > 无效输入 > 空闲。正在运行的批次以 RUNNING 为主，不为其他未开工候选的缺电覆盖运行状态。
- 没有活动任务即可调高，等待物品启动时重新按当前高度计算；活动任务继续锁定高度。
- 结构无效暂停活动进度，保留已付款任务和已抽结果。红石禁止 Capability 与 Container 主动提取，拆除回收使用内部提取接口不受暂停影响。
- BindingHandler 转移源库存后再移除旧溜槽，回滚清空目标再恢复源，解决带物品移动复制。
- 新增 Recovery 6 项和 Binding 3 项测试。首轮130项中129通过；等待批次从3到8格的测试被移动条件拒绝，日志 `build/kinetic-press-stage3.log`，先定位原因再继续，保留失败记录。
- 根因：5格高模板默认包围的 BARRIER 屋顶；对高塔测试设置 `skyAccess=true` 并增加逐格路径检查。修复夹具后实际130项全通过，日志 `build/kinetic-press-stage3-recheck.log`，37秒。

## 阶段 4：并行调度和输出（通过）

- Processor 使用有界轮询，能源或容量不满足则跳过，最多16个候选和16个活动任务。扣电前准备完整任务及预抽结果，服务端单线程提交前再次核验输入与能源，保留最大输出容量预留。
- OutputManager 逐槽尝试可接受产物，保留部分插入回收、空气掉落和失败10tick节流。
- 新增兼容字段 `NextInputSlot`、`NextOutputSlot`，旧档缺失按0处理。
- SchedulingGameTests 新增5项；实际135项required全部通过，日志 `build/kinetic-press-stage4.log`，37秒。

## 阶段 5：同步与显示（通过）

- BE 普通变更脏标记每4tick刷新；任务启停、运行状态、结构和绑定变化紧急标记，同tick末合并。同步 BoundInput/BlockingPosition，字段缺失清空客户端旧绑定。
- Processor 同步 AnimationRevision/AnimationRunning；新增无客户端类依赖的 Timeline，最多预测4tick，不早于确认完成，暂停冻结、旧快照不回退，完成显示一个终帧后清理。
- Renderer 按整个加工柱AABB判断16格可见范围。删除无法可靠识别转移对象的溜槽隐藏拦截，保留普通溜槽原生渲染。
- SyncGameTests新增7项，实际142项required全部通过，日志 `build/kinetic-press-stage5.log`，40秒。
- 可测结果：40tick任务、60tick观察窗口，12次super.sendData刷新请求。原实现仅活动进度就有40次调用；Create本来已合并同tick更新，因此这是请求次数比较，不是实测网络包数或带宽。
- 图形、23格顶部可见性和高延迟联机手感未实机验证，保留人工验收。

## 阶段 6：查找、缓存与配方校验（通过）

- Processor 为最多64种完整物品/组件组合保留配方命中或无匹配结果；当前 RecipeManager 的不可变配方集合视图作为版本标识，数据包 apply/replaceRecipes 替换集合即清缓存，读存档也清缓存。
- 不缓存能源、输出空间或任务启动资格；每次启动仍按实时高度、能源与预留状态检查。
- `getRecipeLookupCount()` 仅记录真实 RecipeManager 查询，用等待批次100次推进测量收益；不冒充TPS或全服性能基准。
- 新增 Lookup：弱 Level 键、弱 BE 引用、按输入位置直接查找；onLoad即时注册，绑定/读取更新，卸载/移除失效。邻居事件只查内存候选，覆盖低两格间隙；保留20tick周期校验。真实两个维度隔离测试通过。
- Recipe/Serializer/Output：产物1..8、参数范围、基础容量、最大输出放得进空9槽；JSON按当前服务端配置校验最高23格可执行性。网络解码共用结构校验，不按客户端COMMON配置误拒服务端配方；非法JSON由DataResult提供明确错误。JEI 0%不再显示1%。
- 新增 Cache 4项、Index 3项、Validation 7项；实际156项required全部通过，日志 `build/kinetic-press-stage6.log`，49秒。
- 可测量：等待100次tick仅1次RecipeManager查询；溜槽从最多21次世界BE读取改为1次输入索引查找；邻居事件0次世界BE读取（最多24次内存索引查找）。后两项为调用路径比较，不是TPS实测。

## 阶段 7：生存内容与使用反馈（通过自动验证）

- 新增 `data/createsandustry/recipe/kinetic_press.json`：`IPI / ECE / IBI`，I=铁锭，P=精密构件，E=电子管，C=黄铜机壳，B=铁块。合计4铁锭、1精密构件、2电子管、1黄铜机壳、1铁块，产出1基座；按现有Create黄铜阶段采用保守可获取方案，没有压制器自依赖，没有修改重力构件的序列装配/弹跳激活。
- 向镐挖掘标签补充基座。既有方块、物品、能力及配方类型注册继续复用。
- Processor新增真实批次总数/完成数、活动任务剩余tick和能源诊断；存在待处理输入、待输出、暂停或结构无效时标记等待，不给整批虚假完成时间。客户端只读服务端同步值。
- BE护目镜显示电量、设定/绑定实际高度、结构及阻挡坐标、输入/活动数、批次完成数、状态、额外FE、缺口和超容量。
- JEI保留原机器动画与布局风格，补齐FE、tick/秒、高度、额外耗电公式和独立概率解释；8个产物槽保持在页面内。额外耗电提示明确是服务端配置，默认2000 FE，不将本地COMMON值冒充服务端实际值。
- 新增 `KineticPressScenes`、复用 `ModPonderScenes` 注册和 `kinetic_press/basic.nbt`（5×12×5）；13段中英文本演示搭建、原生调高、快捷溜槽、六面FE、输入、批次和指数下落、向下输出、低高度耗电、红石暂停与恢复、潜行空手退料。Ponder为客户端脚本演示，不在虚拟场景启动真实服务端加工。
- Diagnostics 5项、Content 2项新增测试通过；实际 `runGameTestServer --offline --console=plain` 共163项required通过，日志 `build/kinetic-press-stage7.log`，50秒。
- 实际Ponder播放、JEI/护目镜文字位置及中文字体观感尚未验证；资源可读和类编译通过不替代这部分人工验收。

## 阶段 8：综合回归与交付（通过自动验证）

收尾复查修复两处边界并补测试：

1. 输入索引由单一拥有者改为同输入位置的候选列表。下方无效基座与上方有效基座都绑定同一溜槽时，按原向下扫描的规则优先最近基座，不受刷新/读取顺序影响；分别移除所属条目，不删除其他拥有者；最近基座暂时无效仍保持上游物品，拆除后下方绑定自动恢复。每次仍只查一次输入位置索引，再检查内存候选。
2. 客户端在BE读取入口先检查修订号，再读取Create行为、高度、电量、结构和绑定，防止旧包只被动画处理器拒绝、却覆盖其余字段。同tick相同修订号的合法变化仍接受；不改服务端库存与能源逻辑。

新增Index、Sync各1项回归，最终命令：

```powershell
.\gradlew.bat build runGameTestServer --offline --console=plain
```

- 首轮完整build及163项回归通过，保留 `build/kinetic-press-final.log`。
- 两处边界补齐后重新完整build及实际GameTest：`build/kinetic-press-final-recheck.log`，`BUILD SUCCESSFUL in 1m 7s`，`All 165 required tests passed`，测试执行9.570秒，进程退出码0。
- 原有18项压制器测试、其他90项项目测试全部保留；没有删除失败测试、放宽业务断言或屏蔽异常。唯一阶段性GameTest失败为阶段3高塔夹具BARRIER屋顶，原因和修复见该阶段；最终0失败。
- 对最终JAR核对合成配方、原压制配方、Ponder NBT、Lookup/Timeline/场景class、Mixin声明及78个压制器相关中英文键与占位符。资源与源码一致；移除的错误隐藏Mixin未残留在JAR。
- 产物：`G:/MODS/MC/createsandustry-neoforge-1.21.1/build/libs/createsandustry-0.1.9.jar`，4625992字节；版本保持0.1.9，没有替用户升级版本或发布。
- SHA-256：`2726ed3e8b3984295498f40cf39b041042857cdc06abee02693812e50fdf52b5`。

### 测试组成

以下均为实际GameTestServer运行并通过的压制器测试；编译不单独计为测试通过。

| 类名（均位于kinetic_press包） | 数量 | 主要覆盖 |
|---|---:|---|
| KineticPressGameTests | 16 | 原有加工、并行、能耗、输出、原生行为与兼容基线 |
| KineticPressAnimationGameTests | 2 | 原有动画公式 |
| KineticPressSafetyGameTests | 8 | 拆除所有权、原料/产物二选一、卸载及NBT恢复 |
| KineticPressInputGameTests | 5 | 配方过滤、模拟插入、溜槽拒收、退料及组件 |
| KineticPressRecoveryGameTests | 6 | 缺电/堵输出/结构/红石/超容量/等待调高恢复 |
| KineticPressBindingGameTests | 3 | 带库存溜槽移动、失败前置检查及恢复 |
| KineticPressSchedulingGameTests | 5 | 混合任务越过阻塞、公平输出、部分接收及确定结果 |
| KineticPressSyncGameTests | 8 | 16动画、预测上限、暂停/恢复、旧包、清理、绑定及同步计数 |
| KineticPressRecipeCacheGameTests | 4 | 正负缓存、组件、替换配方失效及查询计数 |
| KineticPressRecipeValidationGameTests | 7 | 非法参数、容量、JSON/网络Codec、独立概率语义 |
| KineticPressIndexGameTests | 4 | 绑定迁移/卸载、真实维度隔离、低间隙事件、共享输入优先级 |
| KineticPressDiagnosticsGameTests | 5 | 批次计数、真实剩余时间、能源诊断、客户端及旧档计数 |
| KineticPressContentGameTests | 2 | 真实合成配方可获取性、无循环、Ponder模板读取 |
| **压制器总计** | **75** | **原有18 + 新增57，全部通过** |
| **项目完整套件** | **165** | **压制器75 + 其余90，全部通过** |

### 关键修改位置

下列链接为当前工作区位置。关联测试在同包中，名称见上表；各阶段列出的文件共同构成修改范围。

| 文件位置 | 作用 |
|---|---|
| [KineticPressBlockEntity.java:181](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressBlockEntity.java:181) | 拆除集中释放；同文件115行同步合并、234行安全调高、439行退料、564行护目镜、612行整包旧版本保护 |
| [KineticPressProcessor.java:136](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressProcessor.java:136) | 有界轮询与开工事务准备；同文件包含缓存、状态、退料、进度诊断和兼容NBT |
| [KineticPressInputHandler.java](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressInputHandler.java) | 顶部/溜槽共享过滤，16槽每槽1件及模拟语义 |
| [KineticPressOutputManager.java:61](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressOutputManager.java:61) | 多槽轮询、部分输出与待提交掉落恢复；OutputHandler同包实现暂停时拒绝外部抽取 |
| [KineticPressBindingHandler.java:24](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressBindingHandler.java:24) | 溜槽物品所有权转移与失败回滚 |
| [KineticPressLookup.java:19](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressLookup.java:19) | 已加载基座/输入索引、候选优先级、生命周期失效及邻居查找 |
| [KineticPressAnimationTimeline.java:17](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressAnimationTimeline.java:17) | 不依赖客户端类的预测/插值数据及完成清理 |
| [KineticPressRenderer.java:71](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/client/KineticPressRenderer.java:71) | 加工柱AABB距离判断；Mixin JSON删除错误的溜槽隐藏拦截 |
| [KineticPressRecipeSerializer.java:30](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressRecipeSerializer.java:30) | JSON结构/可执行性校验与诊断，Recipe/Output构造器共用参数限制 |
| [KineticPressBlock.java:59](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/block/machine/kinetic_press/KineticPressBlock.java:59) | 潜行主手空手右键退料，权限检查及客户端响应 |
| [KineticPressCategory.java](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/compat/jei/KineticPressCategory.java) | 原风格JEI、0%修复、时间/能源/高度说明；SandustryJeiPlugin同步页面大小 |
| [KineticPressScenes.java](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/java/com/minechai/create_sandustry/content/ponder/scenes/KineticPressScenes.java) | 教程脚本；ModPonderScenes注册；对应NBT与中英文lang资源 |
| [kinetic_press.json](G:/MODS/MC/createsandustry-neoforge-1.21.1/src/main/resources/data/createsandustry/recipe/kinetic_press.json) | 新增生存合成；pickaxe标签补充基座 |

### 交互与兼容规则

- 新增潜行、主手空手右键退料：只返尚未开工物品，优先进背包，溢出掉落；活动任务和已有产物保留。仍有活动/待输出任务时维持批次锁，清空后才接受新批。
- 等待能源、输出或收集中的批次允许调高，启动时重算费用；有活动任务则拒绝调高。移动带库存的溜槽保持原物品及组件，不额外掉落。
- 无配方物品由顶部和绑定溜槽共同拒收，留在上游。重载失效的历史输入可退回；缺电/高费用/输出满不改变物品的配方资格。
- 红石暂停覆盖物品自动接口与加工，仍可充FE及主动退料；解除信号或恢复结构后继续已付费的原任务，不重抽产物。
- 保留原有NBT及旧单任务迁移。新增 `NextInputSlot`/`NextOutputSlot` 缺失按0开始；`BatchCompleted`缺失按0，`BatchTotal`至少按现存输入和活动任务推断，不虚构已输出历史。旧任务继续读取原料、已付FE和确定结果。
- 配方字段名和压制类型ID未改；现有正常配方保持可读。现在会明确拒绝负数、非法概率、空/超过8项产物和最高工作高度仍不能执行的配方，这属于所要求的非法数据校验。
- 活动任务使用开工快照，不因重载重算FE或结果；等待输入及新输入使用当前RecipeManager版本。客户端缓存、索引及修订号不作为库存或能源来源。

### 可测优化与仍未解决的边界

| 测量或路径 | 改进前 | 当前结果 | 证据范围 |
|---|---|---|---|
| 40tick加工、60tick观察的同步刷新请求 | 活动进度单独至少40次调用 | 12次 | 实际测试日志；不是网络包数/带宽 |
| 缺电等待100tick的RecipeManager真实查询 | 等待路径重复查询 | 1次 | 实际测试日志；不包含预热前历史调用 |
| 溜槽定位 | 最多21次世界BE读取 | 1次输入索引查找，候选弱引用/已加载/绑定检查 | 源码路径对照；重叠基座按最近拥有者选择 |
| 邻居更新查找 | 向下重复世界BE扫描 | 0次世界BE读取，最多24次内存索引查找 | 源码路径对照；20tick周期结构兜底保留 |

没有进行TPS、内存或网络带宽基准，不能据上述计数宣称相应百分比的整体性能提升。

当前已覆盖的正常接口路径未发现可复现的复制、丢失或永久卡死，但以下不作已通过承诺：真实磁盘重启/区块卸载、强制杀进程后的跨文件一致性、第三方物流抛异常、强制注入方块恢复失败、客户端图形及多人高延迟。原因与步骤见下方人工清单；附近实体UUID机制不能保证进程任意时刻强杀后的严格原子性。

测试运行环境已有Create Connected / Create Diesel Generators的缺失战利品注册项、外部整合包标签缺失和SSL联机校验错误；阶段0基线同样存在，最终未阻止build或165项测试通过。这些不属于本次压制器修改范围，未静默屏蔽，也未将日志称为零错误。

## 人工验收清单（尚未执行，不计入自动通过）

1. 生存模式按新增配方合成基座，JEI打开压制分类，分别以中文、英文检查产物、耗电、时间、公式提示无重叠；查看Ponder全部步骤和控件。
2. 单人及独立服务端各搭建3格、8格、23格塔；空手原生控件调高，手持普通溜槽右键快捷搭建。新默认配置下3格加工单件残渣应扣1200FE，8格/23格均扣200FE；只允许水平四面供电。
3. 持续供FE后投入16件残渣，确认16个独立物品动画、16粒种子（随机金粒0..16），向下箱子/传送带/置物台接收，部分满时余量保留。8格整批需要3200FE，必须持续供能，不能将3000FE初始容量误当吞料。
4. 缺电批次升至8格再供电；堵住输出再疏通；拆除输入器或在最低两格放阻挡后复原；分别在运行与等待时加红石，确认任务/抽取冻结、解除后恢复且不重复扣电。
5. 潜行空手退料，只返未开工物品；背包满仍有掉落物。携带有组件物品的溜槽上下搬迁，确认没有额外掉落或复制；等待下一批同类物品仍在溜槽中显示。
6. 玩家站在23格塔顶、侧面、基座附近，观察同屏多任务、暂停/恢复、完成清理和重新进入世界；双客户端及可用的高延迟连接检查跳变、残影和幽灵物品。
7. 在活动任务、等待电力、输出堵塞、红石暂停时分别真实离开到区块卸载并返回、正常关闭服务器再重启，核对原料/确定产物/已付FE守恒。自动测试仅覆盖生命周期回调及NBT恢复，不冒充这一步磁盘重启。
8. 用数据包更改/移除压制配方执行真实`/reload`，确认新输入资格和未开工配方及时更新，旧活动任务继续使用原快照；非法配方应有明确加载错误，旧输入可退回。
9. 创造破坏、正常挖掘、爆炸分别检查内部内容只返还一次；自动测试已覆盖共用真实onRemove链，未模拟全部玩家/爆炸来源。

边界：不宣称在进程被强杀、磁盘写入中断或第三方物流代码抛异常时拥有跨文件的严格事务保证；附近实体UUID恢复不是此类保证。底层setBlock/load抛错后的回滚代码已有显式日志和恢复，自动测试尚未注入这类故障。
