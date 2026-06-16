# Relua

Relua 是一个功能强大的 Lua 字节码反编译器，专为 OpenWRT 路由器固件和 IoT 设备开发设计，能够将编译后的 Lua 字节码文件（.luac）高效地反编译为可读的 Lua 源代码。项目采用 Java 编写，同时提供强大的命令行界面（CLI）和现代化交互式图形用户界面（GUI）。

---

## 核心特性

- **多格式支持**：
  - **标准 Lua 5.1** 字节码。
  - **OpenWrt Lua** 字节码格式。
  - **Xiaomi Fate Lua 5.1**：广泛用于小米路由器和 IoT 设备的混淆字节码格式。支持以下特性：
    - 自动识别自定义魔数：`\x1bFate/Z\x1b`。
    - 自定义 Opcode 映射表解码（共映射 42 种操作码）。
    - 独有的 XOR 加密字符串解密算法（使用 `13 * length + 55` 动态密钥解密）。
    - 针对小米混淆循环（如 `FORLOOP` 条件跳转）的控制流恢复优化。

- **深度 peephole 优化与重构**：
  - **常量与变量折叠**：自动在生成阶段将临时寄存器的 `LOADNIL`、`LOADBOOL`、`LOADK` 等指令折叠合并到直接的返回语句中。
  - **安全控制流分析**：内置防线，当目标语句带有控制流跳入标签（LabelPC）或为非临时寄存器（如局部命名变量）时，拒绝错误折叠以保证反编译准确性。

- **交互式图形用户界面（GUI）**：
  - **现代化布局**：基于 JavaFX 和 RichTextFX 打造的响应式多窗格界面。
  - **左侧边栏**：便捷的文件树导航，支持一键打开文件夹和快速加载 `.luac` 文件。
  - **中间代码编辑器**：提供极速的 Lua 语法高亮渲染、代码智能提示以及光标符号定位。
  - **右侧多功能画布**：使用高性能 JavaFX Canvas 渲染的图形可视化窗格。支持三种子视图：
    - **字节码反汇编视图（Bytecode View）**：展示文件头信息（大小端、指令大小、Lua Number 大小等）、主代码块元数据、汇编指令、常量表与局部变量。
    - **抽象语法树视图（AST View）**：可视化展示代码的层次语法结构。
    - **控制流图视图（CFG View）**：可视化基本块的连接关系，以不同颜色标识逻辑判断块（IF）、循环块（LOOP）和分支块（ELSE）。
  - **光标自动同步（Auto-Sync）**：编辑器光标位置与右侧 AST/CFG 画布实时联动，自动识别并定位当前编辑或查看的最深层 Lua 函数块（Chunk）。
  - **指令悬浮释义（Tooltip）**：在 AST/CFG 节点上点击任何字节码指令，右下角将实时浮现该指令的运行语义、操作数计算公式及中文原理解释（例如 `MOVE`、`LOADBOOL`、`TFORLOOP` 等）。
  - **底部实时日志**：内置 Log 控制台，支持从 `DEBUG` 到 `CRITICAL` 级别的日志实时分类过滤。
  - **中英双语（I18n）**：完整提供中文及英文的界面菜单与提示。

- **命令行支持（CLI）**：
  - 完善的命令行参数，支持输出重定向、日志控制与级别调整，轻松集成至自动化分析脚本。

---

## 软件架构

项目采用模块化设计，清晰分离了解析器、反编译器内核、优化器及用户界面：

| 模块名称 | 功能描述 | 关键类与包 |
| :--- | :--- | :--- |
| **AST** | 抽象语法树表示 | `com.github.relua.ast.*` (Block, Statement, Expr等) |
| **Parser** | 二进制字节码解析器 | `com.github.relua.parser.*` (LuacParser, FormatProfiles) |
| **Decode** | 操作码解码与厂商检测 | `com.github.relua.decode.*` (VendorFormatDetector) |
| **Opcode** | 字节码指令映射定义 | `com.github.relua.opcode.*` (Lua51OpcodeTable, CustomVendor) |
| **Decompiler** | 反编译分析引擎与流水线 | `com.github.relua.decompiler.*` (CFGGenerator, AstCleanupPass) |
| **Model** | 字节码结构数据模型 | `com.github.relua.model.*` (Chunk, Proto, Instruction) |
| **GUI** | JavaFX 图形界面与视图组件 | `com.github.relua.gui.*` (TextEditorView, GraphVisualizationView) |
| **Log** | 自定义异步日志输出组件 | `com.github.relua.log.*` (Logger, LogConfig) |
| **Util** | 常用工具类与分析辅助 | `com.github.relua.util.*` (BytecodeFormatter, RegisterUtils) |

---

## 安装教程

### 环境要求

- **Java 8** 或更高版本 (JavaFX 11 依赖已通过 Maven POM 引入)
- **Maven 3.6** 或更高版本 (用于构建项目)

### 构建步骤

1. 克隆项目到本地：
   ```bash
   git clone https://github.com/liert/Relua.git
   cd Relua
   ```

2. 使用 Maven 构建项目：
   ```bash
   mvn clean package
   ```

3. 构建完成后，输出文件将生成在 `target` 目录下：
   - GUI 默认入口：`com.github.relua.gui.ReluaGUI`
   - CLI 默认入口 JAR 位于：`target/relua-1.0.0.jar`

---

## 使用指南

### 命令行（CLI）使用

#### 基本语法
```bash
java -jar target/relua-1.0.0.jar [OPTIONS] INPUT_FILE
```

#### 命令行选项

| 短选项 | 长选项 | 参数类型 | 描述 |
| :--- | :--- | :--- | :--- |
| `-o` | `--output` | `FILE` | 将反编译结果写入指定文件，若不指定则输出到标准输出（stdout） |
| `-b` | `--bytecode` | 无 | 在输出中显示汇编字节码指令及常量表 |
| `-l` | `--log` | `LEVEL` | 设置日志控制台输出级别 (`DEBUG`, `INFO`, `WARNING`, `ERROR`, `CRITICAL`) |
| `-f` | `--log-file` | `FILE` | 重定向日志输出至指定文件 |
| `-v` | `--version` | 无 | 显示 Relua 的版本信息 |
| `-h` | `--help` | 无 | 显示命令行帮助信息 |

#### 系统属性
- 开启控制台日志：`java -Drelua.logToConsole=true -jar relua-1.0.0.jar ...`
- 开启底层捕获调试信息：`java -Drelua.debugOutput=true -jar relua-1.0.0.jar ...`

#### 使用示例

1. **基本反编译**并输出到控制台：
   ```bash
   java -jar target/relua-1.0.0.jar file.luac
   ```

2. 反编译并 **保存至文件**：
   ```bash
   java -jar target/relua-1.0.0.jar -o file.lua file.luac
   ```

3. 反编译并 **包含反汇编指令与调试日志**：
   ```bash
   java -jar target/relua-1.0.0.jar -b -l DEBUG -f run.log file.luac
   ```

---

### 图形界面（GUI）使用

1. **通过 Maven 启动**（推荐）：
   ```bash
   mvn javafx:run
   ```
   或者直接运行构建出的 JAR 包（需要对应平台的 JavaFX 环境配置）：
   ```bash
   java -jar target/relua-1.0.0.jar
   ```

2. **操作步骤**：
   - 点击 **"文件" -> "打开文件夹..."**，导入包含待分析 `.luac` 文件的目录。
   - 双击左侧树状图中的文件，中间区域将立即呈现反编译后的 Lua 源代码，底部显示分析日志。
   - 点击顶部菜单 **"视图" -> "切换图形"** （或选择 **AST 视图** / **CFG 视图**），可在右侧开启动态 Canvas 绘图窗格。
   - 在中间编辑器内移动光标，右侧视图会自动跟随光标同步展现对应函数的 AST 树形图或 CFG 控制流走向。
   - 鼠标左键按住空白处拖拽平移画布，使用 `Ctrl + 鼠标滚轮` 实现画布缩放。
   - 单击节点中带下划线/高亮的字节码指令，右下角释义板将自动显示中文译文与底层虚拟机寄存器变化公式。

---

## 测试说明

项目包含完善的单元测试与集成测试，涵盖了主流路由器固件逆向场景（如小米 `xqdatacenter.lua`、`http.lua`、`XQSecureUtil.lua` 以及 TP-Link 的相关字节码文件）。

运行测试：
```bash
mvn test
```

---

## 许可证信息

本项目采用 [Apache License 2.0](LICENSE) 许可证发布。

## 逆向声明

> [!WARNING]
> 本工具仅用于学习、安全审计和研究目的。请勿将本工具用于任何非法逆向工程或破解活动。因使用本工具造成的任何后果，由使用者自行承担。
