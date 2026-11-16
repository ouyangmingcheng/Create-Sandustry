# Create: Sandustry · 沙金工艺

Minecraft 1.21.1 / NeoForge 21.1 / Java 21 / Create 6.0.10。

项目按功能模块组织，具体目录职责、玩法归属和初始化流程见 [项目结构说明](ARCHITECTURE.md)。

## 开发与验证

在项目目录运行：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

开发依赖和运行配置见 `build.gradle`，版本参数见 `gradle.properties`。`localRuntime` 依赖仅用于开发运行环境。

## 资料

- [NeoForge 文档](https://docs.neoforged.net/)
- [Mojang 映射许可说明](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md)
