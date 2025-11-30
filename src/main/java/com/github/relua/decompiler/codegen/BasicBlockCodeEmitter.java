package com.github.relua.decompiler.codegen;

import java.util.List;

import com.github.relua.decompiler.BasicBlock;
import com.github.relua.decompiler.CodeGeneratorContext;
import com.github.relua.decompiler.DecompilerPipeline;
import com.github.relua.decompiler.IfElsePattern;
import com.github.relua.decompiler.InstructionHandler;
import com.github.relua.manager.RegisterManager;
import com.github.relua.model.Chunk;
import com.github.relua.model.Instruction;
import com.github.relua.model.Instruction.Opcode;
import com.github.relua.model.Register;
import com.github.relua.util.BasicBlockUtils;
import com.github.relua.util.RegisterUtils;

/**
 * 基本块代码生成器，负责生成基本块相关的Lua代码
 */
public class BasicBlockCodeEmitter {
    /**
     * 生成基本块代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    public void emitBasicBlocks(RegisterManager registerManager, Chunk chunk, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        generateBasicBlocksCode(registerManager, chunk, context, pipeline);
    }

    /**
     * 生成基本块代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    private void generateBasicBlocksCode(RegisterManager registerManager, Chunk chunk, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        List<BasicBlock> basicBlocks = pipeline.getBasicBlocks(chunk.getFunction());
        System.out.println("获取到的基本块数量: " + basicBlocks.size());

        if (basicBlocks.isEmpty()) {
            // 如果没有基本块信息，回退到原始的指令生成方式
            System.out.println("没有基本块信息，回退到原始指令生成方式");
            // 这里可以调用InstructionCodeEmitter来生成指令代码
            return;
        }

        // 按顺序生成每个基本块的代码
        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);

            System.out.println("\n=== 开始处理基本块 " + i + " ===");
            System.out.println("基本块类型: "
                    + (block.isIfBlock() ? "IF_BLOCK" : (block.isLoopBlock() ? "LOOP_BLOCK" : "NORMAL_BLOCK")));
            System.out.println("基本块范围: [" + block.getStartIndex() + ", " + block.getEndIndex() + "]");
            System.out.println("前驱数量: " + block.getPredecessors().size() + ", 后继数量: " + block.getSuccessors().size());
            System.out.print("前驱: [");
            for (int j = 0; j < block.getPredecessors().size(); j++) {
                BasicBlock pred = block.getPredecessors().get(j);
                int predId = basicBlocks.indexOf(pred);
                System.out.print(predId);
                if (j < block.getPredecessors().size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
            // 显示后继基本块id
            System.out.print("后继: [");
            for (int j = 0; j < block.getSuccessors().size(); j++) {
                BasicBlock succ = block.getSuccessors().get(j);
                int succId = basicBlocks.indexOf(succ);
                System.out.print(succId);
                if (j < block.getSuccessors().size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");

            // 跳过空的基本块
            if (block.getStartIndex() > block.getEndIndex()) {
                System.out.println("跳过空基本块");
                continue;
            }

            // 使用InstructionHandler为每个基本块计算好的寄存器状态

            Register blockRegister = RegisterUtils.mergePredecessors(block);
            System.out.println("基本块寄存器状态: " + blockRegister);
            registerManager.addRegister(i, blockRegister);

            // 生成基本块代码
            generateBasicBlockCode(registerManager, chunk, block, context, pipeline);

            block.setOutputState(blockRegister);

            System.out.println("=== 基本块 " + i + " 处理完成 ===");
        }
    }

    /**
     * 生成单个基本块的代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param block           基本块
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    private void generateBasicBlockCode(RegisterManager registerManager, Chunk chunk, BasicBlock block, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        List<Instruction> instructions = chunk.getInstructions();

        // 检查基本块类型，生成相应的控制流语句
        if (block.isIfBlock()) {
            // 只对包含TEST或TESTSET指令的块生成if语句
            boolean hasTestInstruction = false;
            for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                if (i < instructions.size()) {
                    com.github.relua.model.Instruction.Opcode opcode = instructions.get(i).getOpcode();
                    if (opcode == com.github.relua.model.Instruction.Opcode.TEST || opcode == com.github.relua.model.Instruction.Opcode.TESTSET ||
                            opcode == com.github.relua.model.Instruction.Opcode.EQ || opcode == com.github.relua.model.Instruction.Opcode.LT || opcode == com.github.relua.model.Instruction.Opcode.LE) {
                        hasTestInstruction = true;
                        break;
                    }
                }
            }

            if (hasTestInstruction) {
                generateIfBlockCode(registerManager, chunk, block, context, pipeline);
            } else {
                // 普通基本块，直接生成指令代码
                for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                    if (i < instructions.size()) {
                        // 这里可以调用InstructionCodeEmitter来生成指令代码
                    }
                }
            }
        } else if (block.isLoopBlock()) {
            generateLoopBlockCode(registerManager, chunk, block, context, pipeline);
        } else {
            // 普通基本块，直接生成指令代码
            for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                if (i < instructions.size()) {
                    // 这里可以调用InstructionCodeEmitter来生成指令代码
                }
            }
        }
    }

    /**
     * 生成if块代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param block           基本块
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    private void generateIfBlockCode(RegisterManager registerManager, Chunk chunk, BasicBlock block, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        List<Instruction> instructions = chunk.getInstructions();

        // 查找块内的条件指令
        com.github.relua.model.Instruction conditionInstruction = null;
        for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                com.github.relua.model.Instruction.Opcode opcode = instructions.get(i).getOpcode();
                if (opcode == com.github.relua.model.Instruction.Opcode.TEST || opcode == com.github.relua.model.Instruction.Opcode.TESTSET ||
                        opcode == com.github.relua.model.Instruction.Opcode.EQ || opcode == com.github.relua.model.Instruction.Opcode.LT || opcode == com.github.relua.model.Instruction.Opcode.LE) {
                    conditionInstruction = instructions.get(i);
                    break;
                }
            }
        }

        // 只有当找到条件指令时才生成if语句
        if (conditionInstruction != null) {
            // 使用InstructionHandler的detectIfElse方法检测if-else结构
            IfElsePattern pattern = pipeline.getControlFlowAnalyzer().detectIfElse(block, chunk);

            if (pattern != null) {
                // 是if-else结构，生成正确的if-else代码
                generateIfElseCode(registerManager, chunk, pattern, context, pipeline);
            } else {
                // 不是if-else结构，生成普通的if代码
                generateSimpleIfCode(registerManager, chunk, block, conditionInstruction, context, pipeline);
            }
        } else {
            // 如果没有条件指令，按普通块处理
            for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
                if (i < instructions.size()) {
                    // 这里可以调用InstructionCodeEmitter来生成指令代码
                }
            }
        }
    }

    /**
     * 生成if-else结构的代码
     */
    private void generateIfElseCode(RegisterManager registerManager, Chunk chunk, IfElsePattern pattern, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        // 生成if条件
        com.github.relua.model.Instruction conditionInstruction = BasicBlockUtils.getLastInstruction(pattern.testBlock, chunk);
        com.github.relua.model.Register blockRegister = pattern.testBlock.getOutputState();
        // 这里可以调用InstructionCodeEmitter来生成if条件代码
        String ifCode = "if true then";
        String condition = ifCode.substring(3, ifCode.length() - 5);
        context.addIfStatement(condition);

        // 生成then块代码
        generateBasicBlockCode(registerManager, chunk, pattern.thenBlock, context, pipeline);

        // 生成else块代码
        context.addElseStatement();
        generateBasicBlockCode(registerManager, chunk, pattern.elseBlock, context, pipeline);

        // 生成endif
        context.addEndStatement();
    }

    /**
     * 生成普通if结构的代码
     */
    private void generateSimpleIfCode(RegisterManager registerManager, Chunk chunk, BasicBlock block,
            Instruction conditionInstruction, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        List<Instruction> instructions = chunk.getInstructions();

        // 使用InstructionHandler为当前块计算好的寄存器状态生成准确的if条件
        Register blockRegister = block.getOutputState();
        // 这里可以调用InstructionCodeEmitter来生成if条件代码
        String ifCode = "if true then";
        String condition = ifCode.substring(3, ifCode.length() - 5);
        context.addIfStatement(condition);

        // 生成if块内的代码 - 处理条件指令后的指令
        for (int i = block.getStartIndex() + 1; i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                // 这里可以调用InstructionCodeEmitter来生成指令代码
            }
        }

        // 生成else块（如果有）
        java.util.List<BasicBlock> successors = block.getSuccessors();
        if (successors.size() > 1) {
            // 第一个后继是if分支，第二个是else分支
            BasicBlock trueBlock = successors.get(0);
            BasicBlock falseBlock = successors.get(1);

            // 如果false分支不是if分支的直接延续，生成else
            if (falseBlock.getStartIndex() != block.getEndIndex() + 1) {
                context.addElseStatement();

                // 生成else块内的代码，使用falseBlock的寄存器状态
                com.github.relua.model.Register falseBlockRegister = falseBlock.getOutputState();
                registerManager.addRegister(falseBlock.getStartIndex(), falseBlockRegister);
                generateBasicBlockCode(registerManager, chunk, falseBlock, context, pipeline);
            }
        }

        // 生成endif
        context.addEndStatement();
    }

    /**
     * 生成循环块代码
     * 
     * @param registerManager 寄存器管理器
     * @param chunk           代码块
     * @param block           基本块
     * @param context         代码生成上下文
     * @param handler         指令处理器
     */
    private void generateLoopBlockCode(RegisterManager registerManager, Chunk chunk, BasicBlock block, CodeGeneratorContext context, DecompilerPipeline pipeline) {
        List<Instruction> instructions = chunk.getInstructions();

        // 检查循环的第一条指令，确定循环类型
        Instruction firstInstruction = instructions.get(block.getStartIndex());
        Instruction.Opcode opcode = firstInstruction.getOpcode();

        if (opcode == Instruction.Opcode.FORLOOP || opcode == com.github.relua.model.Instruction.Opcode.FORPREP) {
            // 这是for循环，暂时生成while循环框架
            context.addCodeLine("while true do");
        } else if (opcode == com.github.relua.model.Instruction.Opcode.TFORLOOP) {
            // 这是泛型for循环，暂时生成while循环框架
            context.addCodeLine("while true do");
        } else {
            // 这是while循环，需要找到循环条件
            // 暂时生成while true do，后续可以根据循环条件优化
            context.addCodeLine("while true do");
        }

        context.increaseIndent();

        // 生成循环体代码
        for (int i = block.getStartIndex(); i <= block.getEndIndex(); i++) {
            if (i < instructions.size()) {
                // 这里可以调用InstructionCodeEmitter来生成指令代码
            }
        }

        // 生成end
        context.decreaseIndent();
        context.addEndStatement();
    }
}
