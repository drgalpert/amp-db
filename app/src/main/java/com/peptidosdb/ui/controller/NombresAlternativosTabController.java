package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.NombreAlternativoDAO;
import com.peptidosdb.model.NombreAlternativo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pestaña "Nombres alternativos" (relación 1:N con péptido).
 * Patrón de "buffer" en memoria: los cambios (altas/bajas) no tocan la BD hasta
 * que se llama a guardar(peptidoId) — necesario porque, si el péptido es nuevo,
 * su id todavía no existe mientras se llena el formulario.
 */
public class NombresAlternativosTabController {

    @FXML private TextField campoNuevoNombre;
    @FXML private ListView<NombreAlternativo> listaNombres;

    private final NombreAlternativoDAO nombreAlternativoDAO = new NombreAlternativoDAO();
    private final ObservableList<NombreAlternativo> items = FXCollections.observableArrayList();
    private final List<Integer> idsAEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        listaNombres.setItems(items);
    }

    public void cargar(Integer peptidoId) throws SQLException {
        items.clear();
        idsAEliminar.clear();
        if (peptidoId == null) return;
        items.addAll(nombreAlternativoDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        String nombre = campoNuevoNombre.getText();
        if (nombre == null || nombre.isBlank()) return;
        items.add(new NombreAlternativo(null, nombre.trim()));
        campoNuevoNombre.clear();
    }

    @FXML
    private void onEliminar() {
        NombreAlternativo seleccionado = listaNombres.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        if (seleccionado.getId() != null) {
            idsAEliminar.add(seleccionado.getId());
        }
        items.remove(seleccionado);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (Integer id : idsAEliminar) {
            nombreAlternativoDAO.eliminar(id);
        }
        for (NombreAlternativo n : items) {
            if (n.getId() == null) {
                nombreAlternativoDAO.agregar(n.getNombre(), peptidoId);
            }
        }
    }
}
