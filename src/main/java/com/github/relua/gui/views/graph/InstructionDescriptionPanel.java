package com.github.relua.gui.views.graph;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

/**
 * 指令语义悬浮释义面板组件，负责展示 Lua 汇编指令的操作码语法与中文释义
 */
public class InstructionDescriptionPanel extends VBox {

    // 指令中文释义映射字典
    private static final Map<String, String[]> OPCODE_INFO = new HashMap<>();

    static {
        OPCODE_INFO.put("MOVE", new String[]{"R(A) := R(B)", "寄存器赋值。将寄存器 B 的值复制给寄存器 A。"});
        OPCODE_INFO.put("LOADK", new String[]{"R(A) := Kst(Bx)", "加载常量。将常量池中的常量 Bx 载入寄存器 A。"});
        OPCODE_INFO.put("LOADBOOL", new String[]{"R(A) := (Bool)B; if (C) pc++", "加载布尔值。将布尔值 B（0为false，1为true）载入寄存器 A。如果 C 非 0，则跳过下一条指令。"});
        OPCODE_INFO.put("LOADNIL", new String[]{"R(A) := ... := R(B) := nil", "批量加载 nil。将寄存器 A 到 B 范围内的所有寄存器设为 nil。"});
        OPCODE_INFO.put("GETUPVAL", new String[]{"R(A) := UpValue[B]", "获取 Upvalue。将 Upvalue B 的值载入寄存器 A。"});
        OPCODE_INFO.put("GETGLOBAL", new String[]{"R(A) := Gbl[Kst(Bx)]", "获取全局变量。将全局变量 Bx 的值载入寄存器 A。"});
        OPCODE_INFO.put("GETTABLE", new String[]{"R(A) := R(B)[RK(C)]", "读取表元素。以寄存器或常量 C 为键，读取表 B 中的值并存入寄存器 A。"});
        OPCODE_INFO.put("SETGLOBAL", new String[]{"Gbl[Kst(Bx)] := R(A)", "设置全局变量。将寄存器 A 的值赋给全局变量 Bx。"});
        OPCODE_INFO.put("SETUPVAL", new String[]{"UpValue[B] := R(A)", "设置 Upvalue。将寄存器 A 的值赋给 Upvalue B。"});
        OPCODE_INFO.put("SETTABLE", new String[]{"R(A)[RK(B)] := RK(C)", "写入表元素。以寄存器或常量 B 为键，将寄存器或常量 C 的值存入表 A 中。"});
        OPCODE_INFO.put("NEWTABLE", new String[]{"R(A) := {} (size = B,C)", "创建新表。在寄存器 A 中创建一个新表，预分配数组大小为 B，哈希表大小为 C。"});
        OPCODE_INFO.put("SELF", new String[]{"R(A+1) := R(B); R(A) := R(B)[RK(C)]", "对象方法准备。用于面向对象调用。将对象 B 复制到 R(A+1)，并将方法 B[RK(C)] 读入寄存器 A。"});
        OPCODE_INFO.put("ADD", new String[]{"R(A) := RK(B) + RK(C)", "加法运算。将寄存器或常量 B 与 C 相加，结果存入寄存器 A。"});
        OPCODE_INFO.put("SUB", new String[]{"R(A) := RK(B) - RK(C)", "减法运算。将寄存器或常量 B 与 C 相减，结果存入寄存器 A。"});
        OPCODE_INFO.put("MUL", new String[]{"R(A) := RK(B) * RK(C)", "乘法运算。将寄存器或常量 B 与 C 相乘，结果存入寄存器 A。"});
        OPCODE_INFO.put("DIV", new String[]{"R(A) := RK(B) / RK(C)", "除法运算。将寄存器或常量 B 与 C 相除，结果存入寄存器 A。"});
        OPCODE_INFO.put("MOD", new String[]{"R(A) := RK(B) % RK(C)", "取模运算。将寄存器或常量 B 对 C 求余，结果存入寄存器 A。"});
        OPCODE_INFO.put("POW", new String[]{"R(A) := RK(B) ^ RK(C)", "幂运算。将寄存器或常量 B 进行 C 次方幂运算，结果存入寄存器 A。"});
        OPCODE_INFO.put("UNM", new String[]{"R(A) := -R(B)", "取负运算。将寄存器 B 的数值取负，结果存入寄存器 A。"});
        OPCODE_INFO.put("NOT", new String[]{"R(A) := not R(B)", "逻辑非运算。将寄存器 B 进行逻辑非（not）运算，结果存入寄存器 A。"});
        OPCODE_INFO.put("LEN", new String[]{"R(A) := length of R(B)", "长度运算。获取寄存器 B（表或字符串）的长度，结果存入寄存器 A。"});
        OPCODE_INFO.put("CONCAT", new String[]{"R(A) := R(B).. ... ..R(C)", "字符串拼接。将寄存器 B 到 C 范围内的值拼接为字符串，存入寄存器 A。"});
        OPCODE_INFO.put("JMP", new String[]{"pc+=sBx", "无条件跳转。将程序计数器 PC 加上偏移量 sBx，实现跳转。"});
        OPCODE_INFO.put("EQ", new String[]{"if ((RK(B) == RK(C)) ~= A) then pc++", "等于测试。比较寄存器或常量 B 与 C。若结果与 A（0或1）不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("LT", new String[]{"if ((RK(B) <  RK(C)) ~= A) then pc++", "小于测试。比较寄存器或常量 B 是否小于 C。若结果与 A 不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("LE", new String[]{"if ((RK(B) <= RK(C)) ~= A) then pc++", "小于等于测试。比较寄存器或常量 B 是否小于等于 C。若结果与 A 不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("TEST", new String[]{"if not (R(A) <=> C) then pc++", "条件测试。检查寄存器 A 的真假值是否与 C（0为false，1为true）一致。若不一致，则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("TESTSET", new String[]{"if (R(B) <=> C) then R(A) := R(B) else pc++", "测试并赋值。如果寄存器 B 的真假值与 C 一致，则将其赋值给寄存器 A，否则跳过下一条 JMP 指令。"});
        OPCODE_INFO.put("CALL", new String[]{"R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1))", "函数调用。调用寄存器 A 中的函数，传入 B-1 个参数，并将 C-1 个返回值存回 R(A) 起始的寄存器。"});
        OPCODE_INFO.put("TAILCALL", new String[]{"return R(A)(R(A+1), ... ,R(A+B-1))", "尾调用。以尾递归或尾调用的形式调用寄存器 A 中的函数，不占用当前栈帧。"});
        OPCODE_INFO.put("RETURN", new String[]{"return R(A), ... ,R(A+B-2)", "函数返回。返回寄存器 A 开始的 B-1 个返回值。若 B 为 0，表示返回当前栈顶的所有值。"});
        OPCODE_INFO.put("FORLOOP", new String[]{"R(A)+=R(A+2); if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }", "数值型循环迭代。将计数器 R(A) 加上步长 R(A+2)。若未达到终点 R(A+1)，则跳转回循环体并将 R(A+3) 设为计数器值。"});
        OPCODE_INFO.put("FORPREP", new String[]{"R(A)-=R(A+2); pc+=sBx", "数值型循环准备。将计数器减去步长，初始化循环参数，并无条件跳转至 FORLOOP 指令进行首次判断。"});
        OPCODE_INFO.put("TFORLOOP", new String[]{"R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2)); if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++", "泛型循环迭代。调用迭代器函数 R(A)，传入状态 R(A+1) 和控制变量 R(A+2)。如果返回的首个值不为 nil，则更新控制变量并跳转回循环体。"});
        OPCODE_INFO.put("SETLIST", new String[]{"R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B", "批量设置表元素。用于快速初始化数组。将寄存器 R(A+1) 起的 B 个值批量填入表 A 的指定索引区间。"});
        OPCODE_INFO.put("CLOSE", new String[]{"close all variables in the stack up to (>=) R(A)", "关闭 Upvalue。关闭所有在栈中等于或高于寄存器 A 地址的活动 Upvalue。"});
        OPCODE_INFO.put("CLOSURE", new String[]{"R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n))", "创建闭包。根据子函数原型 Bx 创建一个新的闭包，并存入寄存器 A。"});
        OPCODE_INFO.put("VARARG", new String[]{"R(A), R(A+1), ..., R(A+B-1) = vararg", "接收变长参数。将当前函数的变长参数（...）载入到寄存器 A 开始 of B-1 个寄存器中。"});
    }

    private Label descTitleLabel;
    private Label descSyntaxLabel;
    private Label descTextLabel;
    private boolean isCollapsed = false;
    private Button toggleDescButton;
    private VBox descContentBox;
    private Label descHeaderLabel;

    public InstructionDescriptionPanel() {
        super(8); // 间距 8
        initUI();
    }

    private void initUI() {
        setPadding(new Insets(12));
        setMinWidth(280);
        setMaxWidth(300);

        // 顶部栏
        HBox descHeaderBar = new HBox();
        descHeaderBar.setAlignment(Pos.CENTER_LEFT);

        descHeaderLabel = new Label("指令语义释义");
        descHeaderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        descHeaderLabel.setTextFill(Color.web("#00a2ff"));

        // 弹性占位
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 切换折叠按钮
        toggleDescButton = new Button("✕");
        toggleDescButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #aaaaaa;" +
            "-fx-padding: 2 6 2 6;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 12;" +
            "-fx-font-weight: bold;"
        );
        toggleDescButton.setOnMouseEntered(e -> toggleDescButton.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-text-fill: #ffffff;" +
            "-fx-padding: 2 6 2 6;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 12;" +
            "-fx-font-weight: bold;"
        ));
        toggleDescButton.setOnMouseExited(e -> toggleDescButton.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #aaaaaa;" +
            "-fx-padding: 2 6 2 6;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 12;" +
            "-fx-font-weight: bold;"
        ));
        toggleDescButton.setOnAction(event -> togglePanel());

        descHeaderBar.getChildren().addAll(descHeaderLabel, spacer, toggleDescButton);

        // 内容盒子
        descContentBox = new VBox(8);

        descTitleLabel = new Label("未选择指令");
        descTitleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        descTitleLabel.setTextFill(Color.WHITE);

        descSyntaxLabel = new Label("语法: -");
        descSyntaxLabel.setFont(Font.font("Consolas", 11));
        descSyntaxLabel.setTextFill(Color.LIGHTGRAY);
        descSyntaxLabel.setWrapText(true);

        descTextLabel = new Label("点击 CFG 块中的汇编指令查看详细释义");
        descTextLabel.setFont(Font.font("System", 11));
        descTextLabel.setTextFill(Color.web("#cccccc"));
        descTextLabel.setWrapText(true);

        descContentBox.getChildren().addAll(descTitleLabel, descSyntaxLabel, descTextLabel);

        getChildren().addAll(descHeaderBar, descContentBox);

        // 默认定位在 StackPane 右下角
        StackPane.setAlignment(this, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(this, new Insets(15));

        updatePanelState();
    }

    /**
     * 切换展开/折叠状态
     */
    public void togglePanel() {
        isCollapsed = !isCollapsed;
        updatePanelState();
    }

    /**
     * 更新显示状态与样式
     */
    private void updatePanelState() {
        if (isCollapsed) {
            descHeaderLabel.setVisible(false);
            descHeaderLabel.setManaged(false);
            descContentBox.setVisible(false);
            descContentBox.setManaged(false);

            toggleDescButton.setText("📖");

            setMinWidth(32);
            setMaxWidth(32);
            setMinHeight(32);
            setMaxHeight(32);
            setPadding(new Insets(0));
            setAlignment(Pos.CENTER);

            setStyle(
                "-fx-background-color: rgba(0, 162, 255, 0.85);" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #0088cc;" +
                "-fx-border-width: 1;"
            );

            toggleDescButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 0;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 14;" +
                "-fx-alignment: center;"
            );
        } else {
            descHeaderLabel.setVisible(true);
            descHeaderLabel.setManaged(true);
            descContentBox.setVisible(true);
            descContentBox.setManaged(true);

            toggleDescButton.setText("✕");

            setMinWidth(280);
            setMaxWidth(300);
            setMinHeight(VBox.USE_COMPUTED_SIZE);
            setMaxHeight(VBox.USE_COMPUTED_SIZE);
            setPadding(new Insets(12));
            setAlignment(Pos.TOP_LEFT);

            setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.85);" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: #555555;" +
                "-fx-border-width: 1;"
            );

            toggleDescButton.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #aaaaaa;" +
                "-fx-padding: 2 6 2 6;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;"
            );
        }
    }

    /**
     * 更新指定汇编指令行的语义释义内容
     * @param line 包含指令的汇编行
     */
    public void updateInstructionDescription(String line) {
        if (line == null || line.trim().isEmpty()) {
            resetInstructionDescription();
            return;
        }

        String trimLine = line.trim();
        int colonIdx = trimLine.indexOf(':');
        if (colonIdx == -1) {
            resetInstructionDescription();
            return;
        }

        String rest = trimLine.substring(colonIdx + 1).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length == 0) {
            resetInstructionDescription();
            return;
        }

        String opcode = parts[0];
        String[] info = OPCODE_INFO.get(opcode);

        if (info != null) {
            descTitleLabel.setText(opcode);
            descSyntaxLabel.setText("语法: " + info[0]);
            descTextLabel.setText(info[1]);

            // 点击有效指令时自动展开面板
            if (isCollapsed) {
                isCollapsed = false;
                updatePanelState();
            }
        } else {
            descTitleLabel.setText(opcode);
            descSyntaxLabel.setText("语法: 未知");
            descTextLabel.setText("未找到该指令的中文详细释义。");
        }
    }

    /**
     * 重置面板内容到初始状态
     */
    public void resetInstructionDescription() {
        if (descTitleLabel != null) {
            descTitleLabel.setText("未选择指令");
            descSyntaxLabel.setText("语法: -");
            descTextLabel.setText("点击 CFG 块中的汇编指令查看详细释义");
        }
    }
}
