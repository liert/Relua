package com.github.relua.decompiler;
import com.github.relua.model.LuacFile;
import com.github.relua.parser.LuacParser;
public class TestXQSecure {
    public static void main(String[] args) throws Exception {
        LuacParser parser = new LuacParser();
        LuacFile luacFile = parser.parse("src/test/resources/XQSecureUtil.lua");
        Decompiler decompiler = new Decompiler();
        System.out.println(decompiler.decompile(luacFile));
    }
}
