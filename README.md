# MAT

**MC Automation Tools** — 基于 Fabric 和 Baritone 的脚本化自动化框架。

通过嵌入 GraalJS 引擎在 Minecraft 客户端内执行 JavaScript 脚本，把寻路、挖矿、聊天、库存查询等操作暴露为可组合的 API，让玩家用代码描述自动化流程。

## 当前状态

MVP 已落地，可在 1.20.4 客户端运行：

- GraalJS 脚本引擎（每个脚本独立 Context，主线程调度）
- `/mat` 命令树：`run / stop / list / reload / record`
- `require()` 模块系统（CommonJS 风格，可跨脚本调用）
- 六个 API 命名空间 + 脚本构建器：`mat.chat / mat.player / mat.inventory / mat.baritone / mat.control / mat.recorder`
- 录制：捕获游戏操作并生成脚本框架代码
- 脚本构建器：可视化拖拽编排脚本序列，实时 JS 预览
- 错误展示：脚本抛异常时聊天框输出多行红色栈追踪
- 容器交互支持：`inventory.drop/quickMove/swap` 可在箱子、熔炉等界面下操作容器槽位

未实现：自然语言接口（见[路线图](#路线图)）。

## 快速开始

1. 编译：

   ```bash
   ./gradlew build
   ```

   产物位于 `build/libs/`，把 jar 放进 `.minecraft/mods/`（同时需要 Fabric Loader、Fabric API；可选 Baritone）。

2. 脚本目录：`.minecraft/config/mat/scripts/`，文件扩展名 `.js`。

3. 写一个 `hello.js`：

   ```js
   mat.chat.log('hello from MAT');
   var pos = mat.player.pos();
   mat.chat.log('I am at ' + pos.x.toFixed(1) + ', ' + pos.y.toFixed(1));
   ```

4. 进入世界后执行：

   ```text
   /mat run hello
   ```

## 命令

| 命令 | 说明 |
| --- | --- |
| `/mat run <name>` | 启动脚本（不带 `.js` 后缀）。脚本在独立后台线程运行，可并发多个 |
| `/mat stop <name>` | 请求取消指定脚本；`/mat stop all` 取消全部 |
| `/mat list` | 列出可用脚本与正在运行的脚本 |
| `/mat reload` | 清空脚本源码缓存（脚本被外部编辑后调用） |
| `/mat record start` | 开始录制游戏操作 |
| `/mat record stop` | 停止录制，返回捕获的动作数量 |
| `/mat record save <name>` | 将录制内容保存为 `config/mat/scripts/<name>.js`（需先 stop） |
| `/mat new <name>` | 打开脚本编辑器，新建空白脚本 |
| `/mat edit <name>` | 打开脚本编辑器，编辑已有脚本 |

`stop` 通过协作式取消生效：`mat.sleep` 期间会立即响应，纯计算循环需脚本自己检查 `mat.cancelled()`。

## 脚本 API 速查

### `mat.chat`

| 调用 | 说明 |
| --- | --- |
| `chat.log(msg)` | 在本地聊天框打印（仅自己可见） |
| `chat.send(msg)` | 向服务器发送聊天消息或命令（以 `/` 开头视为命令） |

### `mat.player`

| 调用 | 返回 | 说明 |
| --- | --- | --- |
| `player.pos()` | `{x, y, z}` | 当前坐标 |
| `player.yaw()` | `float` | 朝向偏航角（度） |
| `player.pitch()` | `float` | 朝向俯仰角（度） |
| `player.onGround()` | `boolean` | 是否在地面 |
| `player.health()` | `double` | 生命值（0–20） |
| `player.food()` | `int` | 饱食度（0–20） |
| `player.saturation()` | `float` | 饱和度 |
| `player.dimension()` | `string` | 当前维度 ID（如 `minecraft:overworld`） |

### `mat.inventory`

容器名：`container` (0..N) / `hotbar` (0–8) / `inventory` (0–26) / `armor` (0–3) / `offhand` (0)。

`container` 类型仅在打开箱子、熔炉等容器界面时出现，对应容器的实际槽位。容器打开时，`slots()` 和 `find()` 会排除 `armor` 和 `offhand`（容器界面不可操作它们）。

| 调用 | 说明 |
| --- | --- |
| `inventory.slots()` | 返回所有 SlotRef |
| `inventory.find(query)` | 按条件查找。`query` 可为物品 ID 字符串，或对象 `{ id, tag, minCount, container }` |
| `inventory.hand()` | 当前手持槽位 |
| `inventory.selectHotbar(0..8)` | 切换手持槽 |
| `inventory.drop(ref)` | 扔出整组（支持容器槽位） |
| `inventory.quickMove(ref)` | shift+click 移到对侧（支持容器槽位） |
| `inventory.swap(hotbarSlot, ref)` | 把热栏槽与目标槽位互换（支持容器槽位） |
| `inventory.closeContainer()` | 关闭当前打开的容器界面（无容器时返回 false） |
| `inventory.containerType()` | 当前容器类型 ID，如 `minecraft:chest`、`minecraft:furnace`；无容器时返回 `null` |

SlotRef 暴露字段：`container / slot / id / count / empty`。slot 操作现在支持在容器界面下执行，容器槽位的 `container` 值为 `"container"`。

### `mat.baritone`

需要 Baritone 在 mods 目录中；未检测到时所有方法返回 `false`。

| 调用 | 说明 |
| --- | --- |
| `baritone.available()` | Baritone 是否在 classpath |
| `baritone.gotoBlock(x, y, z)` | 寻路到精确方块 |
| `baritone.gotoXZ(x, z)` | 仅按 XZ 寻路 |
| `baritone.gotoNear(x, y, z, radius)` | 寻路到附近 |
| `baritone.mine(quantity, ...names)` | 挖指定方块 N 个 |
| `baritone.isPathing()` | 是否正在寻路 |
| `baritone.cancel()` | 中断当前 Baritone 任务 |

### `mat.recorder`

通过 Mixin 和 ClientTick 事件自动捕获玩家操作。

| 调用 | 说明 |
| --- | --- |
| `recorder.start()` | 开始录制，清空之前记录 |
| `recorder.stop()` | 停止录制，返回动作数 |
| `recorder.isRecording()` | 是否正在录制 |
| `recorder.save(name)` | 生成 .js 并保存到脚本目录（需先 stop） |

支持自动捕获的动作：方块挖掘、方块放置、物品使用、实体攻击/交互、玩家移动（每 10 tick 采样）、聊天消息。

### `mat` 顶层

| 调用 | 说明 |
| --- | --- |
| `mat.sleep(ms)` | 阻塞当前脚本（响应取消） |
| `mat.cancelled()` | 是否已请求取消，长循环里手动检查用 |
| `require('name')` | 加载并缓存同目录下的 `name.js`，返回其 `module.exports` |

## 路线图

按计划顺序但未排期：

- **序列构建器 ~~节点编辑器~~**：可视化编排脚本（基础版已实现）
- **节点图编辑器**：基于节点的可视化编程（未来规划）
- **自然语言接口**：通过 LLM 把指令解析为本地脚本调用（不直接执行 LLM 生成的代码）

## 技术依赖

- Loader：[Fabric](https://fabricmc.net/)
- Minecraft 1.20.4
- 脚本引擎：GraalJS 24.x
- 寻路：[Baritone](https://github.com/cabaletta/baritone) API（可选）

### 代码参考

- 灵活脚本：[JS Macros](https://jsmacros.wagyourtail.xyz/?/tutorial.html)
- 视觉可视化：[Maingraph for MC](https://github.com/maingraph-project/for-mc)

## 许可证

本项目采用 **LGPL-3.0** 许可证。详见 [LICENSE](LICENSE) 文件。

### 依赖与致谢

- [Baritone](https://github.com/cabaletta/baritone)：本项目基于其 API 构建，Baritone 本身采用 **LGPL-3.0** 许可证。
- [Fabric API](https://github.com/FabricMC/fabric)：本项目使用 Fabric API 进行开发，其采用 **Apache-2.0** 许可证。
