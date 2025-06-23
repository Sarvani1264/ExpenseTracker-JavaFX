package com.example.expensetracker;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {
    public static <T> void loadController(String fxmlPath, ActionEvent event, ControllerConsumer<T> controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneController.class.getResource(fxmlPath));
        Parent root = loader.load();

        T loadedcontroller = loader.getController();
        controller.accept(loadedcontroller);

        Stage currentStage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        currentStage.setScene((new Scene(root)));
        //currentStage.close();
    }
    public static void loadfxml(String fxmlPath, ActionEvent e) throws IOException{
        Parent root = FXMLLoader.load(SceneController.class.getResource(fxmlPath));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    public static void setVisibleNodes(boolean isVisible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(isVisible);
            node.setManaged(isVisible);
        }
    }
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
