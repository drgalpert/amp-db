package com.peptidosdb.dao;

import com.peptidosdb.model.ModificacionPostraduccional;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de MODIFICACION_POSTRADUCCIONAL (catálogo, UNIQUE por tipo+posicion) y su
 * relación N:M PEPTIDO_MODIFICACION.
 */
public class ModificacionDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<ModificacionPostraduccional> buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT m.id, m.tipo, m.posicion " +
                "FROM MODIFICACION_POSTRADUCCIONAL m " +
                "JOIN PEPTIDO_MODIFICACION pm ON m.id = pm.modificacion_id " +
                "WHERE pm.peptido_id = ? ORDER BY m.tipo";
        List<ModificacionPostraduccional> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ModificacionPostraduccional(
                            rs.getInt("id"), rs.getString("tipo"), rs.getString("posicion")));
                }
            }
        }
        return lista;
    }

    /** Busca o crea la entrada en el catálogo (tipo, posicion es UNIQUE) y devuelve su id. */
    private int obtenerOInsertarModificacion(String tipo, String posicion) throws SQLException {
        String buscar = "SELECT id FROM MODIFICACION_POSTRADUCCIONAL WHERE tipo = ? " +
                "AND posicion IS NOT DISTINCT FROM ?";
        try (PreparedStatement ps = con().prepareStatement(buscar)) {
            ps.setString(1, tipo);
            ps.setString(2, posicion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        String insertar = "INSERT INTO MODIFICACION_POSTRADUCCIONAL (tipo, posicion) VALUES (?, ?) RETURNING id";
        try (PreparedStatement ps = con().prepareStatement(insertar)) {
            ps.setString(1, tipo);
            ps.setString(2, posicion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        }
    }

    public void agregar(int peptidoId, String tipo, String posicion) throws SQLException {
        int modificacionId = obtenerOInsertarModificacion(tipo, posicion);
        String sql = "INSERT INTO PEPTIDO_MODIFICACION (peptido_id, modificacion_id) VALUES (?, ?) " +
                "ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, modificacionId);
            ps.executeUpdate();
        }
    }

    /** Quita la asociación (no borra la entrada del catálogo: otros péptidos pueden usarla). */
    public void eliminar(int peptidoId, int modificacionId) throws SQLException {
        String sql = "DELETE FROM PEPTIDO_MODIFICACION WHERE peptido_id = ? AND modificacion_id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setInt(2, modificacionId);
            ps.executeUpdate();
        }
    }
}
