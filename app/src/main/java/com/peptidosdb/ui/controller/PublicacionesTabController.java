package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.PublicacionDAO;
import com.peptidosdb.model.Publicacion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pestaña "Publicaciones" (relación N:M hacia PUBLICACIÓN,
 * una entidad que puede estar compartida por varios péptidos). Solo pedimos
 * el PMID: el resto de los datos (título, autores, año) los completa después
 * enrich_pubmed.py consultando PubMed, para no duplicar ese trabajo aquí.
 */
public class PublicacionesTabController {

    @FXML private TextField campoPmid;
    @FXML private TableView<Publicacion> tablaPublicaciones;
    @FXML private TableColumn<Publicacion, String> colPmid;
    @FXML private TableColumn<Publicacion, String> colTitulo;
    @FXML private TableColumn<Publicacion, Integer> colAnyo;

    private final PublicacionDAO publicacionDAO = new PublicacionDAO();
    private final ObservableList<Publicacion> items = FXCollections.observableArrayList();
    private final List<String> pmidsAEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        colPmid.setCellValueFactory(new PropertyValueFactory<>("pmid"));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colAnyo.setCellValueFactory(new PropertyValueFactory<>("anyo"));
        tablaPublicaciones.setItems(items);
    }

    public void cargar(Integer peptidoId) throws SQLException {
        items.clear();
        pmidsAEliminar.clear();
        if (peptidoId == null) return;
        items.addAll(publicacionDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        String pmid = campoPmid.getText();
        if (pmid == null || pmid.isBlank()) return;
        String pmidFinal = pmid.trim();
        boolean yaExiste = items.stream().anyMatch(p -> p.getPmid().equals(pmidFinal));
        if (!yaExiste) {
            items.add(new Publicacion(pmidFinal, null, false));
        }
        campoPmid.clear();
    }

    @FXML
    private void onEliminar() {
        Publicacion seleccionada = tablaPublicaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        if (seleccionada.isExistente()) {
            pmidsAEliminar.add(seleccionada.getPmid());
        }
        items.remove(seleccionada);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (String pmid : pmidsAEliminar) {
            publicacionDAO.desasociar(peptidoId, pmid);
        }
        for (Publicacion p : items) {
            publicacionDAO.asegurarExiste(p.getPmid());
            publicacionDAO.asociar(peptidoId, p.getPmid());
        }
    }
}
