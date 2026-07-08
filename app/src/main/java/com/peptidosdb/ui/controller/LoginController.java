package com.peptidosdb.ui.controller;

import com.peptidosdb.model.UsuarioApp;
import com.peptidosdb.service.AuthService;
import com.peptidosdb.ui.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoPassword;
    @FXML private Label etiquetaError;

    private final AuthService authService = new AuthService();

    @FXML
    private void onIniciarSesion() {
        etiquetaError.setText("");
        String usuario = campoUsuario.getText();
        String password = campoPassword.getText();

        if (usuario == null || usuario.isBlank() || password == null || password.isBlank()) {
            etiquetaError.setText("Debe ingresar usuario y contraseña");
            return;
        }

        try {
            UsuarioApp usuarioApp = authService.iniciarSesion(usuario.trim(), password);
            if (usuarioApp == null) {
                etiquetaError.setText("Usuario o contraseña incorrectos");
                return;
            }
            MainApp.cambiarVista("/fxml/PeptidoListado.fxml");
        } catch (SQLException e) {
            etiquetaError.setText("Error de conexión con la base de datos: " + e.getMessage());
        } catch (Exception e) {
            etiquetaError.setText("No se pudo abrir la pantalla principal: " + e.getMessage());
        }
    }
}
