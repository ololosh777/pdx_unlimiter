package com.crschnick.pdx_unlimiter.app.gui;

import com.crschnick.pdx_unlimiter.app.game.GameIntegration;
import com.crschnick.pdx_unlimiter.app.installation.Settings;
import com.crschnick.pdx_unlimiter.app.installation.TaskExecutor;
import com.crschnick.pdx_unlimiter.app.savegame.FileImporter;
import com.jfoenix.controls.JFXSpinner;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;

public class GuiLayout {

    private static BorderPane layout;

    public static StackPane createLayout() {
        layout = new BorderPane();
        var menu = GuiMenuBar.createMenu();
        layout.setTop(menu);
        layout.setBottom(GuiStatusBar.createStatusBar());

        Pane pane = new Pane(new Label());
        layout.setCenter(pane);
        GuiGameCampaignEntryList.createCampaignEntryList(pane);
        layout.setCenter(pane);

        layout.setLeft(GuiGameCampaignList.createCampaignList());
        BorderPane.setAlignment(pane, Pos.CENTER);

        layout.setOnDragOver(event -> {
            if (event.getGestureSource() == null
                    && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE);
            }
            event.consume();
        });

        layout.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            db.getFiles().stream()
                    .map(File::toString)
                    .forEach(FileImporter::addToImportQueue);
            event.setDropCompleted(true);
            event.consume();
        });

        JFXSpinner loading = new JFXSpinner();
        Pane loadingBg = new StackPane(loading);
        loadingBg.getStyleClass().add(GuiStyle.CLASS_LOADING);
        loadingBg.visibleProperty().bind(TaskExecutor.getInstance().busyProperty());
        loadingBg.setMinWidth(Pane.USE_COMPUTED_SIZE);
        loadingBg.setPrefHeight(Pane.USE_COMPUTED_SIZE);

        StackPane stack = new StackPane(new Pane(), layout, loadingBg);

        GameIntegration.currentGameProperty().addListener((c, o, n) -> Platform.runLater(() -> {
            if (n != null) {
                stack.getChildren().set(0, n.getGuiFactory().background());
                try {
                    menu.setOpacity(0.95);
                    stack.styleProperty().set("-fx-font-family: " + n.getGuiFactory().font().getName() + ";");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        return stack;
    }

    public static void init() {
        Platform.runLater(() -> {
            layout.styleProperty().setValue("-fx-font-size: " + Settings.getInstance().getFontSize() + "pt;");
        });
    }
}
