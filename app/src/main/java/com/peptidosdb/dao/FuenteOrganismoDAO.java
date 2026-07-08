package com.peptidosdb.dao;

import com.peptidosdb.model.FuenteOrganismo;
import com.peptidosdb.security.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO del catálogo FUENTE_ORGANISMO (usado, por ejemplo, para llenar combos en la UI). */
public class FuenteOrganismoDAO {

    private Connection con() {
        return Sesion.actual().getConexion();
    }

    public List<FuenteOrganismo> listarTodos() throws SQLException {
        String sql = "SELECT id, nombre_cientifico, reino FROM FUENTE_ORGANISMO ORDER BY nombre_cientifico";
        List<FuenteOrganismo> lista = new ArrayList<>();
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new FuenteOrganismo(rs.getInt("id"), rs.getString("nombre_cientifico"), rs.getString("reino")));
            }
        }
        return lista;
    }

    /** Inserta el organismo si no existe (por nombre científico) y devuelve su id. */
    public int obtenerOInsertar(String nombreCientifico, String reino) throws SQLException {
        String buscar = "SELECT id FROM FUENTE_ORGANISMO WHERE nombre_cientifico = ?";
        try (PreparedStatement ps = con().prepareStatement(buscar)) {
            ps.setString(1, nombreCientifico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        String insertar = "INSERT INTO FUENTE_ORGANISMO (nombre_cientifico, reino) VALUES (?, ?) RETURNING id";
        try (PreparedStatement ps = con().prepareStatement(insertar)) {
            ps.setString(1, nombreCientifico);
            ps.setString(2, reino);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        }
    }
}
