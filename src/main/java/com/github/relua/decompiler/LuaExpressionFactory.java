package com.github.relua.decompiler;

import java.util.ArrayList;
import java.util.List;

import com.github.relua.ast.BinaryOp;
import com.github.relua.ast.Expression;
import com.github.relua.ast.FunctionCall;
import com.github.relua.ast.MemberExpr;
import com.github.relua.ast.Name;
import com.github.relua.ast.SourcePos;
import com.github.relua.ast.StringConst;

public final class LuaExpressionFactory {
    private LuaExpressionFactory() {
    }

    public static Expression arithmetic(String op, Expression left, Expression right, SourcePos pos) {
        if ("%".equals(op) && left instanceof StringConst) {
            return stringFormat(left, right, pos);
        }
        return new BinaryOp(op, left, right, pos);
    }

    private static FunctionCall stringFormat(Expression format, Expression value, SourcePos pos) {
        List<Expression> args = new ArrayList<>();
        args.add(format);
        args.add(value);
        return new FunctionCall(new MemberExpr(new Name("string", pos), "format", pos), args, false, null, pos);
    }
}
