package com.peptidosdb.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage stagePrincipal;

    @Override
    public void start(Stage stage) throws Exception {
        stagePrincipal = stage;
        stage.setTitle("Gestor de Péptidos Antimicrobianos");
        cambiarVista("/fxml/Login.fxml");
        stage.show();
    }

    /** Permite a los controladores navegar entre pantallas (login -> listado, etc). */
    public static void cambiarVista(String rutaFxml) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(rutaFxml));
        Parent root = loader.load();
        stagePrincipal.setScene(new Scene(root));
        stagePrincipal.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
