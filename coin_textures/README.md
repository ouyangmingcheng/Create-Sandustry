# coin_textures — 硬币纹理归档目录

本目录是**开发资源归档**，不是 Minecraft 运行时资源目录，
不要直接当作资源包加载。正式游戏资源仍位于
`src/main/resources/assets/createsandustry/textures/item/`。

## 目录结构

```
coin_textures/
├─ README.md                   本文件
├─ preview_coins.png           开发预览拼图（上排单枚、下排堆叠）
├─ single/
│   ├─ icon.png                单枚灰度母版（16×16, LA, 9 级灰阶）
│   ├─ sand_coin.png           ...
│   ├─ rainbow_coin.png        彩虹币 48 帧纵向动画（16×768）
│   └─ rainbow_coin.png.mcmeta
└─ stack/
    ├─ icon_stack.png          堆叠灰度母版（16×16, LA, 全不透明, 8 级灰阶）
    ├─ sand_coin_stack.png     ...
    ├─ rainbow_coin_stack.png  堆叠彩虹币 48 帧（16×768）
    └─ rainbow_coin_stack.png.mcmeta
```

## 命名对应关系

| 材质 | 单枚 | 堆叠 |
|---|---|---|
| 沙子币 | `sand_coin.png` | `sand_coin_stack.png` |
| 铜币 | `copper_coin.png` | `copper_coin_stack.png` |
| 铁币 | `iron_coin.png` | `iron_coin_stack.png` |
| 锌币 | `zinc_coin.png` | `zinc_coin_stack.png` |
| 金币 | `gold_coin.png` | `gold_coin_stack.png` |
| 黄铜币 | `brass_coin.png` | `brass_coin_stack.png` |
| 钻石币 | `diamond_coin.png` | `diamond_coin_stack.png` |
| 绿宝石币 | `emerald_coin.png` | `emerald_coin_stack.png` |
| 下界合金币 | `netherite_coin.png` | `netherite_coin_stack.png` |
| 黑曜石币 | `obsidian_coin.png` | `obsidian_coin_stack.png` |
| 彩虹币 | `rainbow_coin.png` (+`.mcmeta`) | `rainbow_coin_stack.png` (+`.mcmeta`) |

## 动画规格

| 项 | 单枚彩虹币 | 堆叠彩虹币 |
|---|---|---|
| 帧数 | 48 | 48 |
| 单帧尺寸 | 16×16 | 16×16 |
| 雪碧图尺寸 | 16×768 | 16×768 |
| frametime | 2 tick | 2 tick |
| 循环周期 | 96 tick = 4.8s | 96 tick = 4.8s |
| 色相旋转 | 7.5°/帧，循环无缝 | 7.5°/帧，循环无缝 |

`.mcmeta` 显式声明 `frametime` / `width` / `height` / `frames`，
与 Minecraft 1.21.1 `AnimationMetadataSectionSerializer` 的合法键完全一致。

## 母版尺寸

| 母版 | 尺寸 | 模式 | 透明度 | 灰阶级数 |
|---|---|---|---|---|
| `single/icon.png` | 16×16 | LA | 156/256 不透明 | 9 |
| `stack/icon_stack.png` | 16×16 | LA | 256/256 不透明（无透明区） | 8 |

两张母版均为原始文件的**逐字节副本**（已哈希比对），未被修改。

## 生成方法

脚本：`tools/gen_coin_textures.py`（校验：`tools/verify_coin_textures.py`）

1. 读取母版（只读），统计其离散灰阶，归一化为 t∈[0,1]。
2. 每种材质一份颜色转换档位：9 个节点的 (色相, 饱和度) + 亮度窗口（含 gamma）；锌、黄铜的 9 个节点直接取 Create 真实像素色，节点之间在 RGB 空间线性过渡。
   亮度由二分法反解 HSL lightness，使颜色的 Rec.709 亮度恰好落在窗口值上。
3. 两张母版使用**同一套档位**：每张母版按自身灰阶范围归一化后取样，
   因此单枚与堆叠的最暗/最亮像素取到同一个阴影色和同一个高光色。
4. 彩虹币：色相沿 (x+y) 对角线铺满帧面，每帧整体旋转 360°/48；亮度仍由母版灰阶决定，
   每帧独立从母版重算，不拷贝另一套图像。
5. 写入保护：单枚集合受保护（已存在且字节不同则不覆盖，重跑只能得到逐字节相同的结果）；堆叠集合是本次任务的输出，可重复生成（结果确定，重跑逐字节一致）。

锌与黄铜的参考来源：机械动力 `create-1.21.1-6.0.10-280-slim.jar` 中的
`textures/item/zinc_ingot.png`、`zinc_nugget.png`、`brass_ingot.png`、`brass_nugget.png`、
`brass_sheet.png`、`textures/block/zinc_block.png`、`brass_block.png`、`brass_casing.png`。
色阶全部取自这些真实像素（详见脚本注释）。
