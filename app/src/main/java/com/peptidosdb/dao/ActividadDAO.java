package com.peptidosdb.dao;

import com.peptidosdb.model.ActividadRow;
import com.peptidosdb.model.ActividadRow.Tipo;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO combinado de ACTIVIDAD_ANTIVIRAL, ACTIVIDAD_ANTIFUNGICA y ACTIVIDAD_ANTICANCER
 * (y sus tres relaciones N:M PEPTIDO_ANTIVIRAL / PEPTIDO_ANTIFUNGICO / PEPTIDO_ANTICANCER).
 * Las tres comparten la misma forma "catálogo + nombre identificador único", así que
 * se manejan desde una sola pestaña para no triplicar la interfaz.
 */
public class ActividadDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<ActividadRow> buscarPorPeptido(int peptidoId) throws SQLException {
        List<ActividadRow> lista = new ArrayList<>();
        lista.addAll(buscarPorTipo(peptidoId, Tipo.ANTIVIRAL,
                "ACTIVIDAD_ANTIVIRAL", "nombre_virus", "PEPTIDO_ANTIVIRAL", "antiviral_id"));
        lista.addAll(buscarPorTipo(peptidoId, Tipo.ANTIFUNGICA,
                "ACTIVIDAD_ANTIFUNGICA", "nombre_hongo", "PEPTIDO_ANTIFUNGICO", "antifungico_id"));
        lista.addAll(buscarPorTipo(peptidoId, Tipo.ANTICANCER,
                "ACTIVIDAD_ANTICANCER", "linea_celular", "PEPTIDO_ANTICANCER", "anticancer_id"));
        return lista;
    }

    private List<ActividadRow> buscarPorTipo(int peptidoId, Tipo tipo, String tablaCatalogo,
                                              String columnaNombre, String tablaJuncion,
                                              String columnaFk) throws SQLException {
        String sql = String.format(
                "SELECT c.id, c.%s AS nombre FROM %s c " +
                "JOIN %s j ON c.id = j.%s WHERE j.peptido_id = ? ORDER BY nombre",
                columnaNombre, tablaCatalogo, tablaJuncion, columnaFk);
        List<ActividadRow> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ActividadRow(rs.getInt("id"), tipo, rs.getString("nombre"), true));
                }
            }
        }
        return lista;
    }

    public void agregar(int peptidoId, Tipo tipo, String nombre) throws SQLException {
        switch (tipo) {
            case ANTIVIRAL -> agregarGenerico(peptidoId, nombre,
                    "ACTIVIDAD_ANTIVIRAL", "nombre_virus", "PEPTIDO_ANTIVIRAL", "antiviral_id");
            case ANTIFUNGICA -> agregarGenerico(peptidoId, nombre,
                    "ACTIVIDAD_ANTIFUNGICA", "nombre_hongo", "PEPTIDO_ANTIFUNGICO", "antifungico_id");
            case ANTICANCER -> agregarGenerico(peptidoId, nombre,
                    "ACTIVIDAD_ANTICANCER", "linea_celular", "PEPTIDO_ANTICANCER", "anticancer_id");
        }
    }

    private void agregarGenerico(int peptidoId, String nombre, String tablaCatalogo,
                                  String columnaNombre, String tablaJuncion,
                                  String columnaFk) throws SQLException {
        int catalogoId;
        String buscar = String.format("SELECT id FROM %s WHERE %s = ?", tablaCatalogo, columnaNombre);
        try (PreparedStatement ps = con().prepareStatement(buscar)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    catalogoId = rs.getInt("id");
                } else {
                    catalogoId = -1;
                }
            }
        }
        if (catalogoId == -1) {
            String insertar = String.format(
                    "INSERT INTO %s (%s) VALUES (?) RETURNING id", tablaCatalogo, columnaNombre);
            try (PreparedStatement ps = con().prepareStatement(insertar)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    catalogoId = rs.getInt("id");
                }
            }
        }

        String junction = String.format(
                "INSERT INTO %s (peptido_id, %s) VALUES (?, ?) ON CONFLICT DO NOTHING",
                tablaJuncion, columnaFk);
        try (PreparedStatement ps = con().prepareStatement(junction)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, catalogoId);
            ps.executeUpdate();
        }
    }

    public void eliminar(int peptidoId, Tipo tipo, int catalogoId) throws SQLException {
        String tablaJuncion;
        String columnaFk;
        switch (tipo) {
            case ANTIVIRAL -> { tablaJuncion = "PEPTIDO_ANTIVIRAL"; columnaFk = "antiviral_id"; }
            case ANTIFUNGICA -> { tablaJuncion = "PEPTIDO_ANTIFUNGICO"; columnaFk = "antifungico_id"; }
            case ANTICANCER -> { tablaJuncion = "PEPTIDO_ANTICANCER"; columnaFk = "anticancer_id"; }
            default -> throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        }
        String sql = String.format("DELETE FROM %s WHERE peptido_id = ? AND %s = ?", tablaJuncion, columnaFk);
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, catalogoId);
            ps.executeUpdate();
        }
    }
}
