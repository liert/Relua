package com.github.relua.gui.views;

import com.github.relua.ast.*;
import com.github.relua.gui.model.AstTreeNode;
import com.github.relua.gui.preferences.SettingsManager;
import com.github.relua.gui.services.AstTreeService;
import com.github.relua.model.LineRange;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AstTreeViewTest {
    @BeforeAll static void startFx() {
        try { Platform.class.getMethod("startup", Runnable.class).invoke(null, (Runnable) () -> {}); }
        catch (Throwable ignored) {}
    }
    private void onFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable error) { failure.set(error); } finally { done.countDown(); }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    @Test void pagesStatementsWithoutBreakingSubtreesAndSearchesOtherPages() throws Exception {
        Block block = new Block(null);
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            block.statements.add(new Assign("v" + i, new NumberConst(i, null), null));
            source.append("v").append(i).append(" = ").append(i).append("\n");
        }
        AstTreeNode root = new AstTreeService(source.toString(), Collections.emptyMap()).convert(block, "", "main", 1, 120);
        onFx(() -> {
            AstTreeView view = new AstTreeView(new SettingsManager(null));
            StackPane pane = new StackPane(view.getView());
            new Scene(pane, 600, 700);
            view.setFunctions(Collections.singletonList(new AstTreeService.FunctionTree("main", "main", new LineRange(1,120), root)));
            pane.applyCss(); pane.layout();
            assertEquals(3, view.getPagination().getPageCount());
            assertEquals(50, view.getTree().getRoot().getChildren().size());
            assertEquals(2, view.getTree().getRoot().getChildren().get(0).getChildren().size(), "Assignment left/right stay in their parent statement");
            view.getPagination().setCurrentPageIndex(2);
            pane.applyCss(); pane.layout();
            assertEquals(20, view.getTree().getRoot().getChildren().size());
            assertEquals("v100 = 100", view.getTree().getRoot().getChildren().get(0).getValue().summary);
            TextField search = (TextField)pane.lookup("#ast-search");
            search.setText("v119");
            pane.applyCss(); pane.layout();
            assertEquals(1, view.getPagination().getPageCount());
            assertEquals("v119 = 119", view.getTree().getRoot().getChildren().get(0).getValue().summary);
            search.setText("absent");
            pane.applyCss(); pane.layout();
            assertEquals(0, view.getTree().getRoot().getChildren().size());
            search.clear();
            ComboBox<Integer> pageSize = (ComboBox<Integer>)pane.lookup("#ast-page-size");
            pageSize.setValue(25);
            pane.applyCss(); pane.layout();
            assertEquals(5, view.getPagination().getPageCount());
            view.followSourceLine(101);
            pane.applyCss(); pane.layout();
            assertEquals(4, view.getPagination().getCurrentPageIndex());
            assertEquals("v100 = 100", view.getTree().getSelectionModel().getSelectedItem().getValue().summary);
        });
    }
}
