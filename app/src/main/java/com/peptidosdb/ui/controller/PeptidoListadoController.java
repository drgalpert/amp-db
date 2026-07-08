package com.peptidosdb.ui.controller;

import com.peptidosdb.model.Peptido;
import com.peptidosdb.security.Sesion;
import com.peptidosdb.service.AuthService;
import com.peptidosdb.service.ExportService;
import com.peptidosdb.service.PeptidoService;
import com.peptidosdb.ui.MainApp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;
import javafx.stage.FileChooser;

public class PeptidoListadoController {

    @FXML private TableView<Peptido> tablaPeptidos;
    @FXML private TableColumn<Peptido, Integer> colId;
    @FXML private TableColumn<Peptido, String> colNombre;
    @FXML private TableColumn<Peptido, String> colNombresAlt;
    @FXML private TableColumn<Peptido, String> colSecuencia;
    @FXML private TableColumn<Peptido, Short> colLongitud;
    @FXML private TableColumn<Peptido, String> colEstado;
    @FXML private TableColumn<Peptido, String> colOrganismo;
    @FXML private TableColumn<Peptido, String> colOrganismos;
    @FXML private TableColumn<Peptido, String> colActividades;

    @FXML private TextField campoBusqueda;
    @FXML private Label etiquetaUsuario;
    @FXML private Label etiquetaEstado;
    @FXML private Button botonNuevo;
    @FXML private Button botonEditar;
    @FXML private Button botonDarBaja;
    @FXML private Button botonExportar;

    private final PeptidoService peptidoService = new PeptidoService();
    private final AuthService authService = new AuthService();
    private final ExportService exportService = new ExportService();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombrePrincipal"));
        colNombresAlt.setCellValueFactory(new PropertyValueFactory<>("nombresAlternativos"));
        colSecuencia.setCellValueFactory(new PropertyValueFactory<>("secuencia"));
        colLongitud.setCellValueFactory(new PropertyValueFactory<>("longitud"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoVerificacion"));
        colOrganismo.setCellValueFactory(new PropertyValueFactory<>("organismoFuenteNombre"));
        colOrganismos.setCellValueFactory(new PropertyValueFactory<>("organismosResumen"));
        colActividades.setCellValueFactory(new PropertyValueFactory<>("actividadesResumen"));

        etiquetaUsuario.setText("Usuario: " + Sesion.actual().getUsuario().getNombreUsuario()
                + " (" + Sesion.actual().getUsuario().getRolBd() + ")");

        // Los botones de edición solo se habilitan para administrador/curador;
        // esto es solo UX, PostgreSQL igualmente rechazaría la operación por rol.
        boolean puedeEditar = Sesion.actual().puedeEditar();
        botonNuevo.setDisable(!puedeEditar);
        botonEditar.setDisable(!puedeEditar);
        botonDarBaja.setDisable(!puedeEditar);
        botonExportar.setDisable(false); // siempre disponible

        cargarDatos(null);
    }

    private void cargarDatos(String filtro) {
        try {
            ObservableList<Peptido> datos = FXCollections.observableArrayList(peptidoService.buscar(filtro));
            tablaPeptidos.setItems(datos);
            etiquetaEstado.setText("");
        } catch (SQLException e) {
            etiquetaEstado.setText("Error al consultar la base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void onBuscar() {
        cargarDatos(campoBusqueda.getText());
    }

    @FXML
    private void onNuevo() {
        abrirDetalle(null);
    }

    @FXML
    private void onEditar() {
        Peptido seleccionado = tablaPeptidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            etiquetaEstado.setText("Seleccione un péptido de la tabla primero");
            return;
        }
        abrirDetalle(seleccionado.getId());
    }

    @FXML
    private void onDarDeBaja() {
        Peptido seleccionado = tablaPeptidos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            etiquetaEstado.setText("Seleccione un péptido de la tabla primero");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Dar de baja el péptido '" + seleccionado.getNombrePrincipal() + "'?\n" +
                        "El registro no se borra físicamente: solo deja de mostrarse en las consultas.");
        confirmacion.showAndWait().ifPresent(boton -> {
            if (boton == ButtonType.OK) {
                try {
                    peptidoService.darDeBaja(seleccionado.getId());
                    cargarDatos(campoBusqueda.getText());
                } catch (SQLException e) {
                    etiquetaEstado.setText("Error al dar de baja: " + e.getMessage());
                } catch (SecurityException e) {
                    etiquetaEstado.setText(e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void onExportarExcel() {
        try {
            List<Peptido> datos = peptidoService.buscar(campoBusqueda.getText());
            if (datos.isEmpty()) {
                etiquetaEstado.setText("No hay datos para exportar.");
                return;
            }

            // Diálogo para elegir ubicación y nombre
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar como Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialFileName("peptidos_export_" + java.time.LocalDate.now() + ".xlsx");

            java.io.File file = fileChooser.showSaveDialog(tablaPeptidos.getScene().getWindow());
            if (file != null) {
                exportService.exportarListaAPeptidos(datos, file.getAbsolutePath());
                etiquetaEstado.setText("Exportación completada: " + file.getName());
            }
        } catch (Exception e) {
            etiquetaEstado.setText("Error al exportar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirDetalle(Integer peptidoId) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/PeptidoDetalle.fxml"));
            javafx.scene.Parent root = loader.load();
            PeptidoDetalleController controller = loader.getController();
            controller.cargar(peptidoId);

            abrirVentanaModal(root);
        } catch (Exception e) {
            etiquetaEstado.setText("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    /** Abre el formulario de detalle en una ventana modal y refresca la tabla al cerrarse. */
    private void abrirVentanaModal(javafx.scene.Parent root) {
        javafx.stage.Stage ventana = new javafx.stage.Stage();
        ventana.setTitle("Detalle de péptido");
        ventana.setScene(new javafx.scene.Scene(root));
        ventana.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        ventana.showAndWait();
        cargarDatos(campoBusqueda.getText());
    }

    @FXML
    private void onCerrarSesion() {
        authService.cerrarSesion();
        try {
            MainApp.cambiarVista("/fxml/Login.fxml");
        } catch (Exception e) {
            etiquetaEstado.setText("Error al volver al login: " + e.getMessage());
        }
    }
}
