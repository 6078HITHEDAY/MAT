# MAT（立项）

* 全称：MC Automation Tools

## 技术依赖

* Loader：Fabric
* API：Baritone API
* 基于MC 1.20.4开发

## 功能概述

* 本模组提供一个脚本化的自动化框架，基于 Baritone 实现可编程的行为序列。核心思路是将常见操作抽象为可复用的脚本模块，支持嵌套调用，让玩家能以搭积木的方式构建复杂的自动化流程。

### 主要组成部分

1. 脚本系统

	* 支持录制基础操作并生成脚本

	* 提供节点式编辑界面用于编排逻辑

	* 脚本之间可通过 `import` 互相调用，实现流水线式任务

2. 自然语言接口（插件化）
	* 预留 LLM 接入能力，通过配置文件指定模型与 API

	* 意图解析后转化为本地脚本调用，不直接执行 LLM 生成的代码

3. 库存管理
	* 脚本可访问物品标签、数量、NBT 数据，编写分类与整理逻辑
	* 不预设硬编码规则，分类行为完全由玩家编写的脚本定义

## 技术说明

1. 完全客户端实现，不依赖服务端修改
	
2. 基于 Baritone API 执行具体操作
	
3. 脚本预编译缓存，减少重复解析开销

### 代码参考

Loader：[fabric](https://fabricmc.net/)

核心基础：[baritone](https://github.com/cabaletta/baritone)

灵活脚本：[JS Macros](https://jsmacros.wagyourtail.xyz/?/tutorial.html)

视觉可视化：[Maingraph for MC](https://github.com/maingraph-project/for-mc)
## 许可证

本项目采用 **LGPL-3.0** 许可证。详见 [LICENSE](LICENSE) 文件。

### 依赖与致谢
- [Baritone](https://github.com/cabaletta/baritone): 本项目基于其 API 构建，Baritone 本身采用 **LGPL-3.0** 许可证。
- [Fabric API](https://github.com/FabricMC/fabric): 本项目使用 Fabric API 进行开发，其采用 **Apache-2.0** 许可证。
