package com.github.relua.gui.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 十六进制数据格式化与二进制检查工具类
 */
public final class HexFormatter {

    private HexFormatter() {
    }

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * 将字节数组格式化为标准 16 字节对齐的 Hex / ASCII 视图文本
     * 格式示例:
     * 00000000  1B 4C 75 61 51 00 01 04  08 04 08 00 00 00 00 00  |.LuaQ...........|
     * @param data 二进制字节数组
     * @return 格式化后的多行文本
     */
    public static String formatHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "// 空文件或无二进制数据\n";
        }

        StringBuilder sb = new StringBuilder(data.length * 5);
        int totalBytes = data.length;

        for (int offset = 0; offset < totalBytes; offset += 16) {
            // 1. 偏移量 (8位十六进制)
            sb.append(String.format("%08X  ", offset));

            // 2. 十六进制字节 (前8个 与 后8个，中间空两格)
            int lineLen = Math.min(16, totalBytes - offset);
            for (int i = 0; i < 16; i++) {
                if (i < lineLen) {
                    byte b = data[offset + i];
                    sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
                    sb.append(HEX_CHARS[b & 0x0F]);
                    sb.append(' ');
                } else {
                    sb.append("   ");
                }
                if (i == 7) {
                    sb.append(' ');
                }
            }

            // 3. ASCII 对照列
            sb.append(" |");
            for (int i = 0; i < lineLen; i++) {
                int b = data[offset + i] & 0xFF;
                if (b >= 32 && b <= 126) {
                    sb.append((char) b);
                } else {
                    sb.append('.');
                }
            }
            sb.append("|\n");
        }

        return sb.toString();
    }

    /**
     * 根据光标所在字符偏移，计算对应的原始数据字节偏移
     * 每行固定 79 个字符 (含换行符)
     * @param charOffset 文本中的字符偏移
     * @param totalBytes 原始数据总字节数
     * @return 字节偏移 (0-based)，若不在有效字节范围则返回 -1
     */
    public static int calculateByteOffsetFromCharOffset(int charOffset, int totalBytes) {
        if (charOffset < 0 || totalBytes <= 0) {
            return -1;
        }

        final int LINE_CHARS = 79;
        int row = charOffset / LINE_CHARS;
        int col = charOffset % LINE_CHARS;

        int byteIndexInRow = -1;
        if (col < 10) {
            byteIndexInRow = 0;
        } else if (col >= 10 && col < 34) {
            byteIndexInRow = (col - 10) / 3;
        } else if (col == 34) {
            byteIndexInRow = 7;
        } else if (col >= 35 && col < 59) {
            byteIndexInRow = 8 + (col - 35) / 3;
        } else if (col >= 61 && col <= 76) {
            byteIndexInRow = col - 61;
        }

        if (byteIndexInRow >= 0 && byteIndexInRow < 16) {
            int calculatedOffset = row * 16 + byteIndexInRow;
            if (calculatedOffset < totalBytes) {
                return calculatedOffset;
            }
        }

        return Math.min(row * 16, Math.max(0, totalBytes - 1));
    }

    /**
     * 根据字节偏移，计算在格式化 Hex 文本中该字节对应的字符起始位置
     * @param byteOffset 字节偏移
     * @return 字符起始偏移
     */
    public static int calculateCharOffsetFromByteOffset(int byteOffset) {
        if (byteOffset < 0) {
            return 0;
        }
        final int LINE_CHARS = 79;
        int row = byteOffset / 16;
        int colByte = byteOffset % 16;
        int colChar = (colByte < 8) ? (10 + colByte * 3) : (35 + (colByte - 8) * 3);
        return row * LINE_CHARS + colChar;
    }

    /**
     * 检查并解析指定字节偏移处的多种数据类型表示 (Binary Inspector)
     * @param data 原始数据
     * @param offset 字节偏移
     * @return 描述字符串
     */
    public static String inspectOffset(byte[] data, int offset) {
        if (data == null || offset < 0 || offset >= data.length) {
            return "Offset: - | UInt8: - | Int32: -";
        }

        int b = data[offset] & 0xFF;
        char ascii = (b >= 32 && b <= 126) ? (char) b : '.';

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Offset: 0x%08X (%d) | ", offset, offset));
        sb.append(String.format("UInt8: %d (0x%02X) '%c'", b, b, ascii));

        // Int16 (LE)
        if (offset + 1 < data.length) {
            short sLE = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            sb.append(String.format(" | Int16: %d", sLE));
        }

        // Int32 (LE)
        if (offset + 3 < data.length) {
            int iLE = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            sb.append(String.format(" | Int32: %d", iLE));
        }

        // Float / Double (LE)
        if (offset + 3 < data.length) {
            float fLE = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            if (!Float.isNaN(fLE) && Math.abs(fLE) < 1e7 && Math.abs(fLE) > 1e-5) {
                sb.append(String.format(" | Float: %.3f", fLE));
            }
        }

        return sb.toString();
    }
}
