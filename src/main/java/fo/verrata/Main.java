package fo.verrata;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javafx.util.Pair;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

public class Main extends Application {
    private Pair<CodeArea, CodeArea> codeAreas;
    private List<Subscription> subscriptions = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        GridPane root = new GridPane();
        root.getStyleClass().add("grid-pane");
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(50.0);

        RowConstraints editorRowConstraints = new RowConstraints();
        editorRowConstraints.setVgrow(Priority.ALWAYS);

        RowConstraints labelRowConstraints = new RowConstraints();
        labelRowConstraints.setVgrow(Priority.NEVER);

        root.getColumnConstraints().addAll(columnConstraints, columnConstraints);
        root.getRowConstraints().addAll(editorRowConstraints, labelRowConstraints);

        codeAreas = new Pair<>(codeArea(), codeArea());
        root.add(text("Original"), 0, 1);
        root.add(text("Modified"), 1, 1);
        root.add(new StackPane(new VirtualizedScrollPane<>(codeAreas.getKey())), 0, 0);
        root.add(new StackPane(new VirtualizedScrollPane<>(codeAreas.getValue())), 1, 0);
        Scene scene = new Scene(root, 1024, 600);
        scene.getStylesheets().add(Main.class.getResource("styles.css").toExternalForm());
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setTitle("Verrata");
        primaryStage.show();
    }

    @Override
    public void stop() {
        subscriptions.forEach(Subscription::unsubscribe);
    }

    private Text text(String text) {
        Text textNode = new Text(text);
        textNode.getStyleClass().add("text-label");
        GridPane.setHalignment(textNode, HPos.CENTER);
        return textNode;
    }

    private CodeArea codeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(true);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.selectionProperty().addListener((observable, oldValue, newValue) -> {
            IndexRange selection = codeArea.getSelection();

            if (selection.getStart() == selection.getEnd()) {
                computeHighlighting();
            } else {
                codeArea.setStyleClass(
                        selection.getStart(),
                        selection.getEnd(),
                        "highlighted"
                );
            }
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(
                new MenuItem("Cut"),
                new MenuItem("Redo"),
                new SeparatorMenuItem(),
                new MenuItem("Cut"),
                new MenuItem("Copy"),
                new MenuItem("Paste"),
                new MenuItem("Delete")
        );

        codeArea.setContextMenu(menu);


        subscriptions.add(
                codeArea.multiPlainChanges()
                        .subscribe(ignore -> computeHighlighting())
        );

        return codeArea;
    }

    private void computeHighlighting() {
        int lastDiffEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastDiffEnd1 = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder1 = new StyleSpansBuilder<>();

        List<AbstractDelta<String>> diffs = diff();

        for (AbstractDelta<String> diff : diffs) {
            if (diff.getSource().size() > 0) {
                lastDiffEnd = applyDiff(spansBuilder, lastDiffEnd, diff.getSource(), "removed");
            }

            if (diff.getTarget().size() > 0) {
                lastDiffEnd1 = applyDiff(spansBuilder1, lastDiffEnd1, diff.getTarget(), "added");
            }
        }

        spansBuilder.add(Collections.emptyList(), codeAreas.getKey().getText().length() - lastDiffEnd);
        spansBuilder1.add(Collections.emptyList(), codeAreas.getValue().getText().length() - lastDiffEnd1);

        codeAreas.getKey().setStyleSpans(0, spansBuilder.create());
        codeAreas.getValue().setStyleSpans(0, spansBuilder1.create());
    }

    private int applyDiff(StyleSpansBuilder<Collection<String>> spansBuilder, int lastDiffEnd, Chunk<String> diff, String keyword) {
        int size = diff.getLines().get(0).length();
        spansBuilder.add(Collections.emptyList(), diff.getPosition() - lastDiffEnd);
        spansBuilder.add(Collections.singleton(keyword), size);
        lastDiffEnd = diff.getPosition() + size;
        return lastDiffEnd;
    }

    private List<AbstractDelta<String>> diff() {
        String original = codeAreas.getKey().getText();
        String revised = codeAreas.getValue().getText();

        Patch<String> patch;
        try {
            patch = DiffUtils.diffInline(original, revised);
        } catch (DiffException e) {
            return Collections.emptyList();
        }

        return patch.getDeltas();
    }
}
