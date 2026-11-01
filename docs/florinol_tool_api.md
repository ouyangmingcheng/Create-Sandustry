# 弗洛林电池工具 API

工具通过 `ItemStack` 数据组件保存所绑定的电池总控。绑定信息包括维度、总控坐标与电池 UUID，因此：

- 不受距离限制；
- 跨维度也能工作；
- 电池区块未加载或总控被破坏时暂时失效；
- 重新加载同一个总控后，原绑定自动恢复；
- 在相同坐标放置新的电池不会错误接管旧绑定。

## 快速接入：原生工具类

让工具类实现：

```java
public interface FlorinolPoweredTool {
    int getFlorinolFeCostPerOperation(ItemStack stack);
}
```

然后在每次实际完成工作后调用：

```java
public boolean consumeEnergy(ServerLevel level, ItemStack stack) {
    int cost = FlorinolToolEnergy.getOperationCost(stack);
    return FlorinolToolEnergy.tryConsume(level, stack, cost);
}
```

实现该接口后，右键电池绑定、Shift+右键解绑、FE Tooltip 和单条 FE 条会自动生效。

## 外部模组物品

无法修改继承关系的物品可以注册成本适配器：

```java
FlorinolToolEnergy.registerExternalTool(
        someItem,
        stack -> 25 // 每次操作消耗 25 FE
);
```

之后可以手动调用 `canConsume`、`tryConsume`、`getAvailableEnergy`。

## 主要 API

```java
FlorinolToolEnergy.isBindingCapable(stack);
FlorinolToolEnergy.getOperationCost(stack);
FlorinolToolEnergy.getBinding(stack);
FlorinolToolEnergy.hasBinding(stack);
FlorinolToolEnergy.getBoundBattery(level, stack);
FlorinolToolEnergy.getAvailableEnergy(level, stack);
FlorinolToolEnergy.getEnergySnapshot(stack);
FlorinolToolEnergy.refreshEnergySnapshot(level, stack);
FlorinolToolEnergy.canConsume(level, stack, fe);
FlorinolToolEnergy.tryConsume(level, stack, fe);
FlorinolToolEnergy.unbind(stack);
```

## 便携钻头适配

`create_sa:portable_drill` 已由 `PortableDrillFlorinolCompat` 接入：

- 每个方块消耗 10 FE（由 10 mB 原始燃料 × `florinol_battery.fe_per_mb` 得出）；
- 绑定后不再消耗熔岩或水，完全由 FE 驱动；
- Digging 的 3×3 额外方块和 Chunk Eater 的连锁方块分别计费；
- 绑定后由 FE 替代熔岩燃料，原 Create SA 流体燃料过程会被兼容 Mixin 跳过；
- 电池卸载、结构失效或 FE 不足时无法继续破坏方块；
- Shift+右键解除绑定。
## Create SA 黄铜装备适配

以下物品已注册为可绑定装备：

- `create_sa:brass_jetpack_chestplate`
- `create_sa:brass_exoskeleton_chestplate`

由于装备通常穿在身上，绑定方式为：将装备拿在主手，右键已激活的弗洛林电池总控，再穿回装备槽；Shift+右键可解除绑定。

- 黄铜喷气背包：每 20 tick 消耗一次 10 FE，替代原 10 mB 燃料和供水检查。
- 黄铜外骨骼：每 80 tick 消耗一次 10 FE，替代原燃料和水，并保持力量、急迫与空手暴击击退效果。
- 装备槽中的绑定装备也会由服务端每 20 tick 刷新 FE 快照。
- FE 条和 Tooltip 使用与便携钻头相同的显示逻辑。