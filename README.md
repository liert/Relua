# Relua

## 项目简介

Relua 是一个功能强大的 Lua 字节码反编译器，专为 OpenWRT 设计，能够将 Lua 编译后的字节码文件（.luac）反编译回可读的 Lua 源代码。该项目采用 Java 开发，提供了命令行界面和图形用户界面，支持多种反编译选项。

## 核心功能

- **高效反编译**：快速将 .luac 文件转换为可读的 Lua 源代码
- **命令行支持**：提供丰富的命令行参数，方便集成到脚本和自动化工作流
- **图形界面**：内置直观的 GUI，适合不熟悉命令行的用户
- **字节码显示**：支持在输出中显示原始字节码指令，便于调试和学习
- **跨平台**：基于 Java 开发，可在 Windows、Linux、macOS 等平台运行
- **模块化设计**：清晰的代码结构，便于扩展和维护

## 软件架构

Relua 采用模块化设计，主要包含以下核心组件：

| 模块 | 功能描述 |
| --- | --- |
| AST | 抽象语法树，用于表示 Lua 代码的语法结构 |
| Decompiler | 反编译器核心，负责将字节码转换为 Lua 源代码 |
| GUI | 图形用户界面，提供可视化操作 |
| Log | 日志系统，用于记录运行时信息 |
| Manager | 管理模块，协调各组件工作 |
| Model | 数据模型，定义核心数据结构 |
| Parser | 解析器，负责解析 .luac 文件格式 |
| Util | 工具类，提供通用功能支持 |

## 安装教程

### 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本（用于构建项目）

### 构建步骤

1. 克隆项目到本地：
   ```bash
   git clone https://gitee.com/your-username/relua.git
   cd relua
   ```

2. 使用 Maven 构建项目：
   ```bash
   mvn clean package
   ```

3. 构建完成后，可执行 JAR 文件将生成在 `target` 目录下：
   ```bash
   ls target/*.jar
   ```

## 使用指南

### 命令行使用

#### 基本语法
```bash
java -jar relua.jar [OPTIONS] INPUT_FILE
```

#### 命令行选项

| 选项 | 描述 |
| --- | --- |
| -o, --output FILE | 将反编译结果写入指定文件，而非标准输出 |
| -b, --bytecode | 在输出中显示字节码指令 |
| -v, --version | 显示版本信息 |
| -h, --help | 显示帮助信息 |

#### 使用示例

1. 将 `file.luac` 反编译并输出到标准输出：
   ```bash
   java -jar relua.jar file.luac
   ```

2. 将 `file.luac` 反编译并写入 `file.lua`：
   ```bash
   java -jar relua.jar -o file.lua file.luac
   ```

3. 反编译并显示字节码指令：
   ```bash
   java -jar relua.jar -b file.luac
   ```

### 图形界面使用

1. 启动图形界面：
   ```bash
   java -jar relua.jar
   ```

2. 在图形界面中，点击 "打开" 按钮选择 .luac 文件

3. 点击 "反编译" 按钮开始反编译

4. 查看反编译结果，可选择保存到文件

## 配置说明

### 日志配置

日志配置文件位于 `src/main/resources/log4j2.xml`，可根据需要修改日志级别和输出方式。

### 反编译配置

反编译过程中的一些参数可通过修改源代码中的相关常量进行调整，主要位于：
- `com.github.relua.decompiler.Decompiler` - 反编译器核心配置
- `com.github.relua.parser.LuacParser` - 解析器配置

## 贡献方式

1. Fork 本仓库
2. 新建 Feat_xxx 分支
3. 提交代码
4. 新建 Pull Request

### 开发规范

- 代码风格：遵循 Java 代码规范，使用 4 个空格缩进
- 注释：在复杂逻辑处添加中文注释
- 测试：为新功能添加单元测试
- 文档：更新相关文档

## 许可证信息

本项目采用 Apache License 2.0 许可证，详情请查看 [LICENSE](LICENSE) 文件。

## 版本历史

- v1.0.0 (2025-11-30)：初始版本，支持基本的 Lua 字节码反编译

## 联系方式

- 项目地址：https://gitee.com/your-username/relua
- 问题反馈：https://gitee.com/your-username/relua/issues

## 致谢

感谢所有为本项目做出贡献的开发者和用户！

## 免责声明

本工具仅用于学习和研究目的，请勿用于非法用途。使用本工具产生的一切后果由使用者自行承担。
