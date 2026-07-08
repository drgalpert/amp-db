package com.peptidosdb.dao;

import com.peptidosdb.model.FuentePeptidoRow;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** DAO de FUENTE_DATOS (catálogo de bases de datos externas) y su relación N:M PEPTIDO_FUENTE. */
public class FuenteDatosDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    /** Nombres de fuentes ya existentes en el catálogo (cargadas por el ETL: DRAMP, UniProt, etc.). */
    public List<String> listarNombres() throws SQLException {
        List<String> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement("SELECT nombre FROM FUENTE_DATOS ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString("nombre"));
        }
        return lista;
    }

    public List<FuentePeptidoRow> buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT fuente_nombre, prioridad FROM PEPTIDO_FUENTE WHERE peptido_id = ? ORDER BY prioridad NULLS LAST";
        List<FuentePeptidoRow> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    short prioridad = rs.getShort("prioridad");
                    lista.add(new FuentePeptidoRow(rs.getString("fuente_nombre"),
                            rs.wasNull() ? null : prioridad, true));
                }
            }
        }
        return lista;
    }

    /** Crea la fuente en el catálogo si el usuario escribió un nombre nuevo (sin url/version). */
    private void asegurarExisteFuente(String nombre) throws SQLException {
        String sql = "INSERT INTO FUENTE_DATOS (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    public void guardar(int peptidoId, String nombreFuente, Short prioridad) throws SQLException {
        asegurarExisteFuente(nombreFuente);
        String sql = "INSERT INTO PEPTIDO_FUENTE (peptido_id, fuente_nombre, prioridad) VALUES (?, ?, ?) " +
                "ON CONFLICT (peptido_id, fuente_nombre) DO UPDATE SET prioridad = EXCLUDED.prioridad";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setString(2, nombreFuente);
            if (prioridad != null) {
                ps.setShort(3, prioridad);
            } else {
                ps.setNull(3, Types.SMALLINT);
            }
            ps.executeUpdate();
        }
    }

    public void eliminar(int peptidoId, String nombreFuente) throws SQLException {
        String sql = "DELETE FROM PEPTIDO_FUENTE WHERE peptido_id = ? AND fuente_nombre = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            ps.setString(2, nombreFuente);
            ps.executeUpdate();
        }
    }
}
