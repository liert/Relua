package com.github.relua.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.relua.model.Chunk;
import com.github.relua.model.Constant;
import com.github.relua.model.Instruction;
import com.github.relua.model.LuacFile;
import com.github.relua.model.Opcode;

public class XiaomiFateLua51FormatProfile extends AbstractLua51FormatProfile {
    private static final byte[] XIAOMI_MAGIC = { 0x1B, 'F', 'a', 't', 'e', '/', 'Z', 0x1B };

    @Override
    public String getName() {
        return "Xiaomi Fate Lua 5.1";
    }

    @Override
    public boolean matches(byte[] firstBytes) {
        if (firstBytes == null || firstBytes.length < 4) {
            return false;
        }
        for (int i = 0; i < firstBytes.length && i < XIAOMI_MAGIC.length; i++) {
            if (firstBytes[i] != XIAOMI_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void parseHeader(BinaryReader reader, LuacFile luacFile, byte[] firstBytes) throws IOException {
        byte[] magic = new byte[XIAOMI_MAGIC.length];
        System.arraycopy(firstBytes, 0, magic, 0, firstBytes.length);
        for (int i = firstBytes.length; i < magic.length; i++) {
            magic[i] = reader.readByte();
        }
        for (int i = 0; i < XIAOMI_MAGIC.length; i++) {
            if (magic[i] != XIAOMI_MAGIC[i]) {
                throw new IOException("Invalid Xiaomi Fate Lua header");
            }
        }

        luacFile.setMagicNumber(magic);
        luacFile.setVersion(reader.readByte());
        readCommonHeader(reader, luacFile);
    }

    @Override
    public void parseChunkHeader(BinaryReader reader, Chunk chunk) throws IOException {
        // Confirmed from Xiaomi luac LoadFunction: numparams, source, nups,
        // linedefined, is_vararg, lastlinedefined, maxstacksize.
        chunk.setNumParams(reader.readUnsignedByte());
        readString(reader);
        chunk.setSource(0);
        chunk.setNup(reader.readUnsignedByte());
        chunk.setLineDefined(reader.readInt());
        chunk.setIsVararg(reader.readUnsignedByte());
        chunk.setLastLineDefined(reader.readInt());
        chunk.setMaxStackSize(reader.readUnsignedByte());
    }

    @Override
    public Instruction decodeInstruction(int pc, int raw) {
        return new Instruction(pc, raw, mapOpcode(raw));
    }

    @Override
    public Constant parseConstant(byte type, BinaryReader reader) throws IOException {
        switch (type & 0xFF) {
            case 3:
                return Constant.nil();
            case 4:
                return Constant.booleanConstant(reader.readByte() != 0);
            case 6:
                return Constant.number(reader.readLuaNumber());
            case 7:
                return Constant.string(readString(reader));
            case 12:
                return Constant.number(reader.readInt());
            default:
                throw new IOException("Unknown Xiaomi constant type: " + (type & 0xFF));
        }
    }

    private Opcode mapOpcode(int raw) {
        int opcodeValue = raw & 0x3F;
        switch (opcodeValue) {
            case 1:
                return Opcode.CLOSURE;
            case 2:
                int c = (raw >> 14) & 0x1FF;
                if (c == 0) {
                    return Opcode.CLOSE;
                }
                return c == 3 ? Opcode.NOT : Opcode.UNM;
            case 3:
                return Opcode.LT;
            case 5:
                return Opcode.LT;
            case 7:
                return Opcode.SETLIST;
            case 6:
                return Opcode.LOADK;
            case 8:
                return Opcode.RETURN;
            case 9:
                return Opcode.TEST;
            case 10:
                return Opcode.TFORLOOP;
            case 11:
                return Opcode.FORPREP;
            case 12:
                return Opcode.SUB;
            case 13:
                return Opcode.TAILCALL;
            case 14:
                return Opcode.DIV;
            case 15:
                return Opcode.SELF;
            case 16:
                return Opcode.CALL;
            case 17:
                return Opcode.SETTABLE;
            case 18:
                return Opcode.GETUPVAL;
            case 19:
                return Opcode.EQ;
            case 20:
                return Opcode.EQ;
            case 21:
                return Opcode.CONCAT;
            case 22:
                return Opcode.LE;
            case 23:
                return Opcode.LE;
            case 24:
                return Opcode.LOADBOOL;
            case 25:
                return Opcode.MOD;
            case 26:
                return Opcode.FORLOOP;
            case 27:
                return Opcode.GETTABLE;
            case 28:
                return Opcode.NEWTABLE;
            case 30:
                return Opcode.VARARG;
            case 31:
                return Opcode.JMP;
            case 33:
                return Opcode.POW;
            case 34:
                return Opcode.MUL;
            case 35:
                return Opcode.TESTSET;
            case 36:
                return Opcode.MOVE;
            case 37:
                return Opcode.ADD;
            case 38:
                return Opcode.GETGLOBAL;
            case 39:
                return Opcode.SETUPVAL;
            case 40:
                return Opcode.SETGLOBAL;
            case 41:
                return Opcode.LOADNIL;
            default:
                return Opcode.UNKNOWN;
        }
    }

    @Override
    public String readString(BinaryReader reader) throws IOException {
        return readXorString(reader);
    }

    private String readXorString(BinaryReader reader) throws IOException {
        int length = reader.readInt();
        if (length == 0) {
            return "";
        }

        byte[] bytes = reader.readBytes(length);
        int key = 13 * length + 55;
        byte[] decoded = new byte[length - 1];
        for (int i = 0; i < decoded.length; i++) {
            decoded[i] = (byte) ((bytes[i] & 0xFF) ^ key);
        }
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
