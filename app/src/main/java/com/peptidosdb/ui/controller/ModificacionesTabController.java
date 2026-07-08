package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.ModificacionDAO;
import com.peptidosdb.model.ModificacionPostraduccional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pestaña "Modificaciones" (relación N:M hacia el catálogo
 * MODIFICACION_POSTRADUCCIONAL). Mismo patrón de buffer que las demás pestañas
 * de lista: los cambios se aplican en guardar(peptidoId).
 */
public class ModificacionesTabController {

    @FXML private ComboBox<String> comboTipo;
    @FXML private TextField campoPosicion;
    @FXML private ListView<ModificacionPostraduccional> listaModificaciones;

    private final ModificacionDAO modificacionDAO = new ModificacionDAO();
    private final ObservableList<ModificacionPostraduccional> items = FXCollections.observableArrayList();
    private final List<Integer> idsAEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        comboTipo.setEditable(true);
        comboTipo.setItems(FXCollections.observableArrayList("N-terminal", "C-terminal", "Other"));
        listaModificaciones.setItems(items);
    }

    public void cargar(Integer peptidoId) throws SQLException {
        items.clear();
        idsAEliminar.clear();
        if (peptidoId == null) return;
        items.addAll(modificacionDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        String tipo = comboTipo.getValue();
        if (tipo == null || tipo.isBlank()) return;
        String posicion = campoPosicion.getText();
        posicion = (posicion == null || posicion.isBlank()) ? null : posicion.trim();
        items.add(new ModificacionPostraduccional(null, tipo.trim(), posicion));
        campoPosicion.clear();
    }

    @FXML
    private void onEliminar() {
        ModificacionPostraduccional seleccionada = listaModificaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        if (seleccionada.getId() != null) {
            idsAEliminar.add(seleccionada.getId());
        }
        items.remove(seleccionada);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (Integer id : idsAEliminar) {
            modificacionDAO.eliminar(peptidoId, id);
        }
        for (ModificacionPostraduccional m : items) {
            if (m.getId() == null) {
                modificacionDAO.agregar(peptidoId, m.getTipo(), m.getPosicion());
            }
        }
    }
}
