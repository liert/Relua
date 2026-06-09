package com.github.relua.io;

public class LuaReader {
    private final byte[] data;
    private int position;

    public LuaReader(byte[] data) {
        this.data = data == null ? new byte[0] : data;
    }

    public int position() {
        return position;
    }

    public void position(int position) {
        if (position < 0 || position > data.length) {
            throw new IllegalArgumentException("Position out of range: " + position);
        }
        this.position = position;
    }

    public int remaining() {
        return data.length - position;
    }

    public boolean startsWith(byte[] prefix) {
        if (prefix == null || prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    public byte readByte() {
        require(1);
        return data[position++];
    }

    public byte[] readBytes(int length) {
        require(length);
        byte[] out = new byte[length];
        System.arraycopy(data, position, out, 0, length);
        position += length;
        return out;
    }

    private void require(int length) {
        if (length < 0 || position + length > data.length) {
            throw new IllegalArgumentException("Unexpected end of Lua bytecode");
        }
    }
}
