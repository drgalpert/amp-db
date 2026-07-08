package com.peptidosdb.dao;

import com.peptidosdb.model.NombreAlternativo;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NombreAlternativoDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<NombreAlternativo> buscarPorPeptido(int peptidoId) throws SQLException {
        String sql = "SELECT id, nombre FROM NOMBRE_ALTERNATIVO WHERE peptido_id = ? ORDER BY nombre";
        List<NombreAlternativo> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, peptidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new NombreAlternativo(rs.getInt("id"), rs.getString("nombre")));
                }
            }
        }
        return lista;
    }

    public void agregar(String nombre, int peptidoId) throws SQLException {
        String sql = "INSERT INTO NOMBRE_ALTERNATIVO (nombre, peptido_id) VALUES (?, ?) " +
                "ON CONFLICT (nombre, peptido_id) DO NOTHING";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, peptidoId);
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = con().prepareStatement("DELETE FROM NOMBRE_ALTERNATIVO WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
