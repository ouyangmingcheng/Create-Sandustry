# 沙化矿石纹理审核

本版改为原创的稀疏矿物颗粒，不再移植原版矿脉轮廓。
每面六处小颗粒，矿物覆盖率低于 10%；其余像素精确保留沙子基底。
三阶明暗、左上受光；铜矿保留铜色与氧化绿，锌矿为低饱和灰绿，宝石保持色相差异。
硬化沙与石化沙共享矿物布局，依靠原有基底区分。

| 方块 | 矿物像素 | 占比 | 保留沙子像素 |
|---|---:|---:|---:|
| createsandustry:hardened_sand_coal_ore | 17 | 6.64% | 239 |
| createsandustry:petrified_sand_coal_ore | 17 | 6.64% | 239 |
| createsandustry:hardened_sand_iron_ore | 16 | 6.25% | 240 |
| createsandustry:petrified_sand_iron_ore | 16 | 6.25% | 240 |
| createsandustry:hardened_sand_copper_ore | 16 | 6.25% | 240 |
| createsandustry:petrified_sand_copper_ore | 16 | 6.25% | 240 |
| createsandustry:hardened_sand_gold_ore | 17 | 6.64% | 239 |
| createsandustry:petrified_sand_gold_ore | 17 | 6.64% | 239 |
| createsandustry:hardened_sand_redstone_ore | 17 | 6.64% | 239 |
| createsandustry:petrified_sand_redstone_ore | 17 | 6.64% | 239 |
| createsandustry:hardened_sand_lapis_ore | 18 | 7.03% | 238 |
| createsandustry:petrified_sand_lapis_ore | 18 | 7.03% | 238 |
| createsandustry:hardened_sand_diamond_ore | 16 | 6.25% | 240 |
| createsandustry:petrified_sand_diamond_ore | 16 | 6.25% | 240 |
| createsandustry:hardened_sand_emerald_ore | 16 | 6.25% | 240 |
| createsandustry:petrified_sand_emerald_ore | 16 | 6.25% | 240 |
| createsandustry:hardened_sand_zinc_ore | 15 | 5.86% | 241 |
| createsandustry:petrified_sand_zinc_ore | 15 | 5.86% | 241 |

## 预览

preview.png：源矿石、源基底、沙子基底与新版纹理并排。
grain_review.png：全系列 1 倍/8 倍预览及两种基底的 3×3 连续平铺。
自动检查覆盖率、完全不透明、16×16 尺寸与基底像素保留；视觉检查关注辨识度、颗粒明暗与平铺。
