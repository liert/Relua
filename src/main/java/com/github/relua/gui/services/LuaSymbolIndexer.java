package com.github.relua.gui.services;

import java.util.*;
import java.util.regex.*;
import com.github.relua.ast.Expression;

public class LuaSymbolIndexer {
    
    public enum SymbolType {
        FUNCTION,
        LOCAL_VAR,
        IMPORT_MODULE,
        KEYWORD
    }

    public static class LuaSymbol {
        public final String name;
        public final SymbolType type;
        public final int line;
        public final int startOffset;
        public final int endOffset;
        public final int scopeStartLine;
        public final int scopeEndLine;
        public final String signature;
        public final String description;

        public LuaSymbol(String name, SymbolType type, int line, int startOffset, int endOffset, 
                         int scopeStartLine, int scopeEndLine, String signature, String description) {
            this.name = name;
            this.type = type;
            this.line = line;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.scopeStartLine = scopeStartLine;
            this.scopeEndLine = scopeEndLine;
            this.signature = signature;
            this.description = description;
        }
    }

    private final List<LuaSymbol> symbols = new ArrayList<>();
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "and", "break", "do", "else", "elseif", "end", "false", "for", "function", 
        "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", 
        "true", "until", "while"
    ));

    private static class BlockScope {
        int startLine;
        List<LuaSymbol> locals = new ArrayList<>();
        BlockScope(int startLine) {
            this.startLine = startLine;
        }
    }

    public synchronized void rebuild(String text) {
        symbols.clear();
        
        String[] lines = text.split("\\r?\\n");
        int currentOffset = 0;
        
        Stack<BlockScope> scopeStack = new Stack<>();
        BlockScope rootScope = new BlockScope(1);
        scopeStack.push(rootScope);
        
        Pattern funcDefPattern = Pattern.compile("^(?:local\\s+)?function\\s+([A-Za-z0-9_.:]+)\\s*\\(([^)]*)\\)");
        Pattern localRequirePattern = Pattern.compile("^local\\s+([A-Za-z0-9_]+)\\s*=\\s*require\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)");
        Pattern localAssignPattern = Pattern.compile("^local\\s+([A-Za-z0-9_\\s,]+)(?:=|$)");
        
        Pattern openerPattern = Pattern.compile("\\b(function|if|for|while)\\b");
        Pattern closerPattern = Pattern.compile("\\bend\\b");
        
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            int lineNum = lineIdx + 1;
            int lineLen = line.length();
            
            // Check for function definitions
            Matcher funcMatcher = funcDefPattern.matcher(line.trim());
            if (funcMatcher.find()) {
                String funcName = funcMatcher.group(1);
                String paramsStr = funcMatcher.group(2);
                
                int startOffset = currentOffset + line.indexOf(funcName);
                int endOffset = startOffset + funcName.length();
                
                LuaSymbol funcSymbol = new LuaSymbol(
                    funcName, 
                    SymbolType.FUNCTION, 
                    lineNum, 
                    startOffset, 
                    endOffset, 
                    1, 
                    Integer.MAX_VALUE, 
                    "function " + funcName + "(" + paramsStr + ")", 
                    "Defined at line " + lineNum
                );
                symbols.add(funcSymbol);
                
                // Method signature indexing
                if (funcName.contains(":")) {
                    String methodName = funcName.substring(funcName.lastIndexOf(":") + 1);
                    int mStart = currentOffset + line.indexOf(methodName);
                    LuaSymbol methodSymbol = new LuaSymbol(
                        methodName,
                        SymbolType.FUNCTION,
                        lineNum,
                        mStart,
                        mStart + methodName.length(),
                        1,
                        Integer.MAX_VALUE,
                        "function " + funcName + "(" + paramsStr + ")",
                        "Method on " + funcName.substring(0, funcName.indexOf(":"))
                    );
                    symbols.add(methodSymbol);
                } else if (funcName.contains(".")) {
                    String methodName = funcName.substring(funcName.lastIndexOf(".") + 1);
                    int mStart = currentOffset + line.indexOf(methodName);
                    LuaSymbol methodSymbol = new LuaSymbol(
                        methodName,
                        SymbolType.FUNCTION,
                        lineNum,
                        mStart,
                        mStart + methodName.length(),
                        1,
                        Integer.MAX_VALUE,
                        "function " + funcName + "(" + paramsStr + ")",
                        "Function on " + funcName.substring(0, funcName.indexOf("."))
                    );
                    symbols.add(methodSymbol);
                }
            }
            
            // Manage block nesting depth
            int openers = 0;
            Matcher openerMatcher = openerPattern.matcher(line);
            while (openerMatcher.find()) {
                openers++;
            }
            int closers = 0;
            Matcher closerMatcher = closerPattern.matcher(line);
            while (closerMatcher.find()) {
                closers++;
            }
            
            for (int i = 0; i < openers; i++) {
                scopeStack.push(new BlockScope(lineNum));
            }
            
            // Handle function parameters scope
            if (funcMatcher.find(0)) {
                String paramsStr = funcMatcher.group(2);
                if (!paramsStr.trim().isEmpty() && !scopeStack.isEmpty()) {
                    BlockScope currentScope = scopeStack.peek();
                    String[] params = paramsStr.split(",");
                    for (String param : params) {
                        param = param.trim();
                        if (!param.isEmpty()) {
                            int pStart = currentOffset + line.indexOf(param);
                            LuaSymbol paramSymbol = new LuaSymbol(
                                param,
                                SymbolType.LOCAL_VAR,
                                lineNum,
                                pStart,
                                pStart + param.length(),
                                lineNum,
                                Integer.MAX_VALUE,
                                "local parameter " + param,
                                "Parameter of function at line " + lineNum
                            );
                            currentScope.locals.add(paramSymbol);
                        }
                    }
                }
            }
            
            // Check for require imports
            Matcher requireMatcher = localRequirePattern.matcher(line.trim());
            if (requireMatcher.find()) {
                String varName = requireMatcher.group(1);
                String moduleName = requireMatcher.group(2);
                int startOffset = currentOffset + line.indexOf(varName);
                
                LuaSymbol importSymbol = new LuaSymbol(
                    varName,
                    SymbolType.IMPORT_MODULE,
                    lineNum,
                    startOffset,
                    startOffset + varName.length(),
                    lineNum,
                    Integer.MAX_VALUE,
                    "local " + varName + " = require(\"" + moduleName + "\")",
                    "Module '" + moduleName + "' imported via require"
                );
                if (!scopeStack.isEmpty()) {
                    scopeStack.peek().locals.add(importSymbol);
                } else {
                    symbols.add(importSymbol);
                }
            } else {
                // Check for normal local declarations
                Matcher localMatcher = localAssignPattern.matcher(line.trim());
                if (localMatcher.find()) {
                    String namesList = localMatcher.group(1);
                    String[] names = namesList.split(",");
                    for (String name : names) {
                        name = name.trim();
                        if (!name.isEmpty() && !KEYWORDS.contains(name)) {
                            int startOffset = currentOffset + line.indexOf(name);
                            LuaSymbol localSymbol = new LuaSymbol(
                                name,
                                SymbolType.LOCAL_VAR,
                                lineNum,
                                startOffset,
                                startOffset + name.length(),
                                lineNum,
                                Integer.MAX_VALUE,
                                "local " + name,
                                "Local variable defined at line " + lineNum
                            );
                            if (!scopeStack.isEmpty()) {
                                scopeStack.peek().locals.add(localSymbol);
                            } else {
                                symbols.add(localSymbol);
                            }
                        }
                    }
                }
            }
            
            // Pop scopes for closed blocks
            for (int i = 0; i < closers; i++) {
                if (scopeStack.size() > 1) {
                    BlockScope popped = scopeStack.pop();
                    for (LuaSymbol localSym : popped.locals) {
                        LuaSymbol updated = new LuaSymbol(
                            localSym.name,
                            localSym.type,
                            localSym.line,
                            localSym.startOffset,
                            localSym.endOffset,
                            localSym.scopeStartLine,
                            lineNum,
                            localSym.signature,
                            localSym.description
                        );
                        symbols.add(updated);
                    }
                }
            }
            
            currentOffset += lineLen + 1;
        }
        
        while (!scopeStack.isEmpty()) {
            BlockScope popped = scopeStack.pop();
            for (LuaSymbol localSym : popped.locals) {
                LuaSymbol updated = new LuaSymbol(
                    localSym.name,
                    localSym.type,
                    localSym.line,
                    localSym.startOffset,
                    localSym.endOffset,
                    localSym.scopeStartLine,
                    lines.length,
                    localSym.signature,
                    localSym.description
                );
                symbols.add(updated);
            }
        }
    }

    public synchronized LuaSymbol findDefinition(String name, int currentLine) {
        LuaSymbol bestMatch = null;
        for (LuaSymbol sym : symbols) {
            if (sym.name.equals(name)) {
                if (sym.type == SymbolType.LOCAL_VAR || sym.type == SymbolType.IMPORT_MODULE) {
                    if (currentLine >= sym.scopeStartLine && currentLine <= sym.scopeEndLine) {
                        return sym;
                    }
                } else if (sym.type == SymbolType.FUNCTION) {
                    bestMatch = sym;
                }
            }
        }
        return bestMatch;
    }

    public synchronized List<Integer> findReferences(String text, String name, int definitionLine) {
        List<Integer> refLines = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            if (lineNum == definitionLine) {
                continue;
            }
            if (wordPattern.matcher(lines[i]).find()) {
                refLines.add(lineNum);
            }
        }
        return refLines;
    }

    public synchronized List<LuaSymbol> getAutocompleteSuggestions(String prefix, int currentLine) {
        List<LuaSymbol> results = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            return results;
        }
        
        String lowerPrefix = prefix.toLowerCase();
        Set<String> added = new HashSet<>();
        
        for (LuaSymbol sym : symbols) {
            if (sym.name.toLowerCase().startsWith(lowerPrefix) && !added.contains(sym.name)) {
                if (sym.type == SymbolType.LOCAL_VAR || sym.type == SymbolType.IMPORT_MODULE) {
                    if (currentLine >= sym.scopeStartLine && currentLine <= sym.scopeEndLine) {
                        results.add(sym);
                        added.add(sym.name);
                    }
                } else {
                    results.add(sym);
                    added.add(sym.name);
                }
            }
        }
        
        // Add standard library global keywords/functions if matching
        List<String> stdLib = Arrays.asList(
            "print", "require", "type", "tostring", "tonumber", "math", "table", "string", "pairs", "ipairs"
        );
        for (String lib : stdLib) {
            if (lib.startsWith(lowerPrefix) && !added.contains(lib)) {
                results.add(new LuaSymbol(lib, SymbolType.KEYWORD, 0, 0, 0, 0, 0, lib, "Lua built-in"));
                added.add(lib);
            }
        }
        for (String kw : KEYWORDS) {
            if (kw.startsWith(lowerPrefix) && !added.contains(kw)) {
                results.add(new LuaSymbol(kw, SymbolType.KEYWORD, 0, 0, 0, 0, 0, kw, "Lua keyword"));
                added.add(kw);
            }
        }
        
        results.sort((a, b) -> {
            int scoreA = getAutocompleteScore(a);
            int scoreB = getAutocompleteScore(b);
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }
            return a.name.compareTo(b.name);
        });
        
        return results;
    }

    private int getAutocompleteScore(LuaSymbol sym) {
        if (sym.type == SymbolType.LOCAL_VAR) {
            return 4;
        } else if (sym.type == SymbolType.IMPORT_MODULE) {
            return 3;
        } else if (sym.type == SymbolType.FUNCTION) {
            return 2;
        } else if (sym.type == SymbolType.KEYWORD) {
            return 1;
        }
        return 0;
    }
}
