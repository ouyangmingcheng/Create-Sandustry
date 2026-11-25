# 沙金工艺项目结构

项目使用 Minecraft 1.21.1、NeoForge 21.1 和 Java 21。代码参考 Create: Biotech 的组织方式：以功能模块集中实现，以独立目录承载注册、兼容、网络与基础设施。

## Java 包结构

根包为 `com.minechai.create_sandustry`。

| 目录 | 职责 |
| --- | --- |
| `api/` | 供外部使用的接口与数据类型，目前包含弗洛林工具能源 API。 |
| `client/` | 全局客户端入口、流体扩展和创造栏分类横幅。功能专用渲染与客户端事件放在对应模块的 `client/` 中。 |
| `compat/` | Create、Create: Connected、Stuff 'N Additions、Sable、JEI、Jade 等集成。 |
| `content/` | 按功能划分的实现，方块、方块实体、物品、配方、事件及现有 GameTests 就近放置。 |
| `foundation/` | 多模块共用的方块规则、剑耐久工具、考古事件接口和渲染基础。 |
| `infrastructure/` | 模组启动、事件接线、配置、命令及 Ponder 插件入口。 |
| `mixin/` | 通用游戏及 Create 注入；`client/` 只在客户端加载，`compat/<mod>/` 为可选集成注入。 |
| `network/` | 网络消息类型、编解码和注册。 |
| `ponder/` | 教程场景、场景注册与标签。 |
| `registry/` | 方块、物品、配方类型、数据组件、创造栏和其他注册表。 |

## 功能模块

`content/` 按玩法归属组织，不再使用全局 `block/`、`item/`、`entity/`、`event/` 分类：

| 功能 | 模块 |
| --- | --- |
| 重力压制器 | `kineticpress/`，渲染与动画位于 `kineticpress/client/`。 |
| 弗洛林电池、工具绑定 | `florinolbattery/`，公共能源接口保留在 `api/florinol/`。 |
| 热能缓冲器、沙金机壳 | `thermalbuffer/`、`sandcasing/`。 |
| 筛振传送带及筛分配方 | `sifting/`。 |
| 沙金剑、沙噬剑与剑座 | `sandsword/`、`sandmastersword/`，拔剑会话、投射物和渲染随各自功能归档。 |
| 沙心、黄金之心 | `sandheart/`，含 HUD 与 Tooltip。 |
| 月晶石、月跃石、重力构件 | `mooncrystal/`、`moonhop/`、`gravity/`。 |
| 沙类与沙化矿石、活沙、猫砂 | `sand/`、`livingsand/`、`catlitter/`。 |
| 金色植被、紫晶花、弗洛林土壤 | `goldenflora/`、`amethystflower/`、`florinolsoil/`。 |
| 群友、土豆火箭 | `qunyou/`、`potatorocket/`。 |
| 黄金精选、黄金炒饭进度、黄金经验 | `goldenselection/`、`goldenfriedrice/`、`experience/`。 |
| 旋律谜题、蒸汽笛、时间轴承 | `melody/`、`steamwhistle/`、`timebearing/`。 |
| 沙化效果、考古、其他材料 | `sandification/`、`archaeology/`、`materials/`。 |
| 地质替换与团簇生成 | `worldgen/geology/`。 |

## 初始化与边界

`CreateSandustry` 保留模组 ID、日志和构造入口，委托 `ModBootstrap` 完成初始化。

- `infrastructure/ModBootstrap` 按原顺序注册配置、注册表、生命周期回调和网络消息。
- `infrastructure/SandustryEvents` 集中挂接公共游戏事件，保留原监听顺序和优先级。
- `client/SandustryClient` 仅在物理客户端被调用，挂接客户端初始化。
- 已使用 `@EventBusSubscriber` 的模块继续由 NeoForge 自动发现，避免重复注册。
- 通用启动入口不能无条件调用客户端初始化；新增客户端 Mixin 必须放进配置的 `client` 列表。
- 新功能优先放进 `content/<feature>/`；只有跨功能共用的基础实现才进入 `foundation/`。
- 访问其他模组 API 的集成进入 `compat/`，并沿用现有可选加载检查。

## 资源与创造栏

资源仍采用 Minecraft 标准目录：`assets/createsandustry/` 放模型、纹理、语言、声音与 Ponder 结构；`data/` 放配方、标签、战利品表和世界生成数据。资源路径和注册 ID 不随 Java 包迁移改变。

创造栏仍按「机器 → 材料与装备 → 自然方块」排列，相关物品相邻。分类内容与顺序由 `registry/ModCreativeTabs` 管理，分类标题与图标由 `registry/ModCreativeTabSection` 管理；客户端横幅布局不会向注册表或搜索栏加入占位物品。

## 验证

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

现有 GameTests 保留在所属功能模块中，继续使用原来的模组命名空间和结构模板。移动类后应同时更新 Java 引用、Mixin 配置及插件全限定名。
