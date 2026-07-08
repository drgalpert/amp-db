package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.EstabilidadClinicaDAO;
import com.peptidosdb.model.EstabilidadClinica;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.sql.SQLException;

/** Controlador de la pestaña "Estabilidad clínica" (relación 1:1 con péptido). */
public class EstabilidadClinicaTabController {

    @FXML private TextField campoEstabilidadSuero;
    @FXML private ComboBox<String> comboEstadoClinico;
    @FXML private TextArea campoToxicidad;

    private final EstabilidadClinicaDAO estabilidadDAO = new EstabilidadClinicaDAO();

    @FXML
    private void initialize() {
        comboEstadoClinico.setItems(FXCollections.observableArrayList(
                "preclínica", "fase1", "fase2", "fase3", "aprobado", "retirado"));
    }

    public void cargar(Integer peptidoId) throws SQLException {
        if (peptidoId == null) return;
        EstabilidadClinica ec = estabilidadDAO.buscarPorPeptido(peptidoId);
        if (ec == null) return;
        campoEstabilidadSuero.setText(ec.getEstabilidadSuero() != null ? ec.getEstabilidadSuero().toString() : "");
        comboEstadoClinico.setValue(ec.getEstadoClinico());
        campoToxicidad.setText(ec.getToxicidadInVivo());
    }

    public void guardar(int peptidoId) throws SQLException {
        EstabilidadClinica ec = new EstabilidadClinica();
        String texto = campoEstabilidadSuero.getText();
        if (texto != null && !texto.isBlank()) {
            try {
                ec.setEstabilidadSuero(new BigDecimal(texto.trim().replace(",", ".")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Estabilidad en suero: revise el valor numérico");
            }
        }
        ec.setEstadoClinico(comboEstadoClinico.getValue());
        ec.setToxicidadInVivo(vacioComoNull(campoToxicidad.getText()));
        ec.setPeptidoId(peptidoId);
        estabilidadDAO.guardar(ec);
    }

    private String vacioComoNull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
