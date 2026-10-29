# 沙金工艺批量沙化矿石系统交付报告

## 环境与依赖核实

- Minecraft：1.21.1
- NeoForge：21.1.251
- Create：6.0.10-280
- 项目模组 ID：createsandustry
- 实际 Create JAR：create-1.21.1-6.0.10-280-slim.jar
- Create 锌矿源方块：create:zinc_ore、create:deepslate_zinc_ore
- Create 锌矿挖掘等级：minecraft:needs_iron_tool
- Create 锌矿源方块类型与经验：普通 Block，矿石经验为 0
- 硬化沙子当前模型纹理：minecraft:block/sand
- 石化沙子当前模型纹理：createsandustry:block/petrified_sand

## 新增矿石清单

共 18 个独立方块 ID，全部属于 createsandustry 命名空间，并各自注册 BlockItem。

| 矿物 | 硬化沙子矿石 | 石化沙子矿石 |
|---|---|---|
| 煤 | createsandustry:hardened_sand_coal_ore | createsandustry:petrified_sand_coal_ore |
| 铁 | createsandustry:hardened_sand_iron_ore | createsandustry:petrified_sand_iron_ore |
| 铜 | createsandustry:hardened_sand_copper_ore | createsandustry:petrified_sand_copper_ore |
| 金 | createsandustry:hardened_sand_gold_ore | createsandustry:petrified_sand_gold_ore |
| 红石 | createsandustry:hardened_sand_redstone_ore | createsandustry:petrified_sand_redstone_ore |
| 青金石 | createsandustry:hardened_sand_lapis_ore | createsandustry:petrified_sand_lapis_ore |
| 钻石 | createsandustry:hardened_sand_diamond_ore | createsandustry:petrified_sand_diamond_ore |
| 绿宝石 | createsandustry:hardened_sand_emerald_ore | createsandustry:petrified_sand_emerald_ore |
| 锌 | createsandustry:hardened_sand_zinc_ore | createsandustry:petrified_sand_zinc_ore |

## 源方块到目标方块映射

| 矿物 | 普通源方块 | 深层源方块 | 目标规则 |
|---|---|---|---|
| 煤 | minecraft:coal_ore | minecraft:deepslate_coal_ore | 普通到硬化，深层到石化 |
| 铁 | minecraft:iron_ore | minecraft:deepslate_iron_ore | 普通到硬化，深层到石化 |
| 铜 | minecraft:copper_ore | minecraft:deepslate_copper_ore | 普通到硬化，深层到石化 |
| 金 | minecraft:gold_ore | minecraft:deepslate_gold_ore | 普通到硬化，深层到石化 |
| 红石 | minecraft:redstone_ore | minecraft:deepslate_redstone_ore | 普通到硬化，深层到石化 |
| 青金石 | minecraft:lapis_ore | minecraft:deepslate_lapis_ore | 普通到硬化，深层到石化 |
| 钻石 | minecraft:diamond_ore | minecraft:deepslate_diamond_ore | 普通到硬化，深层到石化 |
| 绿宝石 | minecraft:emerald_ore | minecraft:deepslate_emerald_ore | 普通到硬化，深层到石化 |
| 锌 | create:zinc_ore | create:deepslate_zinc_ore | 普通到硬化，深层到石化 |

完整纹理映射位于 docs/sandified_ores/texture_mapping.json。

## 源纹理到输出纹理映射

| 矿物 | 普通源纹理 | 深层源纹理 | 硬化输出纹理 | 石化输出纹理 |
|---|---|---|---|---|
| 煤 | minecraft:block/coal_ore | minecraft:block/deepslate_coal_ore | createsandustry:block/hardened_sand_coal_ore | createsandustry:block/petrified_sand_coal_ore |
| 铁 | minecraft:block/iron_ore | minecraft:block/deepslate_iron_ore | createsandustry:block/hardened_sand_iron_ore | createsandustry:block/petrified_sand_iron_ore |
| 铜 | minecraft:block/copper_ore | minecraft:block/deepslate_copper_ore | createsandustry:block/hardened_sand_copper_ore | createsandustry:block/petrified_sand_copper_ore |
| 金 | minecraft:block/gold_ore | minecraft:block/deepslate_gold_ore | createsandustry:block/hardened_sand_gold_ore | createsandustry:block/petrified_sand_gold_ore |
| 红石 | minecraft:block/redstone_ore | minecraft:block/deepslate_redstone_ore | createsandustry:block/hardened_sand_redstone_ore | createsandustry:block/petrified_sand_redstone_ore |
| 青金石 | minecraft:block/lapis_ore | minecraft:block/deepslate_lapis_ore | createsandustry:block/hardened_sand_lapis_ore | createsandustry:block/petrified_sand_lapis_ore |
| 钻石 | minecraft:block/diamond_ore | minecraft:block/deepslate_diamond_ore | createsandustry:block/hardened_sand_diamond_ore | createsandustry:block/petrified_sand_diamond_ore |
| 绿宝石 | minecraft:block/emerald_ore | minecraft:block/deepslate_emerald_ore | createsandustry:block/hardened_sand_emerald_ore | createsandustry:block/petrified_sand_emerald_ore |
| 锌 | create:block/zinc_ore | create:block/deepslate_zinc_ore | createsandustry:block/hardened_sand_zinc_ore | createsandustry:block/petrified_sand_zinc_ore |

所有源的普通基底纹理为 minecraft:block/stone，深层基底纹理为 minecraft:block/deepslate。硬化目标基底为 minecraft:block/sand，石化目标基底为 createsandustry:block/petrified_sand。

## 纹理生成

- 脚本：tools/generate_sandified_ore_textures.py
- 无第三方 Python 依赖；内置 PNG 读写器支持 Minecraft 使用的 4 位索引 PNG。
- 遮罩依据源基底精确 RGBA 调色板成员关系，并用普通/深层矿石交叉验证，不使用 RGB 阈值或整体染色。
- 确定矿物像素逐像素保留源 RGBA；透明像素保留原透明度。
- 同时属于两套基底调色板的像素全部记录在 generation_report.json 和 manual_review.md 中。
- 输出均为 16x16 RGBA PNG，使用 filter 0 和直接像素复制，无平滑、缩放或插值。
- 生成器只读取依赖 JAR 和项目源纹理，不读取既有输出作为输入，因此重复运行不会累计失真。
- 预览：docs/sandified_ores/preview.png
- 人工审核清单：docs/sandified_ores/manual_review.md
- 生成报告：docs/sandified_ores/generation_report.json

## 工具等级映射

等级依据源方块实际 needs_*_tool 标签核实，而不是按矿物名称推断。

| 矿石 | 源镐等级 | 目标锹等级 | 目标 needs 标签 |
|---|---|---|---|
| 煤 | 木 | 木 | 无 |
| 铁 | 石 | 石 | minecraft:needs_stone_tool |
| 铜 | 石 | 石 | minecraft:needs_stone_tool |
| 金 | 铁 | 铁 | minecraft:needs_iron_tool |
| 红石 | 铁 | 铁 | minecraft:needs_iron_tool |
| 青金石 | 石 | 石 | minecraft:needs_stone_tool |
| 钻石 | 铁 | 铁 | minecraft:needs_iron_tool |
| 绿宝石 | 铁 | 铁 | minecraft:needs_iron_tool |
| 锌 | 铁 | 铁 | minecraft:needs_iron_tool |

实现同时验证了：

- 目标方块仅加入 minecraft:mineable/shovel，不加入 minecraft:mineable/pickaxe。
- 方块属性单独调用 requiresCorrectToolForDrops()。
- 错误工具和低等级锹可以破坏方块，但不会进入 playerDestroy，因此无物品、无经验。
- 同等级或更高等级锹通过服务端 canHarvestBlock 判定并正常掉落。

## 属性策略

- 硬化沙子矿石复用 hardenedSandProperties()，其内容为 minecraft:sand 的完整属性副本。
- 石化沙子矿石复用 petrifiedSandProperties()，其内容为沙子属性副本再设置 strength(3.0F, 6.0F)。
- 两种矿石额外增加 requiresCorrectToolForDrops()，以满足矿石掉落判定。
- 红石矿石保留 RedStoneOreBlock 的 lit 状态、随机熄灭和 0/9 光照行为。
- GameTest 逐项比较硬度、抗爆性、音效、地图颜色和音符盒乐器与对应基底的运行时值。

## 战利品表策略

- 直接从当前 Minecraft 1.21.1 与 Create 6.0.10-280 JAR 读取源战利品表。
- 只替换精准采集分支中指向源方块的物品引用为目标沙化矿石。
- 普通矿物物品、数量范围、时运公式、爆炸衰减、条件、函数和随机序列逻辑均原样保留。
- random_sequence 改为 createsandustry:blocks/<target_id>。
- 未使用铁矿石规则替代 Create 锌矿；两种锌矿分别适配 create:zinc_ore 和 create:deepslate_zinc_ore 实际表。
- 矿石经验由 DropExperienceBlock 的源范围保留；红石使用 RedStoneOreBlock 的实际 1-5 经验逻辑；锌保持 0。

## 修改与新增文件

Java：

- 修改 src/main/java/com/minechai/create_candustry/block/ModBlocks.java
- 新增 src/main/java/com/minechai/create_candustry/block/SandifiedOreDefinitions.java
- 修改 src/main/java/com/minechai/create_candustry/item/ModItems.java
- 修改 src/main/java/com/minechai/create_candustry/item/ModCreativeTabs.java
- 新增 src/main/java/com/minechai/create_candustry/gametest/SandifiedOreGameTests.java

资源：

- src/main/resources/assets/createsandustry/blockstates/：新增 18 个 JSON
- src/main/resources/assets/createsandustry/models/block/：新增 18 个 JSON
- src/main/resources/assets/createsandustry/models/item/：新增 18 个 JSON
- src/main/resources/assets/createsandustry/textures/block/：新增 18 个 PNG
- src/main/resources/assets/createsandustry/lang/zh_cn.json、en_us.json：新增 18 组名称
- src/main/resources/data/createsandustry/loot_table/blocks/：新增 18 个 JSON
- src/main/resources/data/createsandustry/structure/empty.nbt：新增 GameTest 测试结构
- src/main/resources/data/minecraft/tags/block/mineable/shovel.json：加入 18 个目标
- src/main/resources/data/minecraft/tags/block/needs_stone_tool.json：新增
- src/main/resources/data/minecraft/tags/block/needs_iron_tool.json：新增

工具与文档：

- tools/sandified_ore_manifest.py
- tools/sandified_png.py
- tools/sandified_resources.py
- tools/generate_sandified_ore_textures.py
- tools/generate_sandified_ore_data.py
- tools/validate_sandified_ores.py
- tools/generate_empty_gametest_structure.py
- docs/sandified_ores/preview.png
- docs/sandified_ores/texture_mapping.json
- docs/sandified_ores/generation_report.json
- docs/sandified_ores/manual_review.md
- docs/sandified_ores/loot_mapping.json
- docs/sandified_ores/static_validation.json、static_validation.md
- docs/sandified_ores/runtime_validation.json、runtime_validation.md
- docs/sandified_ores/idempotency_validation.json、idempotency_validation.md
- docs/sandified_ores/final_report.md
- .gitignore：忽略 Python 缓存

## 验证结果

- Gradle build：通过。产物 build/libs/createsandustry-0.1.1.jar，SHA-256 433BD59C06E020307422DE01B1660307B859848E0D1B3AD805D4DAF43C3C2E5B。
- 静态验证：通过，18 个唯一 ID、18 个战利品表、18 个模型/方块状态、18 个纹理、18 个铲标签条目全部匹配。
- 纹理幂等性：104 个生成/纹理相关文件在连续两次生成后 SHA-256 均无变化；纹理 --check 模式也通过。
- GameTest：通过，1 个运行时测试覆盖全部 18 个目标，实际调用 ServerPlayerGameMode.destroyBlock。
- 实测覆盖：错误镐、低等级锹无物品无经验；正确等级锹正常掉落；精准采集掉落目标自身；时运锹正常产出；红石/青金石/钻石/绿宝石经验球；红石 0/9 光照；Create 普通锌矿与深层锌矿。

## 尚未完成与注意事项

- 未启动图形客户端做人工画面检查；纹理已生成放大预览，仍需按 manual_review.md 进行最终人工视觉确认。
- 有颜色同时落入基底调色板的像素无法只靠 PNG 无损判定，已全部列出供人工复核。
- Gradle 构建存在项目既有弃用 API 警告，不影响本次构建通过。
- 工作区原有未跟踪文件 config/、output/、run-gametest/、$null 未纳入本次修改。
