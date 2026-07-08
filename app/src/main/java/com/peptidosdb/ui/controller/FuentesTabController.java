package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.FuenteDatosDAO;
import com.peptidosdb.model.FuentePeptidoRow;
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
import java.util.Optional;

/** Controlador de la pestaña "Fuentes" (relación N:M hacia el catálogo FUENTE_DATOS). */
public class FuentesTabController {

    @FXML private ComboBox<String> comboFuente;
    @FXML private TextField campoPrioridad;
    @FXML private TableView<FuentePeptidoRow> tablaFuentes;
    @FXML private TableColumn<FuentePeptidoRow, String> colFuente;
    @FXML private TableColumn<FuentePeptidoRow, Short> colPrioridad;

    private final FuenteDatosDAO fuenteDatosDAO = new FuenteDatosDAO();
    private final ObservableList<FuentePeptidoRow> items = FXCollections.observableArrayList();
    private final List<String> nombresAEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        comboFuente.setEditable(true);
        colFuente.setCellValueFactory(new PropertyValueFactory<>("nombreFuente"));
        colPrioridad.setCellValueFactory(new PropertyValueFactory<>("prioridad"));
        tablaFuentes.setItems(items);
        try {
            comboFuente.setItems(FXCollections.observableArrayList(fuenteDatosDAO.listarNombres()));
        } catch (SQLException e) {
            // El catálogo queda vacío; el usuario igual puede escribir un nombre nuevo.
        }
    }

    public void cargar(Integer peptidoId) throws SQLException {
        items.clear();
        nombresAEliminar.clear();
        if (peptidoId == null) return;
        items.addAll(fuenteDatosDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        String nombre = comboFuente.getValue();
        if (nombre == null || nombre.isBlank()) return;
        nombre = nombre.trim();

        Short prioridad = null;
        String texto = campoPrioridad.getText();
        if (texto != null && !texto.isBlank()) {
            try {
                prioridad = Short.parseShort(texto.trim());
            } catch (NumberFormatException e) {
                return; // valor inválido: se ignora, el campo queda como estaba
            }
        }

        String nombreFinal = nombre;
        Optional<FuentePeptidoRow> existente = items.stream()
                .filter(f -> f.getNombreFuente().equalsIgnoreCase(nombreFinal))
                .findFirst();

        if (existente.isPresent()) {
            existente.get().setPrioridad(prioridad);
            tablaFuentes.refresh();
        } else {
            items.add(new FuentePeptidoRow(nombre, prioridad, false));
        }
        campoPrioridad.clear();
    }

    @FXML
    private void onEliminar() {
        FuentePeptidoRow seleccionada = tablaFuentes.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        if (seleccionada.isExistente()) {
            nombresAEliminar.add(seleccionada.getNombreFuente());
        }
        items.remove(seleccionada);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (String nombre : nombresAEliminar) {
            fuenteDatosDAO.eliminar(peptidoId, nombre);
        }
        for (FuentePeptidoRow fila : items) {
            fuenteDatosDAO.guardar(peptidoId, fila.getNombreFuente(), fila.getPrioridad());
        }
    }
}
