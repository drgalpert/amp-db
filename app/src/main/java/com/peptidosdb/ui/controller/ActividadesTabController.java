package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.ActividadDAO;
import com.peptidosdb.model.ActividadRow;
import com.peptidosdb.model.ActividadRow.Tipo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pestaña "Actividades" — cubre ACTIVIDAD_ANTIVIRAL,
 * ACTIVIDAD_ANTIFUNGICA y ACTIVIDAD_ANTICANCER desde una sola tabla, ya que
 * las tres son estructuralmente el mismo patrón (catálogo + nombre único).
 */
public class ActividadesTabController {

    @FXML private ComboBox<Tipo> comboTipo;
    @FXML private TextField campoNombre;
    @FXML private TableView<ActividadRow> tablaActividades;
    @FXML private TableColumn<ActividadRow, Tipo> colTipo;
    @FXML private TableColumn<ActividadRow, String> colNombre;

    private final ActividadDAO actividadDAO = new ActividadDAO();
    private final ObservableList<ActividadRow> items = FXCollections.observableArrayList();
    private final List<ActividadRow> aEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        comboTipo.setItems(FXCollections.observableArrayList(Tipo.values()));
        comboTipo.setValue(Tipo.ANTIVIRAL);
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        tablaActividades.setItems(items);
    }

    public void cargar(Integer peptidoId) throws SQLException {
        items.clear();
        aEliminar.clear();
        if (peptidoId == null) return;
        items.addAll(actividadDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        Tipo tipo = comboTipo.getValue();
        String nombre = campoNombre.getText();
        if (tipo == null || nombre == null || nombre.isBlank()) return;
        nombre = nombre.trim();

        String nombreFinal = nombre;
        boolean yaExiste = items.stream()
                .anyMatch(a -> a.getTipo() == tipo && a.getNombre().equalsIgnoreCase(nombreFinal));
        if (!yaExiste) {
            items.add(new ActividadRow(null, tipo, nombre, false));
        }
        campoNombre.clear();
    }

    @FXML
    private void onEliminar() {
        ActividadRow seleccionada = tablaActividades.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        if (seleccionada.isExistente()) {
            aEliminar.add(seleccionada);
        }
        items.remove(seleccionada);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (ActividadRow a : aEliminar) {
            actividadDAO.eliminar(peptidoId, a.getTipo(), a.getId());
        }
        for (ActividadRow a : items) {
            if (a.getId() == null) {
                actividadDAO.agregar(peptidoId, a.getTipo(), a.getNombre());
            }
        }
    }
}
