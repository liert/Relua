package com.github.relua.gui.services;

import com.github.relua.ast.*;
import com.github.relua.gui.model.AstTreeNode;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AstTreeServiceTest {
    @Test void preservesNestedParentsAndTableFields() {
        TableConstructor table = new TableConstructor(Arrays.asList(
                new TableField(new StringConst("answer", null), new NumberConst(42, null))), null);
        Block body = new Block(null);
        body.statements.add(new Assign("values", table, new SourcePos(7, 80)));
        Block root = new Block(null);
        root.statements.add(new WhileStatement(new BinaryOp("<", new Name("n", null), new NumberConst(5, null), null), body, null));
        AstTreeNode converted = new AstTreeService("", Collections.emptyMap()).convert(root, "", "main", 1, 100);
        AstTreeNode loop = converted.children.get(0);
        assertEquals("WhileStatement", loop.type);
        assertEquals(Arrays.asList("condition", "body"), Arrays.asList(loop.children.get(0).role, loop.children.get(1).role));
        assertEquals("left", loop.children.get(0).children.get(0).role);
        assertEquals("right", loop.children.get(0).children.get(1).role);
        AstTreeNode assignment = loop.children.get(1).children.get(0);
        assertEquals(7, assignment.pc);
        assertEquals(80, assignment.originalLine);
        assertEquals(-1, assignment.sourceLine, "Bytecode debug lines must not masquerade as generated source lines");
        AstTreeNode fields = assignment.children.get(1);
        assertEquals("TableConstructor", fields.type);
        assertEquals("fields[0].key", fields.children.get(0).role);
        assertEquals("fields[0].value", fields.children.get(1).role);
        assertEquals("42", fields.children.get(1).summary);
        assertEquals(1, converted.children.size(), "Descendants must not be flattened into the root");
    }

    @Test void handlesPreviouslyOmittedLoopAndMemberNodes() {
        Block body = new Block(null);
        body.statements.add(new ExpressionStatement(new MemberExpr(new Name("api", null), "run", null), null));
        Block root = new Block(null);
        root.statements.add(new RepeatStatement(body, new BooleanConst(true, null), null));
        root.statements.add(new ForNumeric("i", new NumberConst(1, null), new NumberConst(3, null), new NumberConst(1, null), body, null));
        root.statements.add(new ForIn(Collections.singletonList("item"), Collections.singletonList(new Name("items", null)), body, null));
        AstTreeNode converted = new AstTreeService("", Collections.emptyMap()).convert(root, "", "main", 1, 100);
        assertEquals(2, converted.children.get(0).children.size());
        assertEquals(4, converted.children.get(1).children.size());
        assertEquals(2, converted.children.get(2).children.size());
        AstTreeNode member = converted.children.get(0).children.get(0).children.get(0).children.get(0);
        assertEquals("MemberExpr", member.type);
        assertEquals("table", member.children.get(0).role);
    }

    @Test void searchesDescendantsAndMapsOnlyMatchingGeneratedLines() {
        Block block = new Block(null);
        block.statements.add(new Assign("value", new Name("needle", null), new SourcePos(0, 999)));
        block.statements.add(new ReturnStatement(Collections.singletonList(new Name("value", null)), null));
        AstTreeNode root = new AstTreeService("value = renamed\nreturn value\n",
                Collections.singletonMap("needle", "renamed")).convert(block, "", "main", 1, 2);
        assertEquals(1, root.children.get(0).sourceLine);
        assertEquals(2, root.children.get(1).sourceLine);
        assertEquals("value = renamed", root.children.get(0).summary);
        AstTreeNode filtered = root.children.get(0).filter("renamed");
        assertNotNull(filtered);
        assertEquals("Assign", filtered.type);
        assertNull(root.children.get(1).filter("renamed"));
        assertEquals(2, root.children.size(), "Filtering must not mutate shared models");
        AstTreeNode ambiguous = new AstTreeService("return value\nreturn value\n", Collections.emptyMap())
                .convert(block.statements.get(1), "", "return", 1, 2);
        assertEquals(-1, ambiguous.sourceLine, "Repeated statements must not navigate to an arbitrary occurrence");
    }
}
