package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent rootNode = (Parent) FXMLLoader.load(getClass().getClassLoader().getResource("fxml/client2.fxml"));

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(rootNode, 800, 600));
        primaryStage.show();
    }

}
