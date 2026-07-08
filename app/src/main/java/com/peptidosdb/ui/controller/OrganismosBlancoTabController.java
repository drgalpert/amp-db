package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.OrganismoBlancoDAO;
import com.peptidosdb.dao.PeptidoOrganismoDAO;
import com.peptidosdb.model.OrganismoBlanco;
import com.peptidosdb.model.PeptidoOrganismoRow;
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

/**
 * Controlador de la pestaña "Organismos blanco" (relación N:M PEPTIDO_ORGANISMO,
 * con MIC como atributo propio de la relación). Mismo patrón de "buffer" que
 * NombresAlternativosTabController, pero usando upsert idempotente: al guardar,
 * se reinserta/actualiza todo lo que quedó en la tabla, y se borra solo lo que
 * el usuario quitó explícitamente (idsAEliminar).
 */
public class OrganismosBlancoTabController {

    @FXML private ComboBox<OrganismoBlanco> comboOrganismo;
    @FXML private TextField campoMicValor;
    @FXML private TextField campoMicUnidad;
    @FXML private TableView<PeptidoOrganismoRow> tablaOrganismos;
    @FXML private TableColumn<PeptidoOrganismoRow, String> colOrganismo;
    @FXML private TableColumn<PeptidoOrganismoRow, Double> colMicValor;
    @FXML private TableColumn<PeptidoOrganismoRow, String> colMicUnidad;

    private final OrganismoBlancoDAO organismoBlancoDAO = new OrganismoBlancoDAO();
    private final PeptidoOrganismoDAO peptidoOrganismoDAO = new PeptidoOrganismoDAO();

    private final ObservableList<PeptidoOrganismoRow> filas = FXCollections.observableArrayList();
    private final List<Integer> organismosAEliminar = new ArrayList<>();

    @FXML
    private void initialize() {
        colOrganismo.setCellValueFactory(new PropertyValueFactory<>("nombreOrganismo"));
        colMicValor.setCellValueFactory(new PropertyValueFactory<>("micValor"));
        colMicUnidad.setCellValueFactory(new PropertyValueFactory<>("micUnidad"));
        tablaOrganismos.setItems(filas);

        try {
            comboOrganismo.setItems(FXCollections.observableArrayList(organismoBlancoDAO.listarTodos()));
        } catch (SQLException e) {
            // El catálogo simplemente queda vacío; el error real se verá al intentar guardar.
        }
    }

    public void cargar(Integer peptidoId) throws SQLException {
        filas.clear();
        organismosAEliminar.clear();
        if (peptidoId == null) return;
        filas.addAll(peptidoOrganismoDAO.buscarPorPeptido(peptidoId));
    }

    @FXML
    private void onAgregar() {
        OrganismoBlanco organismo = comboOrganismo.getValue();
        if (organismo == null) return;

        Double micValor = null;
        String texto = campoMicValor.getText();
        if (texto != null && !texto.isBlank()) {
            try {
                micValor = Double.parseDouble(texto.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                return; // valor inválido: se ignora silenciosamente, el campo queda como estaba
            }
        }
        String micUnidad = campoMicUnidad.getText();

        // Si el organismo ya está en la tabla, se actualiza en vez de duplicar.
        Optional<PeptidoOrganismoRow> existente = filas.stream()
                .filter(f -> f.getOrganismoId().equals(organismo.getId()))
                .findFirst();

        if (existente.isPresent()) {
            existente.get().setMicValor(micValor);
            existente.get().setMicUnidad(micUnidad);
            tablaOrganismos.refresh();
        } else {
            filas.add(new PeptidoOrganismoRow(organismo.getId(), organismo.getNombreCientifico(),
                    micValor, micUnidad, false));
        }

        campoMicValor.clear();
        campoMicUnidad.clear();
    }

    @FXML
    private void onEliminar() {
        PeptidoOrganismoRow seleccionada = tablaOrganismos.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;
        if (seleccionada.isExistente()) {
            organismosAEliminar.add(seleccionada.getOrganismoId());
        }
        filas.remove(seleccionada);
    }

    public void guardar(int peptidoId) throws SQLException {
        for (Integer organismoId : organismosAEliminar) {
            peptidoOrganismoDAO.eliminar(peptidoId, organismoId);
        }
        for (PeptidoOrganismoRow fila : filas) {
            peptidoOrganismoDAO.guardar(peptidoId, fila.getOrganismoId(), fila.getMicValor(), fila.getMicUnidad());
        }
    }
}
