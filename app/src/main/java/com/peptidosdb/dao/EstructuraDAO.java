package com.peptidosdb.dao;

import com.peptidosdb.model.Estructura;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** DAO de ESTRUCTURA: relación 1:1 con PÉPTIDO (peptido_id es UNIQUE). */
public class EstructuraDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public Estructura buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT * FROM ESTRUCTURA WHERE peptido_id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Estructura e = new Estructura();
                e.setId(rs.getInt("id"));
                e.setTipoEstructura(rs.getString("tipo_estructura"));
                e.setPdbId(rs.getString("pdb_id"));
                e.setMetodo(rs.getString("metodo"));
                e.setCiclizacion(rs.getString("ciclizacion"));
                e.setPeptidoId(rs.getInt("peptido_id"));
                return e;
            }
        }
    }

    /** Inserta o actualiza (upsert por peptido_id, que es UNIQUE). Si todos los campos
     *  quedan vacíos, no persiste nada (evita filas "fantasma" sin datos útiles). */
    public void guardar(Estructura e) throws SQLException {
        boolean vacio = vacio(e.getTipoEstructura()) && vacio(e.getPdbId())
                && vacio(e.getMetodo()) && vacio(e.getCiclizacion());
        if (vacio) {
            eliminar(e.getPeptidoId());
            return;
        }
        String sql = "INSERT INTO ESTRUCTURA (tipo_estructura, pdb_id, metodo, ciclizacion, peptido_id) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (peptido_id) DO UPDATE SET " +
                "tipo_estructura = EXCLUDED.tipo_estructura, pdb_id = EXCLUDED.pdb_id, " +
                "metodo = EXCLUDED.metodo, ciclizacion = EXCLUDED.ciclizacion";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, e.getTipoEstructura());
            ps.setString(2, e.getPdbId());
            ps.setString(3, e.getMetodo());
            ps.setString(4, e.getCiclizacion());
            ps.setInt(5, e.getPeptidoId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int peptidoId) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM ESTRUCTURA WHERE peptido_id = ?")) {
            ps.setInt(1, peptidoId);
            ps.executeUpdate();
        }
    }

    private boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
