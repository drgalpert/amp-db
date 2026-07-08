package com.peptidosdb.ui.controller;

import com.peptidosdb.dao.EstructuraDAO;
import com.peptidosdb.model.Estructura;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/** Controlador de la pestaña "Estructura" (relación 1:1 con péptido). */
public class EstructuraTabController {

    @FXML private TextField campoTipoEstructura;
    @FXML private TextField campoPdbId;
    @FXML private TextField campoMetodo;
    @FXML private TextField campoCiclizacion;

    private final EstructuraDAO estructuraDAO = new EstructuraDAO();

    /** Carga los datos existentes; si peptidoId es null (péptido nuevo) deja el formulario vacío. */
    public void cargar(Integer peptidoId) throws SQLException {
        if (peptidoId == null) return;
        Estructura e = estructuraDAO.buscarPorPeptido(peptidoId);
        if (e == null) return;
        campoTipoEstructura.setText(e.getTipoEstructura());
        campoPdbId.setText(e.getPdbId());
        campoMetodo.setText(e.getMetodo());
        campoCiclizacion.setText(e.getCiclizacion());
    }

    /** Persiste los datos de esta pestaña; se llama luego de guardar el péptido principal. */
    public void guardar(int peptidoId) throws SQLException {
        Estructura e = new Estructura();
        e.setTipoEstructura(vacioComoNull(campoTipoEstructura.getText()));
        e.setPdbId(vacioComoNull(campoPdbId.getText()));
        e.setMetodo(vacioComoNull(campoMetodo.getText()));
        e.setCiclizacion(vacioComoNull(campoCiclizacion.getText()));
        e.setPeptidoId(peptidoId);
        estructuraDAO.guardar(e);
    }

    private String vacioComoNull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
