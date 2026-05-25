---
name: mat-dev
description: >-
  为 MAT（MC Automation Tools）项目添加新功能：API 命名空间、命令、Script Builder 动作、Mixin、Recorder 动作。
  当用户要求添加或扩展 MAT 模组功能时触发。
---

# MAT 开发技能

## 约束

- 目标环境：Minecraft 1.20.4、Java 17、Fabric Loom
- 纯客户端模组，所有 Minecraft API 调用必须通过 `ClientThread.runSync()` / `runSyncVoid()` 派发到主线程
- GraalJS 使用 `HostAccess.EXPLICIT`，所有 JS 可见成员必须标注 `@HostAccess.Export`
- 修改后始终运行 `./gradlew build` 验证编译

## 新增 API 命名空间（如 mat.foo）

1. 创建 `src/client/java/cn/myflycat/mat/client/api/FooBindings.java`
   - `public final class`，方法标注 `@HostAccess.Export`
   - 所有 Minecraft API 调用通过 `ClientThread.runSync()` / `runSyncVoid()` 派发
2. 在 `ApiRoot.java:13`（即 `RecorderBindings recorder;` 之后）添加字段：
   ```java
   @HostAccess.Export public final FooBindings foo;
   ```
3. 在 `ApiRoot` 构造函数中初始化：`this.foo = new FooBindings();`
4. 更新 README.md API 表格

## 新增命令

1. 在 `MatCommand.register()` 的 `.then()` 链中添加子命令：
   ```java
   .then(ClientCommandManager.literal("name")
       .executes(ctx -> handleName(ctx)))
   ```
2. 编写静态 handler 方法，返回 `int`（1=成功，0=失败）
3. 更新 README.md 命令表格

## 新增 Script Builder 动作类型

1. 在 `ActionStep.java` 添加 `TYPE_*` 常量
2. 在 `toJavaScript()` 的 `switch` 中添加 case
3. 在 `ScriptBuilderScreen.java` 的 `PARAM_DEFS` 中添加参数定义
4. 在 `buildPalette()` 中添加至对应分类

## 新增 Mixin

1. 在 `src/client/java/cn/myflycat/mat/mixin/client/` 下创建类
2. 标注 `@Mixin(TargetClass.class)`，方法标注 `@Inject`
3. 在 `src/client/resources/mat.client.mixins.json` 的 `"client"` 数组中注册

## 新增 Recorder 动作类型

1. 在 `ActionType` 枚举中添加值
2. 在 `Recorder` 中添加 `record*()` 方法
3. 在 `CodeGenerator` 中添加 `emit*()` 方法，并在 `emitAction()` switch 中添加分支
4. 若需拦截游戏事件，添加对应 Mixin

## 构建验证

```bash
./gradlew build
```

确认 `:remapJar` 任务成功，无编译错误或 mixin 注入失败。
