# 贡献与版本控制工作流

本项目使用 Git 和 Conventional Commits 管理《沙金工艺》模组源码。提交应保持单一目的，避免把功能、重构、资源和构建配置的无关改动混在同一提交中。

## 开始修改前

1. 检查工作区：

   ```powershell
   git status --short --branch
   ```

2. 确认当前分支和本地/远程差异。
3. 对较大的功能开发或重构，先确保已有改动已被有意保存，再从 `main` 创建专用分支，例如：

   ```powershell
   git switch -c refactor/package-structure
   ```

4. 只修改与当前任务相关且职责清晰的文件。

## 提交前检查

1. 检查改动范围：

   ```powershell
   git diff --check
   git diff --stat
   git diff
   ```

2. 运行构建：

   ```powershell
   .\gradlew.bat build
   ```

3. 确认没有把以下内容加入暂存区：

   - `build/`、`.gradle/`、`run/`
   - IDE 个人配置、日志、崩溃报告
   - 本地反编译参考源码或工具生成的临时预览
   - 密码、令牌、私钥、证书、本地环境配置

4. 分逻辑单元暂存并提交：

   ```powershell
   git add <相关文件>
   git diff --cached
   git commit -m "<type>(<scope>): <summary>"
   ```

## 提交信息规范

格式：

```text
<type>(<scope>): <summary>
```

- `type`：提交类型，使用下列英文小写值。
- `scope`：可选，说明受影响模块，例如 `package`、`florinol-battery`、`sand-heart`、`network`。
- `summary`：简洁说明实际变更，不使用“更新”“修复”这类单独出现的含糊描述。

| 类型 | 用途 |
|---|---|
| `feat` | 新增功能 |
| `fix` | 修复问题 |
| `refactor` | 重构代码，不改变外部行为 |
| `perf` | 性能优化 |
| `assets` | 纹理、模型、声音、语言等资源变更 |
| `config` | 配置默认值、配置项或配置结构调整 |
| `build` | 构建系统、Gradle 或依赖调整 |
| `docs` | 文档更新 |
| `test` | 测试相关变更 |
| `chore` | 工程维护、版本控制配置等 |

示例：

```text
feat(florinol-battery): add multiblock energy storage
fix(cat-litter): preserve archaeology loot table
refactor(package): split registry and interaction packages
assets(coin): update rainbow coin animation frames
build(deps): update NeoForge dependency
docs(workflow): document Conventional Commits
chore(git): initialize repository and ignore local run data
```

## 分支与提交原则

- `main` 保持可构建、可发布状态。
- 功能、修复和较大重构优先使用独立分支。
- 一个提交只完成一个逻辑目标。
- 不提交未经验证的行为变更。
- 不使用 `git reset --hard`、强制推送或重写共享历史。
- 未经明确授权，不自动提交或推送无关改动。
- 不提交敏感信息；疑似秘密文件应立即移除并停止提交。

## 远程备份

配置远程仓库后，推送前应再次检查：

```powershell
git status --short --branch
git log --oneline --decorate -5
git remote -v
```

不要擅自创建远程仓库、修改远程 URL 或强制推送。